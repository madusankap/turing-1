/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.service;

import org.apache.cassandra.auth.Action;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.Resources;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql.CQLStatement;
import org.apache.cassandra.db.Table;
import org.apache.cassandra.thrift.AuthenticationException;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.utils.SemanticVersion;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A container for per-client, thread-local state that Avro/Thrift threads must hold.
 * TODO: Kill thrift exceptions
 */
public class ClientState
{
    private static final int MAX_CACHE_PREPARED = 10000;    // Enough to keep buggy clients from OOM'ing us
    private static Logger logger = LoggerFactory.getLogger(ClientState.class);
    public static final SemanticVersion DEFAULT_CQL_VERSION = org.apache.cassandra.cql.QueryProcessor.CQL_VERSION;

    // Current user for the session
    private AuthenticatedUser user;
    private String keyspace;
    // Only for internal usage
    private String qualifiedKeyspace;
    private String prefix;
    // Reusable array for authorization
    private final List<Object> resource = new ArrayList<Object>();
    private SemanticVersion cqlVersion = DEFAULT_CQL_VERSION;

    // An LRU map of prepared statements
    private Map<Integer, CQLStatement> prepared = new LinkedHashMap<Integer, CQLStatement>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, CQLStatement> eldest) {
            return size() > MAX_CACHE_PREPARED;
        }
    };

    private Map<Integer, org.apache.cassandra.cql3.CQLStatement> cql3Prepared = new LinkedHashMap<Integer, org.apache.cassandra.cql3.CQLStatement>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, org.apache.cassandra.cql3.CQLStatement> eldest) {
            return size() > MAX_CACHE_PREPARED;
        }
    };

    private long clock;

    /**
     * Construct a new, empty ClientState: can be reused after logout() or reset().
     */
    public ClientState()
    {
        reset();
    }

    public Map<Integer, CQLStatement> getPrepared()
    {
        return prepared;
    }

    public Map<Integer, org.apache.cassandra.cql3.CQLStatement> getCQL3Prepared()
    {
        return cql3Prepared;
    }

    public String getRawKeyspace()
    {
        return keyspace;
    }

    public String getKeyspace() throws InvalidRequestException
    {
        if (keyspace == null)
            throw new InvalidRequestException("no keyspace has been specified");
        return keyspace;
    }

    public void setKeyspace(String ks) throws InvalidRequestException
    {
        //if (Schema.instance.getKSMetaData(ks) == null)
        //    throw new InvalidRequestException("Keyspace '" + ks + "' does not exist");
        keyspace = ks;
		setQualifiedKeyspace();
    }

    public String getSchedulingValue()
    {
        switch(DatabaseDescriptor.getRequestSchedulerId())
        {
            case keyspace: return keyspace;
        }
        return "default";
    }

    /**
     * Attempts to login this client with the given credentials map.
     */
    public void login(Map<? extends CharSequence,? extends CharSequence> credentials) throws AuthenticationException
    {
        AuthenticatedUser user = DatabaseDescriptor.getAuthenticator().authenticate(credentials);
        if (logger.isDebugEnabled())
            logger.debug("logged in: {}", user);
        this.user = user;
        setQualifiedKeyspace();
    }

    public void logout()
    {
        if (logger.isDebugEnabled())
            logger.debug("logged out: {}", user);
        reset();
    }

    private void resourceClear()
    {
        resource.clear();
        resource.add(Resources.ROOT);
        resource.add(Resources.KEYSPACES);
    }

    public void reset()
    {
        user = DatabaseDescriptor.getAuthenticator().defaultUser();
        keyspace = null;
        qualifiedKeyspace = null;
        prefix = null;
        resourceClear();
        prepared.clear();
        cql3Prepared.clear();
    }

    /**
     * Confirms that the client thread has the given Permission for the Keyspace list.
     */
    public void hasKeyspaceSchemaAccess(Permission perm, Action action) throws InvalidRequestException
    {
        validateLogin();

        resourceClear();
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource, action);

        hasAccess(user, perms, perm, resource);
    }

    public void hasColumnFamilySchemaAccess(Permission perm, Action action) throws InvalidRequestException
    {
        hasColumnFamilySchemaAccess(keyspace, perm, action);
    }

    /**
     * Confirms that the client thread has the given Permission for the ColumnFamily list of
     * the provided keyspace.
     */
    public void hasColumnFamilySchemaAccess(String keyspace, Permission perm , Action action) throws InvalidRequestException
    {
        validateLogin();
        validateKeyspace(keyspace);

        // hardcode disallowing messing with system keyspace
        if (keyspace.equalsIgnoreCase(Table.SYSTEM_TABLE) && perm == Permission.WRITE)
            throw new InvalidRequestException("system keyspace is not user-modifiable");

        resourceClear();
        resource.add(keyspace);
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource, action);

        hasAccess(user, perms, perm, resource);
    }

    /**
     * Confirms that the client thread has the given Permission in the context of the given
     * ColumnFamily and the current keyspace.
     */
    public void hasColumnFamilyAccess(String columnFamily, Permission perm, Action action) throws InvalidRequestException
    {
        hasColumnFamilyAccess(keyspace, columnFamily, perm, action);
    }

    public void hasColumnFamilyAccess(String keyspace, String columnFamily, Permission perm, Action action) throws InvalidRequestException
    {
        validateLogin();
        validateKeyspace(keyspace);

        resourceClear();
        resource.add(keyspace);
        resource.add(columnFamily);
        Set<Permission> perms = DatabaseDescriptor.getAuthority().authorize(user, resource, action);

        hasAccess(user, perms, perm, resource);
    }


    /**
     * Returns the keyspace to be used for further Cassandra's operations
     * If the current user has a keyspace qualified by his or her domain name, returns it. O.w. returns the original keyspace
     *
     * @return <String> the resolved keyspace name
     */
    public String getResolvedKeyspace() {
        resolveKeyspace(keyspace);
        return qualifiedKeyspace == null ? keyspace : qualifiedKeyspace;
    }

    /**
     * Returns the keyspace's name to be used by internal Cassandra's operations
     * The given keyspace's name is modified appropriately by prefixing it with the user's domain name to the keyspace
     *
     * @param keyspaceTobeResolved the original keyspace's name given by the client application
     * @return <String> the resolved keyspace name
     */
    public String resolveKeyspace(String keyspaceTobeResolved) {

        return (prefix==null || prefix.equals("carbon_super_") || keyspaceTobeResolved.startsWith(prefix)) ? keyspaceTobeResolved : (prefix + keyspaceTobeResolved);
    }

    /**
     * Remove the domain name from the keyspace name if there is one
     *
     * @param keyspaceWithDomainName keyspace name with domain name attach
     * @return the resolved keyspace name
     */
    public String getDomainNameLessKeyspace(String keyspaceWithDomainName) {
        return (prefix == null || !keyspaceWithDomainName.startsWith(prefix)) ? keyspaceWithDomainName :
                keyspaceWithDomainName.substring(prefix.length());

    }

	private void setQualifiedKeyspace() {
        if (user != null && user.domainName != null) {
            if (prefix == null) {
                String tmpPrefix = user.domainName + "_";
                tmpPrefix = tmpPrefix.replace(".", "_");
                tmpPrefix = tmpPrefix.replace("-", "z");
                prefix = tmpPrefix;

            }
            if (keyspace != null) {
                //qualifiedKeyspace = prefix + keyspace;
                qualifiedKeyspace = prefix.equals("carbon_super_")?keyspace:prefix + keyspace;
            }
        }
    }


    private void validateLogin() throws InvalidRequestException
    {
        if (user == null)
            throw new InvalidRequestException("You have not logged in");
    }

    private static void validateKeyspace(String keyspace) throws InvalidRequestException
    {
        if (keyspace == null)
            throw new InvalidRequestException("You have not set a keyspace for this session");
    }

    private static void hasAccess(AuthenticatedUser user, Set<Permission> perms, Permission perm, List<Object> resource) throws InvalidRequestException
    {
        if (perms.contains(perm))
            return;
        throw new InvalidRequestException(String.format("%s does not have permission %s for %s",
                                                        user,
                                                        perm,
                                                        Resources.toString(resource)));
    }

    /**
     * This clock guarantees that updates from a given client will be ordered in the sequence seen,
     * even if multiple updates happen in the same millisecond.  This can be useful when a client
     * wants to perform multiple updates to a single column.
     */
    public long getTimestamp()
    {
        long current = System.currentTimeMillis() * 1000;
        clock = clock >= current ? clock + 1 : current;
        return clock;
    }

    public void setCQLVersion(String str) throws InvalidRequestException
    {
        SemanticVersion version;
        try
        {
            version = new SemanticVersion(str);
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidRequestException(e.getMessage());
        }

        SemanticVersion cql = org.apache.cassandra.cql.QueryProcessor.CQL_VERSION;
        SemanticVersion cql3 = org.apache.cassandra.cql3.QueryProcessor.CQL_VERSION;

        if (version.isSupportedBy(cql))
            cqlVersion = cql;
        else if (version.isSupportedBy(cql3))
            cqlVersion = cql3;
        else
            throw new InvalidRequestException(String.format("Provided version %s is not supported by this server (supported: %s)",
                                                            version,
                                                            StringUtils.join(getCQLSupportedVersion(), ", ")));
    }

    public SemanticVersion getCQLVersion()
    {
        return cqlVersion;
    }

    public static SemanticVersion[] getCQLSupportedVersion()
    {
        SemanticVersion cql = org.apache.cassandra.cql.QueryProcessor.CQL_VERSION;
        SemanticVersion cql3 = org.apache.cassandra.cql3.QueryProcessor.CQL_VERSION;

        return new SemanticVersion[]{ cql, cql3 };
    }

    /**
     * Returen authenticated tenant user
     * @return  Tenant user name
     */
    public String getTenantUser() {
        return user.username;
    }

    /**
     * Return Tenant domain
     * @return  Tenant domain
     */
    public String getTenantDomain(){
        return user.domainName;
    }

    public String getPrefix(){
        return prefix;
    }
}

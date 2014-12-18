/*
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
package org.apache.cassandra.auth;

import com.google.common.base.Objects;

/**
 * Returned from IAuthenticator#authenticate(), represents an authenticated user everywhere internally.
 */
public class AuthenticatedUser
{
    public static final String ANONYMOUS_USERNAME = "anonymous";
    public static final AuthenticatedUser ANONYMOUS_USER = new AuthenticatedUser(ANONYMOUS_USERNAME, null);

    private final String name;
	private final String domainName;

    public AuthenticatedUser(String name)
    {
        this.name = name;
        this.domainName = null;
    }

    public AuthenticatedUser(String name, String domainName)
    {
        this.name = name;
		this.domainName = domainName;
    }

    public String getName()
    {
        return name;
    }

    public String getDomainName() {
        return domainName;
    }

    /**
     * Checks the user's superuser status.
     * Only a superuser is allowed to perform CREATE USER and DROP USER queries.
     * Im most cased, though not necessarily, a superuser will have Permission.ALL on every resource
     * (depends on IAuthorizer implementation).
     */
    public boolean isSuper()
    {
        return !isAnonymous() && Auth.isSuperuser(name);
    }

    /**
     * If IAuthenticator doesn't require authentication, this method may return true.
     */
    public boolean isAnonymous()
    {
        return this == ANONYMOUS_USER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatedUser)) return false;

        AuthenticatedUser that = (AuthenticatedUser) o;

        if (!domainName.equals(that.domainName)) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + domainName.hashCode();
        return result;
    }
}

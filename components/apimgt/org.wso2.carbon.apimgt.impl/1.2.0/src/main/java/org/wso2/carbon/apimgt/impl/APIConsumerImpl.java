/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.apimgt.impl;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.api.model.Tag;
import org.wso2.carbon.apimgt.handlers.security.stub.types.APIKeyMapping;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.dto.TierPermissionDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIAuthenticationAdminClient;
import org.wso2.carbon.apimgt.impl.utils.APINameComparator;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.APIVersionComparator;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.pagination.PaginationContext;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides the core API store functionality. It is implemented in a very
 * self-contained and 'pure' manner, without taking requirements like security into account,
 * which are subject to frequent change. Due to this 'pure' nature and the significance of
 * the class to the overall API management functionality, the visibility of the class has
 * been reduced to package level. This means we can still use it for internal purposes and
 * possibly even extend it, but it's totally off the limits of the users. Users wishing to
 * programmatically access this functionality should use one of the extensions of this
 * class which is visible to them. These extensions may add additional features like
 * security to this class.
 */
class APIConsumerImpl extends AbstractAPIManager implements APIConsumer {

    private static final Log log = LogFactory.getLog(APIConsumerImpl.class);

    /* Map to Store APIs against Tag */
    private Map<String, Set<API>> taggedAPIs;
    private boolean isTenantModeStoreView;
    private String requestedTenant;

    public APIConsumerImpl() throws APIManagementException {
        super();
    }

    public APIConsumerImpl(String username) throws APIManagementException {
        super(username);
    }

    public Subscriber getSubscriber(String subscriberId) throws APIManagementException {
        Subscriber subscriber = null;
        try {
            subscriber = apiMgtDAO.getSubscriber(subscriberId);
        } catch (APIManagementException e) {
            handleException("Failed to get Subscriber", e);
        }
        return subscriber;
    }


    /**
     * Returns the set of APIs with the given tag from the taggedAPIs Map
     *
     * @param tag
     * @return
     * @throws APIManagementException
     */
    public Set<API> getAPIsWithTag(String tag) throws APIManagementException {
        return taggedAPIs.get(tag);
    }

    /**
     * Returns the set of APIs with the given tag from the taggedAPIs Map
     *
     * @param tag
     * @return
     * @throws APIManagementException
     */
    public Map<String,Object> getPaginatedAPIsWithTag(String tag,int start,int end) throws APIManagementException {
        List<API> apiSet = new ArrayList<API>();
        Set<API> resultSet = new TreeSet<API>(new APIVersionComparator());
        Map<String,Object> results = new HashMap<String, Object>();
        Set<API> taggedAPISet=taggedAPIs.get(tag);
        if(taggedAPISet!=null){
        if(taggedAPISet.size()<end){
        end=taggedAPISet.size();
        }

        apiSet.addAll(taggedAPIs.get(tag));
        for(int i=start;i<end;i++) {
          resultSet.add(apiSet.get(i));
        }

            results.put("apis",resultSet);
            results.put("length",taggedAPISet.size());
        }else{
            results.put("apis",null);
            results.put("length",0);

        }
        return results ;
    }


    /**
     * Returns the set of APIs with the given tag, retrieved from registry
     *
     * @param registry - Current registry; tenant/SuperTenant
     * @param tag
     * @return
     * @throws APIManagementException
     */
    private Set<API> getAPIsWithTag(Registry registry, String tag)
            throws APIManagementException {
        Set<API> apiSet = new TreeSet<API>(new APINameComparator());
        boolean isTenantFlowStarted = false;
        try {
        	if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
        		isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        	}
            String resourceByTagQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/resource-by-tag";
            Map<String, String> params = new HashMap<String, String>();
            params.put("1", tag);
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCE_UUID_RESULT_TYPE);
            Collection collection = registry.executeQuery(resourceByTagQueryPath, params);

            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);

            for (String row : collection.getChildren()) {
                String uuid = row.substring(row.indexOf(";") + 1, row.length());
                GenericArtifact genericArtifact = artifactManager.getGenericArtifact(uuid);
                if (genericArtifact != null && genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS).equals(APIConstants.PUBLISHED)) {
                apiSet.add(APIUtil.getAPI(genericArtifact));
                }
            }

        } catch (RegistryException e) {
            handleException("Failed to get API for tag " + tag, e);
        } finally {
        	if (isTenantFlowStarted) {
        		PrivilegedCarbonContext.endTenantFlow();
        	}
        }
        return apiSet;
    }

    /**
     * The method to get APIs to Store view      *
     *
     * @return Set<API>  Set of APIs
     * @throws APIManagementException
     */
    public Set<API> getAllPublishedAPIs(String tenantDomain) throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Registry userRegistry;
            boolean isTenantMode=(tenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(tenantDomain))) {//Tenant store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = registry;
            }
            this.isTenantModeStoreView = isTenantMode;
            this.requestedTenant = tenantDomain;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(userRegistry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                    return apiSortedSet;
                }

                Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
                List<API> multiVersionedAPIs = new ArrayList<API>();
                Comparator<API> versionComparator = new APIVersionComparator();
                Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
                Boolean displayAPIsWithMultipleStatus = isAllowDisplayAPIsWithMultipleStatus();
                for (GenericArtifact artifact : genericArtifacts) {
                    // adding the API provider can mark the latest API .
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!displayAPIsWithMultipleStatus) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
                if (!displayMultipleVersions) {
                    for (API api : latestPublishedAPIs.values()) {
                        apiSortedSet.add(api);
                    }
                    return apiSortedSet;
                } else {
                    for (API api : multiVersionedAPIs) {
                        apiVersionsSortedSet.add(api);
                    }
                    return apiVersionsSortedSet;
                }
            }

        } catch (RegistryException e) {
            handleException("Failed to get all published APIs", e);
        } catch (UserStoreException e) {
            handleException("Failed to get all published APIs", e);
        }
        return apiSortedSet;

    }


    /**
     * The method to get APIs to Store view      *
     *
     * @return Set<API>  Set of APIs
     * @throws APIManagementException
     */
    public Map<String,Object> getAllPaginatedPublishedAPIs(String tenantDomain,int start,int end) throws APIManagementException {
    	
    	Boolean displayAPIsWithMultipleStatus = isAllowDisplayAPIsWithMultipleStatus();
    	Map<String, List<String>> listMap = new HashMap<String, List<String>>();
        //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
        if (!displayAPIsWithMultipleStatus) {
            //Create the search attribute map
            listMap.put(APIConstants.API_OVERVIEW_STATUS, new ArrayList<String>() {{
                add(APIConstants.PUBLISHED);
            }});
        } else{
            return getAllPaginatedAPIs(tenantDomain, start, end);
        }
        
        
        Map<String,Object> result=new HashMap<String, Object>();
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        int totalLength=0;
        try {
            Registry userRegistry;
            boolean isTenantMode=(tenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(tenantDomain))) {//Tenant store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = registry;
            }
            this.isTenantModeStoreView = isTenantMode;
            this.requestedTenant = tenantDomain;
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(this.username);

            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
            
            PaginationContext.init(start, end, "ASC", APIConstants.API_OVERVIEW_NAME, Integer.MAX_VALUE);
            
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(userRegistry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager.findGenericArtifacts(listMap);
                totalLength=PaginationContext.getInstance().getLength();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                    result.put("apis",apiSortedSet);
                    result.put("totalLength",totalLength);
                    return result;
                }

                for (GenericArtifact artifact : genericArtifacts) {
                    // adding the API provider can mark the latest API .
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api  = APIUtil.getAPI(artifact);

                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
                if (!displayMultipleVersions) {
                    for (API api : latestPublishedAPIs.values()) {
                        apiSortedSet.add(api);
                    }
                    result.put("apis",apiSortedSet);
                    result.put("totalLength",totalLength);
                    return result;

                } else {
                    for (API api : multiVersionedAPIs) {
                        apiVersionsSortedSet.add(api);
                    }
                    result.put("apis",apiVersionsSortedSet);
                    result.put("totalLength",totalLength);
                    return result;

                }
            }

        } catch (RegistryException e) {
            handleException("Failed to get all published APIs", e);
        } catch (UserStoreException e) {
            handleException("Failed to get all published APIs", e);
        }finally {
            PaginationContext.destroy();
        }
        result.put("apis",apiSortedSet);
        result.put("totalLength",totalLength);
        return result;

    }
    
    /**
     * The method to get All PUBLISHED and DEPRECATED APIs, to Store view      
     *
     * @return Set<API>  Set of APIs
     * @throws APIManagementException
     */
    public Map<String,Object> getAllPaginatedAPIs(String tenantDomain,int start,int end) throws APIManagementException {
        Map<String,Object> result=new HashMap<String, Object>();
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        int totalLength=0;
        try {
            Registry userRegistry;
            boolean isTenantMode=(tenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(tenantDomain))) {//Tenant store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = registry;
            }
            this.isTenantModeStoreView = isTenantMode;
            this.requestedTenant = tenantDomain;
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(this.username);

            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
            
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(userRegistry, APIConstants.API_KEY);
            
            PaginationContext.init(start, end, "ASC", APIConstants.API_OVERVIEW_NAME, Integer.MAX_VALUE);
                       
           
            boolean noPublishedAPIs = false;
            if (artifactManager != null) {
            	
            	//Create the search attribute map for PUBLISHED APIs
            	Map<String, List<String>> listMap = new HashMap<String, List<String>>();
                listMap.put(APIConstants.API_OVERVIEW_STATUS, new ArrayList<String>() {{
                        add(APIConstants.PUBLISHED);
                    }});
                
                GenericArtifact[] genericArtifacts = artifactManager.findGenericArtifacts(listMap);
                totalLength = PaginationContext.getInstance().getLength();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                	noPublishedAPIs = true;
                }

                for (GenericArtifact artifact : genericArtifacts) {
                    // adding the API provider can mark the latest API .
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api  = APIUtil.getAPI(artifact);

                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
                
                //Create the search attribute map for DEPRECATED APIs
                Map<String, List<String>> listMapForDeprecatedAPIs = new HashMap<String, List<String>>();
                listMapForDeprecatedAPIs.put(APIConstants.API_OVERVIEW_STATUS, new ArrayList<String>() {{
                        add(APIConstants.DEPRECATED);
                    }});
                
                GenericArtifact[] genericArtifactsForDeprecatedAPIs = artifactManager.findGenericArtifacts(listMapForDeprecatedAPIs);
                totalLength = totalLength + PaginationContext.getInstance().getLength();
                if ((genericArtifactsForDeprecatedAPIs == null || genericArtifactsForDeprecatedAPIs.length == 0) && noPublishedAPIs) {
                	result.put("apis",apiSortedSet);
                    result.put("totalLength",totalLength);
                    return result;
                }

                for (GenericArtifact artifact : genericArtifactsForDeprecatedAPIs) {
                    // adding the API provider can mark the latest API .
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api  = APIUtil.getAPI(artifact);

                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
                
                if (!displayMultipleVersions) {
                    for (API api : latestPublishedAPIs.values()) {
                        apiSortedSet.add(api);
                    }
                    result.put("apis",apiSortedSet);
                    result.put("totalLength",totalLength);
                    return result;

                } else {
                    for (API api : multiVersionedAPIs) {
                        apiVersionsSortedSet.add(api);
                    }
                    result.put("apis",apiVersionsSortedSet);
                    result.put("totalLength",totalLength);
                    return result;

                }
            }

        } catch (RegistryException e) {
            handleException("Failed to get all published APIs", e);
        } catch (UserStoreException e) {
            handleException("Failed to get all published APIs", e);
        }finally {
            PaginationContext.destroy();
        }
        result.put("apis",apiSortedSet);
        result.put("totalLength",totalLength);
        return result;

    }

    private <T> T[] concatArrays(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


    public Set<API> getTopRatedAPIs(int limit) throws APIManagementException {
        int returnLimit = 0;
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSortedSet;
            }
            for (GenericArtifact genericArtifact : genericArtifacts) {
                String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                if (status.equals(APIConstants.PUBLISHED)) {
                    String artifactPath = genericArtifact.getPath();

                    float rating = registry.getAverageRating(artifactPath);
                    if (rating > APIConstants.TOP_TATE_MARGIN && (returnLimit < limit)) {
                        returnLimit++;
                        apiSortedSet.add(APIUtil.getAPI(genericArtifact, registry));
                    }
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to get top rated API", e);
        }
        return apiSortedSet;
    }

    /**
     * Get the recently added APIs set
     *
     * @param limit no limit. Return everything else, limit the return list to specified value.
     * @return Set<API>
     * @throws APIManagementException
     */
    public Set<API> getRecentlyAddedAPIs(int limit, String tenantDomain)
            throws APIManagementException {
        SortedSet<API> recentlyAddedAPIs = new TreeSet<API>(new APINameComparator());
        SortedSet<API> recentlyAddedAPIsWithMultipleVersions = new TreeSet<API>(new APIVersionComparator());
        Registry userRegistry = null;
        String latestAPIQueryPath = null;
        try {
            boolean isTenantMode=(tenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(tenantDomain))) {//Tenant based store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = registry;
            }

            latestAPIQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/latest-apis";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCES_RESULT_TYPE);
            if (userRegistry != null) {
                Collection collection = userRegistry.executeQuery(latestAPIQueryPath, params);
                int resultSetSize = Math.min(limit, collection.getChildCount());
                String[] recentlyAddedAPIPaths = new String[resultSetSize];
                for (int i = 0; i < resultSetSize; i++) {
                    recentlyAddedAPIPaths[i] = collection.getChildren()[i];
                }
                Set<API> apisSet = getAPIs(userRegistry, limit, recentlyAddedAPIPaths);
                if (!isAllowDisplayMultipleVersions()) {

                recentlyAddedAPIs.addAll(apisSet);
                    return recentlyAddedAPIs;
                }else{
                recentlyAddedAPIsWithMultipleVersions.addAll(apisSet);
                return recentlyAddedAPIsWithMultipleVersions;
                }
            }
         return recentlyAddedAPIs;



        } catch (RegistryException e) {
        	try {
        		//Before a tenant login to the store or publisher at least one time, 
        		//a registry exception is thrown when the tenant store is accessed the store in anonymous mode.
        		//This fix checks whether query resource available in the registry. If not
        		// give a warn. 
				if (!userRegistry.resourceExists(latestAPIQueryPath)) {
					log.warn("Failed to retrieve recently added API query resource at " + latestAPIQueryPath);
					return recentlyAddedAPIs;
				}
			} catch (RegistryException e1) {
				//ignore
			}
            handleException("Failed to get recently added APIs", e);
            return null;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Failed to get recently added APIs", e);
            return null;
        }


    }

    public Set<Tag> getAllTags(String requestedTenantDomain) throws APIManagementException {
        Set<Tag> tagSet = new TreeSet<Tag>(new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.isTenantModeStoreView = (requestedTenantDomain != null &&
                !requestedTenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME));

        if(requestedTenantDomain != null){
            this.requestedTenant = requestedTenantDomain;
        }
        taggedAPIs = new HashMap<String, Set<API>>();
        Registry userRegistry = null;
        String tagsQueryPath = null;
        try {
            tagsQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/tag-summary";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.TAG_SUMMARY_RESULT_TYPE);
            if ((this.isTenantModeStoreView && this.tenantDomain==null) || (this.isTenantModeStoreView && isTenantDomainNotMatching(requestedTenantDomain))) {//Tenant based store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(this.requestedTenant);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = registry;
            }
            Collection collection = userRegistry.executeQuery(tagsQueryPath, params);
            for (String fullTag : collection.getChildren()) {
                //remove hardcoded path value
                String tagName = fullTag.substring(fullTag.indexOf(";") + 1, fullTag.indexOf(":"));
                String tagOccurenceCountStr = fullTag.substring(fullTag.indexOf(":") + 1, fullTag.length());
                int tagOccurenceCount = Integer.valueOf(tagOccurenceCountStr);
                Set<API> apisWithTag = getAPIsWithTag(userRegistry, tagName);
                    //tagSet.add(new Tag(tagName, tagOccurenceCount));
                    /* Add the APIs against the tag name */

                    if (apisWithTag.size() != 0) {
                        if (taggedAPIs.containsKey(tagName)) {
                            for (API api : apisWithTag) {
                                taggedAPIs.get(tagName).add(api);
                            }
                        } else {
                            taggedAPIs.put(tagName, apisWithTag);
                        }
                        tagSet.add(new Tag(tagName, tagOccurenceCount));
                    }


            }


        } catch (RegistryException e) {
        	try {
        		//Before a tenant login to the store or publisher at least one time, 
        		//a registry exception is thrown when the tenant store is accessed in anonymous mode.
        		//This fix checks whether query resource available in the registry. If not
        		// give a warn. 
				if (!userRegistry.resourceExists(tagsQueryPath)) {
					log.warn("Failed to retrieve tags query resource at " + tagsQueryPath);
					return tagSet;
				}
			} catch (RegistryException e1) {
				//ignore
			}
            handleException("Failed to get all the tags", e);
        } catch (UserStoreException e) {
            handleException("Failed to get all the tags", e);
        }
        return tagSet;
    }

    public void rateAPI(APIIdentifier apiId, APIRating rating,
                        String user) throws APIManagementException {
        apiMgtDAO.addRating(apiId, rating.getRating(), user);

    }

    public void removeAPIRating(APIIdentifier apiId, String user) throws APIManagementException {
        apiMgtDAO.removeAPIRating(apiId, user);

    }

    public int getUserRating(APIIdentifier apiId, String user) throws APIManagementException {
        return apiMgtDAO.getUserRating(apiId, user);
    }

    public Set<API> getPublishedAPIsByProvider(String providerId, int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean displayAPIsWithMultipleStatus = isAllowDisplayAPIsWithMultipleStatus();
            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                    providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                    APIConstants.PROVIDER_ASSOCIATION);
            if (associations.length < limit || limit == -1) {
                limit = associations.length;
            }
            for (int i = 0; i < limit; i++) {
                Association association = associations[i];
                String apiPath = association.getDestinationPath();
                Resource resource = registry.get(apiPath);
                String apiArtifactId = resource.getUUID();
                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);
                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!displayAPIsWithMultipleStatus) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                } else {
                    throw new GovernanceException("artifact id is null of " + apiPath);
                }
            }
            if (!displayMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }


    }

    public Set<API> getPublishedAPIsByProvider(String providerId, String loggedUsername, int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean showAllAPIs = isAllowDisplayAPIsWithMultipleStatus();

            String providerDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(providerId));
            int id = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(providerDomain);
            Registry registry = ServiceReferenceHolder.getInstance().
                    getRegistryService().getGovernanceSystemRegistry(id);

            org.wso2.carbon.user.api.AuthorizationManager manager = ServiceReferenceHolder.getInstance().
                    getRealmService().getTenantUserRealm(id).
                    getAuthorizationManager();

            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                                  providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                                                                                APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                                                                  APIConstants.PROVIDER_ASSOCIATION);
            int publishedAPICount = 0;

            for (Association association1 : associations) {

                if (publishedAPICount >= limit) {
                    break;
                }

                Association association = association1;
                String apiPath = association.getDestinationPath();

                Resource resource;
                String path = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                                                            RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + apiPath);
                boolean checkAuthorized = false;
                String userNameWithoutDomain = loggedUsername;

                String loggedDomainName = "";
                if (!"".equals(loggedUsername) &&
                    !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(super.tenantDomain)) {
                    String[] nameParts = loggedUsername.split("@");
                    loggedDomainName = nameParts[1];
                    userNameWithoutDomain = nameParts[0];
                }

                if (loggedUsername.equals("")) {
                    // Anonymous user is viewing.
                    checkAuthorized = manager.isRoleAuthorized(APIConstants.ANONYMOUS_ROLE, path, ActionConstants.GET);
                } else {
                    // Some user is logged in.
                    checkAuthorized = manager.isUserAuthorized(userNameWithoutDomain, path, ActionConstants.GET);
                }

                String apiArtifactId = null;
                if (checkAuthorized) {
                    resource = registry.get(apiPath);
                    apiArtifactId = resource.getUUID();
                }

                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);

                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                            publishedAPICount++;
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);
                            publishedAPICount++;

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }

    }

    public Set<API> searchAPI(String searchTerm, String searchType, String requestedTenantDomain)
            throws APIManagementException {
        Set<API> apiSet = new HashSet<API>();
        try {
            Registry userRegistry;
            boolean isTenantMode=(requestedTenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(requestedTenantDomain))) {//Tenant store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(requestedTenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = this.registry;
            }
            apiSet.addAll(searchAPI(userRegistry, searchTerm, searchType));

        } catch (Exception e) {
            handleException("Failed to Search APIs", e);
        }
        return apiSet;
    }

    public Set<API> searchAPI(Registry registry, String searchTerm, String searchType) throws APIManagementException {
        SortedSet<API> apiSet = new TreeSet<API>(new APINameComparator());
        String regex = "(?i)[\\w.|-]*" + searchTerm.trim() + "[\\w.|-]*";
        Pattern pattern;
        Matcher matcher;
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager
                        .getAllGenericArtifacts();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                    return apiSet;
                }
                pattern = Pattern.compile(regex);

                for (GenericArtifact artifact : genericArtifacts) {
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    if (searchType.equalsIgnoreCase("Provider")) {
                        String api = APIUtil.replaceEmailDomainBack(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER));
                        matcher = pattern.matcher(api);
                    } else if (searchType.equalsIgnoreCase("Version")) {
                        String api = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
                        matcher = pattern.matcher(api);
                    } else if (searchType.equalsIgnoreCase("Context")) {
                        String api = artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT);
                        matcher = pattern.matcher(api);
                    } else {
                        String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                        matcher = pattern.matcher(apiName);
                    }
                    if (isAllowDisplayAPIsWithMultipleStatus()) {
                        if (matcher.find() && (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED))) {
                            apiSet.add(APIUtil.getAPI(artifact, registry));
                        }
                    } else {
                        if (matcher.find() && status.equals(APIConstants.PUBLISHED)) {
                            apiSet.add(APIUtil.getAPI(artifact, registry));
                        }
                    }

                }
            }
        } catch (RegistryException e) {
            handleException("Failed to search APIs with type", e);
        }
        return apiSet;
    }

    public Map<String,Object> searchPaginatedAPIs(String searchTerm, String searchType, String requestedTenantDomain,int start,int end)
            throws APIManagementException {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            Registry userRegistry;
            boolean isTenantMode=(requestedTenantDomain != null);
            if ((isTenantMode && this.tenantDomain==null) || (isTenantMode && isTenantDomainNotMatching(requestedTenantDomain))) {//Tenant store anonymous mode
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(requestedTenantDomain);
                userRegistry = ServiceReferenceHolder.getInstance().
                        getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenantId);
            } else {
                userRegistry = this.registry;
            }
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(this.username);
            result=searchPaginatedAPIs(userRegistry, searchTerm, searchType,start,end);

        } catch (Exception e) {
            handleException("Failed to Search APIs", e);
        }
        return result;
    }

    public Map<String,Object> searchPaginatedAPIs(Registry registry, String searchTerm, String searchType,int start,int end) throws APIManagementException {
        SortedSet<API> apiSet = new TreeSet<API>(new APINameComparator());
        List<API> apiList = new ArrayList<API>();
        String regex = "(?i)[\\w.|-]*" + searchTerm.trim() + "[\\w.|-]*";
        Pattern pattern;
        Matcher matcher;
        Map<String,Object> result=new HashMap<String, Object>();
        try {

            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager
                        .getAllGenericArtifacts();
                if (genericArtifacts == null || genericArtifacts.length == 0) {

                    result.put("apis",apiSet);
                    result.put("length",0);
                    return result;
                }
                pattern = Pattern.compile(regex);

                for (GenericArtifact artifact : genericArtifacts) {
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    if (searchType.equalsIgnoreCase("Provider")) {
                        String api = APIUtil.replaceEmailDomainBack(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER));
                        matcher = pattern.matcher(api);
                    } else if (searchType.equalsIgnoreCase("Version")) {
                        String api = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
                        matcher = pattern.matcher(api);
                    } else if (searchType.equalsIgnoreCase("Context")) {
                        String api = artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT);
                        matcher = pattern.matcher(api);
                    } else {
                        String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                        matcher = pattern.matcher(apiName);
                    }
                    if (isAllowDisplayAPIsWithMultipleStatus()) {
                        if (matcher.find() && (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED))) {
                            apiList.add(APIUtil.getAPI(artifact, registry));
                        }
                    } else {
                        if (matcher.find() && status.equals(APIConstants.PUBLISHED)) {
                            apiList.add(APIUtil.getAPI(artifact, registry));
                        }
                    }

                }
                if(apiList.size()<end){
                    end=apiList.size();
                }
                for(int i=start;i<end;i++){
                   apiSet.add(apiList.get(i));


                }
            }
        } catch (RegistryException e) {
            handleException("Failed to search APIs with type", e);
        }
        result.put("apis",apiSet);
        result.put("length",apiList.size());
        return result;
    }

    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber) throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = null;
        try {
            subscribedAPIs = apiMgtDAO.getSubscribedAPIs(subscriber);
        } catch (APIManagementException e) {
            handleException("Failed to get APIs of " + subscriber.getName(), e);
        }
        return subscribedAPIs;
    }

    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber, String applicationName) throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = null;
        try {
            subscribedAPIs = apiMgtDAO.getSubscribedAPIs(subscriber, applicationName);
        } catch (APIManagementException e) {
            handleException("Failed to get APIs of " + subscriber.getName() + " under application " + applicationName, e);
        }
        return subscribedAPIs;
    }

    public Set<APIIdentifier> getAPIByConsumerKey(String accessToken) throws APIManagementException {
        try {
            return apiMgtDAO.getAPIByConsumerKey(accessToken);
        } catch (APIManagementException e) {
            handleException("Error while obtaining API from API key", e);
        }
        return null;
    }

    public boolean isSubscribed(APIIdentifier apiIdentifier, String userId)
            throws APIManagementException {
        boolean isSubscribed;
        try {
            isSubscribed = apiMgtDAO.isSubscribed(apiIdentifier, userId);
        } catch (APIManagementException e) {
            String msg = "Failed to check if user(" + userId + ") has subscribed to " + apiIdentifier;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return isSubscribed;
    }

    public void addSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        if (api.getStatus().equals(APIStatus.PUBLISHED)) {
            apiMgtDAO.addSubscription(identifier, api.getContext(), applicationId);
            if (APIUtil.isAPIGatewayKeyCacheEnabled()) {
                invalidateCachedKeys(applicationId, identifier);
            }
        } else {
            throw new APIManagementException("Subscriptions not allowed on APIs in the state: " +
                    api.getStatus().getStatus());
        }
    }

    public void removeSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        apiMgtDAO.removeSubscription(identifier, applicationId);
        if (APIUtil.isAPIGatewayKeyCacheEnabled()) {
            invalidateCachedKeys(applicationId, identifier);
        }
    }

    private void invalidateCachedKeys(int applicationId, APIIdentifier identifier) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        if (config.getApiGatewayEnvironments().size() <= 0) {
            return;
        }

        Set<String> keys = apiMgtDAO.getApplicationKeys(applicationId);
        if (keys.size() > 0) {
            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            API api = getAPI(identifier);
            for (String key : keys) {
                APIKeyMapping mapping = new APIKeyMapping();
                mapping.setKey(key);
                mapping.setApiVersion(identifier.getVersion());
                mapping.setContext(api.getContext());
                mappings.add(mapping);
            }

            try {
                List<Environment> gatewayEnvs = config.getApiGatewayEnvironments();
                for (Environment environment : gatewayEnvs) {
                    APIAuthenticationAdminClient client = new APIAuthenticationAdminClient(environment);
                    client.invalidateKeys(mappings);
                }
            } catch (AxisFault axisFault) {
                log.warn("Error while invalidating API keys at the gateway", axisFault);
            }
        }
    }

    public void removeSubscriber(APIIdentifier identifier, String userId)
            throws APIManagementException {
        throw new UnsupportedOperationException("Unsubscribe operation is not yet implemented");
    }

    public void updateSubscriptions(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        apiMgtDAO.updateSubscriptions(identifier, api.getContext(), applicationId);
    }

    public void addComment(APIIdentifier identifier, String commentText, String user) throws APIManagementException {
        apiMgtDAO.addComment(identifier, commentText, user);
    }

    public org.wso2.carbon.apimgt.api.model.Comment[] getComments(APIIdentifier identifier)
            throws APIManagementException {
        return apiMgtDAO.getComments(identifier);
    }

    public void addApplication(Application application, String userId)
            throws APIManagementException {
        apiMgtDAO.addApplication(application, userId);
    }

    public void updateApplication(Application application) throws APIManagementException {
        apiMgtDAO.updateApplication(application);
    }

    public void removeApplication(Application application) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        boolean gatewayExists = config.getApiGatewayEnvironments().size() > 0;
        Set<SubscribedAPI> apiSet = null;
        Set<String> keys = null;
        if (gatewayExists) {
            keys = apiMgtDAO.getApplicationKeys(application.getId());
            apiSet = getSubscribedAPIs(application.getSubscriber());
        }
        apiMgtDAO.deleteApplication(application);

        if (gatewayExists && apiSet != null && keys != null) {
            Set<SubscribedAPI> removables = new HashSet<SubscribedAPI>();
            for (SubscribedAPI api : apiSet) {
                if (!api.getApplication().getName().equals(application.getName())) {
                    removables.add(api);
                }
            }

            for (SubscribedAPI api : removables) {
                apiSet.remove(api);
            }

            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            for (String key : keys) {
                for (SubscribedAPI api : apiSet) {
                    APIKeyMapping mapping = new APIKeyMapping();
                    API apiDefinition = getAPI(api.getApiId());
                    mapping.setApiVersion(api.getApiId().getVersion());
                    mapping.setContext(apiDefinition.getContext());
                    mapping.setKey(key);
                    mappings.add(mapping);
                }
            }

            if (mappings.size() > 0) {
                try {
                    List<Environment> gatewayEnvs = config.getApiGatewayEnvironments();
                    for (Environment environment : gatewayEnvs) {
                        APIAuthenticationAdminClient client =
                                new APIAuthenticationAdminClient(environment);
                        client.invalidateKeys(mappings);
                    }
                } catch (AxisFault axisFault) {
                    // Just logging the error is enough - We have already deleted the application
                    // which is what's important
                    log.warn("Error while invalidating API keys at the gateway", axisFault);
                }
            }
        }
    }

    public Application[] getApplications(Subscriber subscriber) throws APIManagementException {
        return apiMgtDAO.getApplications(subscriber);
    }

    public boolean isApplicationTokenExists(String accessToken) throws APIManagementException {
        return apiMgtDAO.isAccessTokenExists(accessToken);
    }

    public Set<SubscribedAPI> getSubscribedIdentifiers(Subscriber subscriber, APIIdentifier identifier)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPISet = new HashSet<SubscribedAPI>();
        Set<SubscribedAPI> subscribedAPIs = getSubscribedAPIs(subscriber);
        for (SubscribedAPI api : subscribedAPIs) {
            if (api.getApiId().equals(identifier)) {
                subscribedAPISet.add(api);
            }
        }
        return subscribedAPISet;
    }

    private boolean isAllowDisplayAPIsWithMultipleStatus() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String displayAllAPIs = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_ALL_APIS);
        if (displayAllAPIs == null) {
            log.warn("The configurations related to show deprecated APIs in APIStore " +
                    "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayAllAPIs);
    }

    private boolean isAllowDisplayMultipleVersions() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        String displayMultiVersions = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_MULTIPLE_VERSIONS);
        if (displayMultiVersions == null) {
            log.warn("The configurations related to show multiple versions of API in APIStore " +
                    "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayMultiVersions);
    }


    public void updateAccessAllowDomains(String accessToken, String[] accessAllowDomains)
            throws APIManagementException {
        apiMgtDAO.updateAccessAllowDomains(accessToken, accessAllowDomains);
    }

    /**
     * Returns a list of tiers denied
     *
     * @return Set<Tier>
     */
    public Set<String> getDeniedTiers() throws APIManagementException {
        Set<String> deniedTiers = new HashSet<String>();
        String[] currentUserRoles = new String[0];
        try {
            if (tenantId != 0) {
                /* Get the roles of the Current User */
                currentUserRoles = ((UserRegistry) ((UserAwareAPIConsumer) this).registry).
                        getUserRealm().getUserStoreManager().getRoleListOfUser(((UserRegistry) this.registry).getUserName());

                Set<TierPermissionDTO> tierPermissions = apiMgtDAO.getTierPermissions(tenantId);
                for (TierPermissionDTO tierPermission : tierPermissions) {
                    String type = tierPermission.getPermissionType();

                    List<String> currentRolesList = new ArrayList<String>(Arrays.asList(currentUserRoles));
                    List<String> roles = new ArrayList<String>(Arrays.asList(tierPermission.getRoles()));
                    currentRolesList.retainAll(roles);

                    if (APIConstants.TIER_PERMISSION_ALLOW.equals(type)) {
                        /* Current User is not allowed for this Tier*/
                        if (currentRolesList.size() == 0) {
                            deniedTiers.add(tierPermission.getTierName());
                        }
                    } else {
                        /* Current User is denied for this Tier*/
                        if (currentRolesList.size() > 0) {
                            deniedTiers.add(tierPermission.getTierName());
                        }
                    }
                }
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("cannot retrieve user role list for tenant" + tenantDomain);
        }
        return deniedTiers;
    }

    /**
     * Check whether given Tier is denied for the user
     *
     * @param tierName
     * @return
     * @throws APIManagementException if failed to get the tiers
     */
    public boolean isTierDeneid(String tierName) throws APIManagementException {
        String[] currentUserRoles = new String[0];
        try {
            if (tenantId != 0) {
                /* Get the roles of the Current User */
                currentUserRoles = ((UserRegistry) ((UserAwareAPIConsumer) this).registry).
                        getUserRealm().getUserStoreManager().getRoleListOfUser(((UserRegistry) this.registry).getUserName());
                TierPermissionDTO tierPermission = apiMgtDAO.getTierPermission(tierName, tenantId);
                if (tierPermission == null) {
                    return false;
                } else {
                    List<String> currentRolesList = new ArrayList<String>(Arrays.asList(currentUserRoles));
                    List<String> roles = new ArrayList<String>(Arrays.asList(tierPermission.getRoles()));
                    currentRolesList.retainAll(roles);
                    if (APIConstants.TIER_PERMISSION_ALLOW.equals(tierPermission.getPermissionType())) {
                        if (currentRolesList.size() == 0) {
                            return true;
                        }
                    } else {
                        if (currentRolesList.size() > 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            log.error("cannot retrieve user role list for tenant" + tenantDomain);
        }
        return false;
    }

    /**
     * Returned an API set from a set of registry paths
     *
     * @param registry Registry object from which the APIs retrieving,
     * @param limit    Specifies the number of APIs to add.
     * @param apiPaths Array of API paths.
     * @return Set<API> set of APIs
     * @throws RegistryException
     * @throws APIManagementException
     */
    private Set<API> getAPIs(Registry registry, int limit, String[] apiPaths)
            throws RegistryException, APIManagementException,
            org.wso2.carbon.user.api.UserStoreException {

        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());

        Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
        Boolean showAllAPIs = isAllowDisplayAPIsWithMultipleStatus();
        Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
        List<API> multiVersionedAPIs = new ArrayList<API>();
        Comparator<API> versionComparator = new APIVersionComparator();

        //Find UUID
        GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                APIConstants.API_KEY);
        for (int a = 0; a < apiPaths.length; a++) {
            Resource resource = registry.get(apiPaths[a]);
            if (resource != null && artifactManager != null) {
                GenericArtifact genericArtifact = artifactManager.getGenericArtifact(resource.getUUID());
                API api = null;
                String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                if (!showAllAPIs) {
                    // then we are only interested in published APIs here...
                    if (status.equals(APIConstants.PUBLISHED)) {
                        api = APIUtil.getAPI(genericArtifact, registry);
                    }
                } else {   // else we are interested in both deprecated/published APIs here...
                    if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                        api = APIUtil.getAPI(genericArtifact, registry);

                    }

                }
                if (api != null) {
                    String key;
                    //Check the configuration to allow showing multiple versions of an API true/false
                    if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                        API existingAPI = latestPublishedAPIs.get(key);
                        if (existingAPI != null) {
                            // If we have already seen an API with the same name, make sure
                            // this one has a higher version number
                            if (versionComparator.compare(api, existingAPI) > 0) {
                                latestPublishedAPIs.put(key, api);
                            }
                        } else {
                            // We haven't seen this API before
                            latestPublishedAPIs.put(key, api);
                        }
                    } else { //If allow showing multiple versions of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                .getVersion();
                        multiVersionedAPIs.add(api);
                    }
                }

            }
        }
        if (!allowMultipleVersions) {
            for (API api : latestPublishedAPIs.values()) {
                apiSortedSet.add(api);
            }
            return apiSortedSet;
        } else {
            for (API api : multiVersionedAPIs) {
                apiVersionsSortedSet.add(api);
            }
            return apiVersionsSortedSet;
        }

    }

    private boolean isAllowDisplayAllAPIs() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String displayAllAPIs = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_ALL_APIS);
        if (displayAllAPIs == null) {
            log.warn("The configurations related to show deprecated APIs in APIStore " +
                    "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayAllAPIs);
    }
    
    private boolean isTenantDomainNotMatching(String tenantDomain) {
    	if (this.tenantDomain != null) {
    		return !(this.tenantDomain.equals(tenantDomain));
    	}
    	return true;
    }
}

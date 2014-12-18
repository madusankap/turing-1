/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.impl.utils;

import com.google.gson.Gson;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.doc.model.APIDefinition;
import org.wso2.carbon.apimgt.api.doc.model.APIResource;
import org.wso2.carbon.apimgt.api.doc.model.Operation;
import org.wso2.carbon.apimgt.api.doc.model.Parameter;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.internal.APIManagerComponent;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.ExceptionException;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfo;
import org.wso2.carbon.core.commons.stub.loggeduserinfo.LoggedUserInfoAdminStub;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.endpoints.EndpointManager;
import org.wso2.carbon.governance.api.endpoints.dataobjects.Endpoint;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.api.wsdls.WsdlManager;
import org.wso2.carbon.governance.api.wsdls.dataobjects.Wsdl;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.*;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.FileUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class contains the utility methods used by the implementations of APIManager, APIProvider
 * and APIConsumer interfaces.
 */
public final class APIUtil {

    private static final Log log = LogFactory.getLog(APIUtil.class);

    private static boolean isContextCacheInitialized = false;

    private static Set<Integer> registryInitializedTenants = new HashSet<Integer>();

    /**
     * This method used to get API from governance artifact
     *
     * @param artifact API artifact
     * @param registry Registry
     * @return API
     * @throws APIManagementException if failed to get API from artifact
     */
    public static API getAPI(GovernanceArtifact artifact, Registry registry)
            throws APIManagementException {

        API api;
        try {
            String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
            String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
            APIIdentifier apiId=new APIIdentifier(providerName, apiName, apiVersion);
            api = new API(apiId);
            // set rating
            String artifactPath = GovernanceUtils.getArtifactPath(registry, artifact.getId());
           // BigDecimal bigDecimal = new BigDecimal(getAverageRating(apiId));
            //BigDecimal res = bigDecimal.setScale(1, RoundingMode.HALF_UP);
            api.setRating(getAverageRating(apiId));
            //set description
            api.setDescription(artifact.getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION));
            //set last access time
            api.setLastUpdated(registry.get(artifactPath).getLastModified());
            // set url
            api.setUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_URL));
            api.setSandboxUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_SANDBOX_URL));
            api.setStatus(getApiStatus(artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS)));
            api.setThumbnailUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL));
            api.setWsdlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WSDL));
            api.setWadlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WADL));
            api.setTechnicalOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER));
            api.setTechnicalOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER_EMAIL));
            api.setBusinessOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER));
            api.setBusinessOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER_EMAIL));
            api.setVisibility(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY));
            api.setVisibleRoles(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES));
            api.setVisibleTenants(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS));
            api.setEndpointSecured(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_SECURED)));
            api.setEndpointUTUsername(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_USERNAME));
            api.setEndpointUTPassword(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_PASSWORD));
            api.setTransports(artifact.getAttribute(APIConstants.API_OVERVIEW_TRANSPORTS));
            api.setInSequence(artifact.getAttribute(APIConstants.API_OVERVIEW_INSEQUENCE));
            api.setOutSequence(artifact.getAttribute(APIConstants.API_OVERVIEW_OUTSEQUENCE));

            api.setRedirectURL(artifact.getAttribute(APIConstants.API_OVERVIEW_REDIRECT_URL));
            api.setApiOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_OWNER));
            api.setAdvertiseOnly(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ADVERTISE_ONLY)));
            
            String tenantDomainName = MultitenantUtils.getTenantDomain(replaceEmailDomainBack(providerName));
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(tenantDomainName);
            
            Set<Tier> availableTier = new HashSet<Tier>();
            String tiers = artifact.getAttribute(APIConstants.API_OVERVIEW_TIER);
            Map<String, Tier> definedTiers = getTiers(tenantId);
            if (tiers != null && !"".equals(tiers)) {
                String[] tierNames = tiers.split("\\|\\|");
                for (String tierName : tierNames) {
                    Tier definedTier = definedTiers.get(tierName);
                    if (definedTier != null) {
                        availableTier.add(definedTier);
                    } else {
                        log.warn("Unknown tier: " + tierName + " found on API: " + apiName);
                    }
                }
            }
            api.addAvailableTiers(availableTier);
            api.setContext(artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT));
            api.setLatest(Boolean.valueOf(artifact.getAttribute(APIConstants.API_OVERVIEW_IS_LATEST)));


            Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
            List<String> uriTemplateNames = new ArrayList<String>();


            HashMap<String,String> urlPatternsSet;
            urlPatternsSet = ApiMgtDAO.getURITemplatesPerAPIAsString(api.getId());
            Set<String> urlPatternsKeySet = urlPatternsSet.keySet();
            for (String urlPattern : urlPatternsKeySet) {
                    URITemplate uriTemplate = new URITemplate();
                    String uTemplate = urlPattern.split("::")[0];
                    String method = urlPattern.split("::")[1];
                    String authType = urlPattern.split("::")[2];
                    String throttlingTier = urlPattern.split("::")[3];
                    uriTemplate.setHTTPVerb(method);
                    uriTemplate.setAuthType(authType);
                    uriTemplate.setThrottlingTier(throttlingTier);
                    uriTemplate.setHttpVerbs(method);
                    uriTemplate.setAuthTypes(authType);
                    uriTemplate.setUriTemplate(uTemplate);
                    uriTemplate.setResourceURI(api.getUrl());
                    uriTemplate.setResourceSandboxURI(api.getSandboxUrl());
                    uriTemplate.setThrottlingTiers(throttlingTier);
                    //Checking for duplicate uri template names
                    if (uriTemplateNames.contains(uTemplate)) {
                        for (URITemplate tmp : uriTemplates) {
                            if (uTemplate.equals(tmp.getUriTemplate())) {
                                tmp.setHttpVerbs(method);
                                tmp.setAuthTypes(authType);
                                tmp.setThrottlingTiers(throttlingTier);
                                break;
                            }
                        }

                    } else {
                        uriTemplates.add(uriTemplate);
                    }

                    uriTemplateNames.add(uTemplate);


                }
            api.setUriTemplates(uriTemplates);


            Set<String> tags = new HashSet<String>();
            org.wso2.carbon.registry.core.Tag[] tag = registry.getTags(artifactPath);
            for (Tag tag1 : tag) {
                tags.add(tag1.getTagName());
            }
            api.addTags(tags);
            api.setLastUpdated(registry.get(artifactPath).getLastModified());

        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        } catch (RegistryException e) {
            String msg = "Failed to get LastAccess time or Rating";
            throw new APIManagementException(msg, e);
        } catch (UserStoreException e){
            String msg = "Failed to get User Realm of API Provider";
            throw new APIManagementException(msg, e);
        }
        return api;
    }

    public static API getAPI(GovernanceArtifact artifact)
            throws APIManagementException {

        API api;
        try {
            String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
            String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
            api = new API(new APIIdentifier(providerName, apiName, apiVersion));
            api.setThumbnailUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL));
            api.setStatus(getApiStatus(artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS)));
            api.setContext(artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT));
            api.setVisibility(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY));
            api.setVisibleRoles(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES));
            api.setVisibleTenants(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS));
            api.setTransports(artifact.getAttribute(APIConstants.API_OVERVIEW_TRANSPORTS));
            api.setInSequence(artifact.getAttribute(APIConstants.API_OVERVIEW_INSEQUENCE));
            api.setInSequence(artifact.getAttribute(APIConstants.API_OVERVIEW_OUTSEQUENCE));
            api.setDescription(artifact.getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION));

            api.setRedirectURL(artifact.getAttribute(APIConstants.API_OVERVIEW_REDIRECT_URL));
            api.setApiOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_OWNER));
            api.setAdvertiseOnly(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ADVERTISE_ONLY)));
            
        } catch (GovernanceException e) {
            String msg = "Failed to get API from artifact ";
            throw new APIManagementException(msg, e);
        }
        return api;
    }

    /**
     * This method used to get Provider from provider artifact
     *
     * @param artifact provider artifact
     * @return Provider
     * @throws APIManagementException if failed to get Provider from provider artifact.
     */
    public static Provider getProvider(GenericArtifact artifact) throws APIManagementException {
        Provider provider;
        try {
            provider =
                    new Provider(artifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_NAME));
            provider.setDescription(artifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_DESCRIPTION));
            provider.setEmail(artifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_EMAIL));

        } catch (GovernanceException e) {
            String msg = "Failed to get provider ";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return provider;
    }

    /**
     * Create Governance artifact from given attributes
     *
     * @param artifact initial governance artifact
     * @param api      API object with the attributes value
     * @return GenericArtifact
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to create API
     */
    public static GenericArtifact createAPIArtifactContent(GenericArtifact artifact, API api)
            throws APIManagementException {
        try {
            String apiStatus = api.getStatus().getStatus();
            artifact.setAttribute(APIConstants.API_OVERVIEW_NAME, api.getId().getApiName());
            artifact.setAttribute(APIConstants.API_OVERVIEW_VERSION, api.getId().getVersion());
            artifact.setAttribute(APIConstants.API_OVERVIEW_CONTEXT, api.getContext());
            artifact.setAttribute(APIConstants.API_OVERVIEW_PROVIDER, api.getId().getProviderName());
            artifact.setAttribute(APIConstants.API_OVERVIEW_DESCRIPTION, api.getDescription());
            artifact.setAttribute(APIConstants.API_OVERVIEW_ENDPOINT_URL, api.getUrl());
            artifact.setAttribute(APIConstants.API_OVERVIEW_SANDBOX_URL, api.getSandboxUrl());
            artifact.setAttribute(APIConstants.API_OVERVIEW_WSDL, api.getWsdlUrl());
            artifact.setAttribute(APIConstants.API_OVERVIEW_WADL, api.getWadlUrl());
            artifact.setAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL, api.getThumbnailUrl());
            artifact.setAttribute(APIConstants.API_OVERVIEW_STATUS, apiStatus);
            artifact.setAttribute(APIConstants.API_OVERVIEW_TEC_OWNER, api.getTechnicalOwner());
            artifact.setAttribute(APIConstants.API_OVERVIEW_TEC_OWNER_EMAIL, api.getTechnicalOwnerEmail());
            artifact.setAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER, api.getBusinessOwner());
            artifact.setAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER_EMAIL, api.getBusinessOwnerEmail());
            artifact.setAttribute(APIConstants.API_OVERVIEW_VISIBILITY, api.getVisibility());
            artifact.setAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES, api.getVisibleRoles());
            artifact.setAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS, api.getVisibleTenants());
            artifact.setAttribute(APIConstants.API_OVERVIEW_ENDPOINT_SECURED,Boolean.toString(api.isEndpointSecured()));
            artifact.setAttribute(APIConstants.API_OVERVIEW_ENDPOINT_USERNAME, api.getEndpointUTUsername());
            artifact.setAttribute(APIConstants.API_OVERVIEW_ENDPOINT_PASSWORD, api.getEndpointUTPassword());
            artifact.setAttribute(APIConstants.API_OVERVIEW_TRANSPORTS, api.getTransports());
            artifact.setAttribute(APIConstants.API_OVERVIEW_INSEQUENCE, api.getInSequence());
            artifact.setAttribute(APIConstants.API_OVERVIEW_OUTSEQUENCE, api.getOutSequence());

            artifact.setAttribute(APIConstants.API_OVERVIEW_REDIRECT_URL, api.getRedirectURL());
            artifact.setAttribute(APIConstants.API_OVERVIEW_OWNER, api.getApiOwner());
            artifact.setAttribute(APIConstants.API_OVERVIEW_ADVERTISE_ONLY, Boolean.toString(api.isAdvertiseOnly()));
            
            String tiers = "";
            for (Tier tier : api.getAvailableTiers()) {
                tiers += tier.getName() + "||";
            }
            if (!"".equals(tiers)) {
                tiers = tiers.substring(0, tiers.length() - 2);
                artifact.setAttribute(APIConstants.API_OVERVIEW_TIER, tiers);
            }
            if (APIConstants.PUBLISHED.equals(apiStatus)) {
                artifact.setAttribute(APIConstants.API_OVERVIEW_IS_LATEST, "true");
            }
            String[] keys = artifact.getAttributeKeys();
            for (String key : keys) {
                if (key.contains("URITemplate")) {
                    artifact.removeAttribute(key);
                }
            }

            Set<URITemplate> uriTemplateSet = api.getUriTemplates();
            int i = 0;
            for (URITemplate uriTemplate : uriTemplateSet) {
                artifact.addAttribute(APIConstants.API_URI_PATTERN + i,
                        uriTemplate.getUriTemplate());
                artifact.addAttribute(APIConstants.API_URI_HTTP_METHOD + i,
                        uriTemplate.getHTTPVerb());
                artifact.addAttribute(APIConstants.API_URI_AUTH_TYPE + i,
                        uriTemplate.getAuthType());
                i++;

            }

        } catch (GovernanceException e) {
            String msg = "Failed to create API for : " + api.getId().getApiName();
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return artifact;
    }

    /**
     * Create the Documentation from artifact
     *
     * @param artifact Documentation artifact
     * @return Documentation
     * @throws APIManagementException if failed to create Documentation from artifact
     */
    public static Documentation getDocumentation(GenericArtifact artifact)
            throws APIManagementException {

        Documentation documentation;

        try {
            DocumentationType type;
            String docType = artifact.getAttribute(APIConstants.DOC_TYPE);

            if (docType.equalsIgnoreCase(DocumentationType.HOWTO.getType())) {
                type = DocumentationType.HOWTO;
            } else if (docType.equalsIgnoreCase(DocumentationType.PUBLIC_FORUM.getType())) {
                type = DocumentationType.PUBLIC_FORUM;
            } else if (docType.equalsIgnoreCase(DocumentationType.SUPPORT_FORUM.getType())) {
                type = DocumentationType.SUPPORT_FORUM;
            } else if (docType.equalsIgnoreCase(DocumentationType.API_MESSAGE_FORMAT.getType())) {
                type = DocumentationType.API_MESSAGE_FORMAT;
            } else if (docType.equalsIgnoreCase(DocumentationType.SAMPLES.getType())) {
                type = DocumentationType.SAMPLES;
            } else {
                type = DocumentationType.OTHER;
            }
            documentation = new Documentation(type, artifact.getAttribute(APIConstants.DOC_NAME));
            documentation.setSummary(artifact.getAttribute(APIConstants.DOC_SUMMARY));

            Documentation.DocumentSourceType docSourceType = Documentation.DocumentSourceType.INLINE;
            String artifactAttribute = artifact.getAttribute(APIConstants.DOC_SOURCE_TYPE);

            if (artifactAttribute.equals(Documentation.DocumentSourceType.URL.name())) {
                docSourceType = Documentation.DocumentSourceType.URL;
            } else if (artifactAttribute.equals(Documentation.DocumentSourceType.FILE.name())) {
                docSourceType = Documentation.DocumentSourceType.FILE;
            }

            documentation.setSourceType(docSourceType);
            if (artifact.getAttribute(APIConstants.DOC_SOURCE_TYPE).equals("URL")) {
                documentation.setSourceUrl(artifact.getAttribute(APIConstants.DOC_SOURCE_URL));
            }

            if (docSourceType == Documentation.DocumentSourceType.FILE) {
                documentation.setFilePath(prependWebContextRoot(artifact.getAttribute(APIConstants.DOC_FILE_PATH)));
            }

            if(documentation.getType() == DocumentationType.OTHER){
                documentation.setOtherTypeName(artifact.getAttribute(APIConstants.DOC_OTHER_TYPE_NAME));
            }

        } catch (GovernanceException e) {
            throw new APIManagementException("Failed to get documentation from artifact", e);
        }
        return documentation;
    }

    /**
     * Create the Documentation from artifact
     *
     * @param artifact Documentation artifact
     * @return Documentation
     * @throws APIManagementException if failed to create Documentation from artifact
     */
    public static Documentation getDocumentation(GenericArtifact artifact,String docCreatorName)
            throws APIManagementException {

        Documentation documentation;

        try {
            DocumentationType type;
            String docType = artifact.getAttribute(APIConstants.DOC_TYPE);

            if (docType.equalsIgnoreCase(DocumentationType.HOWTO.getType())) {
                type = DocumentationType.HOWTO;
            } else if (docType.equalsIgnoreCase(DocumentationType.PUBLIC_FORUM.getType())) {
                type = DocumentationType.PUBLIC_FORUM;
            } else if (docType.equalsIgnoreCase(DocumentationType.SUPPORT_FORUM.getType())) {
                type = DocumentationType.SUPPORT_FORUM;
            } else if (docType.equalsIgnoreCase(DocumentationType.API_MESSAGE_FORMAT.getType())) {
                type = DocumentationType.API_MESSAGE_FORMAT;
            } else if (docType.equalsIgnoreCase(DocumentationType.SAMPLES.getType())) {
                type = DocumentationType.SAMPLES;
            } else {
                type = DocumentationType.OTHER;
            }
            documentation = new Documentation(type, artifact.getAttribute(APIConstants.DOC_NAME));
            documentation.setSummary(artifact.getAttribute(APIConstants.DOC_SUMMARY));

            Documentation.DocumentSourceType docSourceType = Documentation.DocumentSourceType.INLINE;
            String artifactAttribute = artifact.getAttribute(APIConstants.DOC_SOURCE_TYPE);

            if (artifactAttribute.equals(Documentation.DocumentSourceType.URL.name())) {
                docSourceType = Documentation.DocumentSourceType.URL;
            } else if (artifactAttribute.equals(Documentation.DocumentSourceType.FILE.name())) {
                docSourceType = Documentation.DocumentSourceType.FILE;
            }

            documentation.setSourceType(docSourceType);
            if (artifact.getAttribute(APIConstants.DOC_SOURCE_TYPE).equals("URL")) {
                documentation.setSourceUrl(artifact.getAttribute(APIConstants.DOC_SOURCE_URL));
            }

            if (docSourceType == Documentation.DocumentSourceType.FILE) {
                String filePath=prependTenantPrefix(artifact.getAttribute(APIConstants.DOC_FILE_PATH),docCreatorName);
                documentation.setFilePath(prependWebContextRoot(filePath));
            }

            if(documentation.getType() == DocumentationType.OTHER){
                documentation.setOtherTypeName(artifact.getAttribute(APIConstants.DOC_OTHER_TYPE_NAME));
            }

        } catch (GovernanceException e) {
            throw new APIManagementException("Failed to get documentation from artifact", e);
        }
        return documentation;
    }

    public static APIStatus getApiStatus(String status) throws APIManagementException {
        APIStatus apiStatus = null;
        for (APIStatus aStatus : APIStatus.values()) {
            if (aStatus.getStatus().equals(status)) {
                apiStatus = aStatus;
            }
        }
        return apiStatus;

    }

    /**
     * Prepends the Tenant Prefix to a registry path. ex: /t/test1.com
     * @param postfixUrl path to be prepended.
     * @return Path prepended with he Tenant domain prefix.
     */
    public static String prependTenantPrefix(String postfixUrl, String username) {
    	String tenantDomain = MultitenantUtils.getTenantDomain(replaceEmailDomainBack(username));
    	if (!(tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
    		String tenantPrefix = "/t/";
            if (tenantDomain != null) {

                postfixUrl = tenantPrefix + tenantDomain + postfixUrl;
            }
        }
        
        return postfixUrl;
    }
    
    /**
     * Prepends the webcontextroot to a registry path.
     * @param postfixUrl path to be prepended.
     * @return Path prepended with he WebContext root.
     */
    public static String prependWebContextRoot(String postfixUrl) {
        String webContext = CarbonUtils.getServerConfiguration().getFirstProperty("WebContextRoot");
        if (webContext != null && !webContext.equals("/")) {

            postfixUrl = webContext + postfixUrl;
        }
        return postfixUrl;
    }

    /**
     * Utility method for creating storage path for an icon.
     *
     * @param identifier APIIdentifier
     * @return Icon storage path.
     */
    public static String getIconPath(APIIdentifier identifier) {
        String artifactPath = APIConstants.API_IMAGE_LOCATION + RegistryConstants.PATH_SEPARATOR +
                identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                identifier.getApiName() + RegistryConstants.PATH_SEPARATOR + identifier.getVersion();
        return artifactPath + RegistryConstants.PATH_SEPARATOR + APIConstants.API_ICON_IMAGE;
    }

    /**
     * Utility method to generate the path for a file.
     *
     * @param identifier APIIdentifier
     * @return Generated path.
     * @fileName File name.
     */
    public static String getDocumentationFilePath(APIIdentifier identifier, String fileName) {
        String contentPath = APIUtil.getAPIDocPath(identifier) + APIConstants.DOCUMENT_FILE_DIR +
                RegistryConstants.PATH_SEPARATOR + fileName;
        return contentPath;
    }
    
    public static String getAPIDefinitionFilePath(String apiName, String apiVersion) {
    	String resourcePath = APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR + 
		apiName +"-"  + apiVersion + RegistryConstants.PATH_SEPARATOR + APIConstants.API_DOC_RESOURCE_NAME;

    	return resourcePath;
    }
    
    /**
     * Utility method to get api path from APIIdentifier
     *
     * @param identifier APIIdentifier
     * @return API path
     */
    public static String getAPIPath(APIIdentifier identifier) {
        return APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                identifier.getApiName() + RegistryConstants.PATH_SEPARATOR +
                identifier.getVersion() + APIConstants.API_RESOURCE_NAME;
    }

    /**
     * Utility method to get API provider path
     *
     * @param identifier APIIdentifier
     * @return API provider path
     */
    public static String getAPIProviderPath(APIIdentifier identifier) {
        return APIConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR
                + identifier.getProviderName();
    }

    /**
     * Utility method to get documentation path
     *
     * @param apiId APIIdentifier
     * @return Doc path
     */
    public static String getAPIDocPath(APIIdentifier apiId) {
        return APIConstants.API_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiId.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiId.getApiName() + RegistryConstants.PATH_SEPARATOR +
                apiId.getVersion() + RegistryConstants.PATH_SEPARATOR +
                APIConstants.DOC_DIR + RegistryConstants.PATH_SEPARATOR;
    }
    
    /**
     * Utility method to get documentation content file path
     *
     * @param apiId APIIdentifier
     * @param documentationName String
     * @return Doc content path
     */
    public static String getAPIDocContentPath(APIIdentifier apiId, String documentationName) {
        return getAPIDocPath(apiId) + APIConstants.INLINE_DOCUMENT_CONTENT_DIR + 
        		RegistryConstants.PATH_SEPARATOR + documentationName;
    }

    /**
     * This utility method used to create documentation artifact content
     *
     * @param artifact      GovernanceArtifact
     * @param apiId         APIIdentifier
     * @param documentation Documentation
     * @return GenericArtifact
     * @throws APIManagementException if failed to get GovernanceArtifact from Documentation
     */
    public static GenericArtifact createDocArtifactContent(GenericArtifact artifact,
                                                           APIIdentifier apiId,
                                                           Documentation documentation)
            throws APIManagementException {
        try {
            artifact.setAttribute(APIConstants.DOC_NAME, documentation.getName());
            artifact.setAttribute(APIConstants.DOC_SUMMARY, documentation.getSummary());
            artifact.setAttribute(APIConstants.DOC_TYPE, documentation.getType().getType());

            Documentation.DocumentSourceType sourceType = documentation.getSourceType();

            switch (sourceType) {
                case INLINE:
                    sourceType = Documentation.DocumentSourceType.INLINE;
                    break;
                case URL:
                    sourceType = Documentation.DocumentSourceType.URL;
                    break;
                case FILE: {
                    sourceType = Documentation.DocumentSourceType.FILE;
                    setFilePermission(documentation.getFilePath());
                }
                break;
            }
            artifact.setAttribute(APIConstants.DOC_SOURCE_TYPE, sourceType.name());
            artifact.setAttribute(APIConstants.DOC_SOURCE_URL, documentation.getSourceUrl());
            artifact.setAttribute(APIConstants.DOC_FILE_PATH, documentation.getFilePath());
            artifact.setAttribute(APIConstants.DOC_OTHER_TYPE_NAME,documentation.getOtherTypeName());
            String basePath = apiId.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                    apiId.getApiName() + RegistryConstants.PATH_SEPARATOR +
                    apiId.getVersion();
            artifact.setAttribute(APIConstants.DOC_API_BASE_PATH, basePath);
        } catch (GovernanceException e) {
            String msg = "Filed to create doc artifact content from :" + documentation.getName();
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return artifact;
    }

    /**
     * this method used to initialized the ArtifactManager
     *
     * @param registry Registry
     * @param key      , key name of the key
     * @return GenericArtifactManager
     * @throws APIManagementException if failed to initialized GenericArtifactManager
     */
    public static GenericArtifactManager getArtifactManager(Registry registry, String key)
            throws APIManagementException {
        GenericArtifactManager artifactManager = null;

        try {
            GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
            if(GovernanceUtils.findGovernanceArtifactConfiguration(key, registry)!=null){
            artifactManager = new GenericArtifactManager(registry, key);
            }
        } catch (RegistryException e) {
            String msg = "Failed to initialize GenericArtifactManager";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return artifactManager;
    }

    /**
     * Crate an WSDL from given wsdl url.
     *
     * @param wsdlUrl  wsdl url
     * @param registry Registry space to save the WSDL
     * @return Path of the created resource
     * @throws APIManagementException If an error occurs while adding the WSDL
     */
    public static String createWSDL(String wsdlUrl, Registry registry) throws RegistryException {
        try {
            WsdlManager wsdlManager = new WsdlManager(registry);
            Wsdl wsdl = wsdlManager.newWsdl(wsdlUrl);
            wsdlManager.addWsdl(wsdl);
            return GovernanceUtils.getArtifactPath(registry, wsdl.getId());
        } catch (RegistryException e) {
            String msg = "Failed to add WSDL " + wsdlUrl + " to the registry";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    /**
     * Create an Endpoint
     *
     * @param endpointUrl Endpoint url
     * @param registry    Registry space to save the endpoint
     * @return Path of the created resource
     * @throws APIManagementException If an error occurs while adding the endpoint
     */
    public static String createEndpoint(String endpointUrl, Registry registry) throws APIManagementException {
        try {
            EndpointManager endpointManager = new EndpointManager(registry);
            Endpoint endpoint = endpointManager.newEndpoint(endpointUrl);
            endpointManager.addEndpoint(endpoint);
            return GovernanceUtils.getArtifactPath(registry, endpoint.getId());
        } catch (RegistryException e) {
            String msg = "Failed to import endpoint " + endpointUrl + " to registry ";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
    }

    /**
     * Returns a map of API availability tiers as defined in the underlying governance
     * registry.
     *
     * @return a Map of tier names and Tier objects - possibly empty
     * @throws APIManagementException if an error occurs when loading tiers from the registry
     */
    public static Map<String, Tier> getTiers() throws APIManagementException {
        Map<String, Tier> tiers = new TreeMap<String, Tier>();
        try {
            Registry registry = ServiceReferenceHolder.getInstance().getRegistryService().
                    getGovernanceSystemRegistry();
            if (registry.resourceExists(APIConstants.API_TIER_LOCATION)) {
                Resource resource = registry.get(APIConstants.API_TIER_LOCATION);
                String content = new String((byte[]) resource.getContent());
                OMElement element = AXIOMUtil.stringToOM(content);
                OMElement assertion = element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT);
                Iterator policies = assertion.getChildrenWithName(APIConstants.POLICY_ELEMENT);
                while (policies.hasNext()) {
                    OMElement policy = (OMElement) policies.next();
                    OMElement id = policy.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT);
                    Tier tier = new Tier(id.getText());
                    tier.setPolicyContent(policy.toString().getBytes());
                    // String desc = resource.getProperty(APIConstants.TIER_DESCRIPTION_PREFIX + id.getText());
                    String desc;
                    try {
                        desc = APIDescriptionGenUtil.generateDescriptionFromPolicy(policy);
                    } catch (APIManagementException ex) {
                        desc = APIConstants.TIER_DESC_NOT_AVAILABLE;
                    }
                    Map<String,Object> tierAttributes=APIDescriptionGenUtil.getTierAttributes(policy);
                    if(tierAttributes!=null && tierAttributes.size()!=0){
                    tier.setTierAttributes(APIDescriptionGenUtil.getTierAttributes(policy));
                    }
                    tier.setDescription(desc);
                    if (!tier.getName().equalsIgnoreCase("Unauthenticated")) {
                        tiers.put(tier.getName(), tier);
                    }
                }
            }

            APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                    getAPIManagerConfigurationService().getAPIManagerConfiguration();
            if (Boolean.parseBoolean(config.getFirstProperty(APIConstants.ENABLE_UNLIMITED_TIER))) {
                Tier tier = new Tier(APIConstants.UNLIMITED_TIER);
                tier.setDescription(APIConstants.UNLIMITED_TIER_DESC);
                tiers.put(tier.getName(), tier);
            }
        } catch (RegistryException e) {
            String msg = "Error while retrieving API tiers from registry";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        } catch (XMLStreamException e) {
            String msg = "Malformed XML found in the API tier policy resource";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return tiers;
    }

    /**
     * Returns a map of API availability tiers of the tenant as defined in the underlying governance
     * registry.
     *
     * @return a Map of tier names and Tier objects - possibly empty
     * @throws APIManagementException if an error occurs when loading tiers from the registry
     */
    public static Map<String, Tier> getTiers(int tenantId) throws APIManagementException {
        Map<String, Tier> tiers = new TreeMap<String, Tier>();
        try {
            Registry registry = ServiceReferenceHolder.getInstance().getRegistryService().
                    getGovernanceSystemRegistry(tenantId);
            if (registry.resourceExists(APIConstants.API_TIER_LOCATION)) {
                Resource resource = registry.get(APIConstants.API_TIER_LOCATION);
                String content = new String((byte[]) resource.getContent());
                OMElement element = AXIOMUtil.stringToOM(content);
                OMElement assertion = element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT);
                Iterator policies = assertion.getChildrenWithName(APIConstants.POLICY_ELEMENT);
                while (policies.hasNext()) {
                    OMElement policy = (OMElement) policies.next();
                    OMElement id = policy.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT);
                    Tier tier = new Tier(id.getText());
                    tier.setPolicyContent(policy.toString().getBytes());
                    // String desc = resource.getProperty(APIConstants.TIER_DESCRIPTION_PREFIX + id.getText());
                    String desc;
                    try {
                        desc = APIDescriptionGenUtil.generateDescriptionFromPolicy(policy);
                    } catch (APIManagementException ex) {
                        desc = APIConstants.TIER_DESC_NOT_AVAILABLE;
                    }
                    Map<String,Object> tierAttributes=APIDescriptionGenUtil.getTierAttributes(policy);
                    if(tierAttributes!=null && tierAttributes.size()!=0){
                    tier.setTierAttributes(APIDescriptionGenUtil.getTierAttributes(policy));
                    }
                    tier.setDescription(desc);
                    if (!tier.getName().equalsIgnoreCase("Unauthenticated")) {
                        tiers.put(tier.getName(), tier);
                    }
                }
            }

            APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                    getAPIManagerConfigurationService().getAPIManagerConfiguration();
            if (Boolean.parseBoolean(config.getFirstProperty(APIConstants.ENABLE_UNLIMITED_TIER))) {
                Tier tier = new Tier(APIConstants.UNLIMITED_TIER);
                tier.setDescription(APIConstants.UNLIMITED_TIER_DESC);
                tiers.put(tier.getName(), tier);
            }
        } catch (RegistryException e) {
            String msg = "Error while retrieving API tiers from registry";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        } catch (XMLStreamException e) {
            String msg = "Malformed XML found in the API tier policy resource";
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return tiers;
    }

    /**
     * Checks whether the specified user has the specified permission.
     *
     * @param username   A username
     * @param permission A valid Carbon permission
     * @throws APIManagementException If the user does not have the specified permission or if an error occurs
     */
    public static void checkPermission(String username, String permission)
            throws APIManagementException {
        if (username == null) {
            throw new APIManagementException("Attempt to execute privileged operation as" +
                                             " the anonymous user");
        }
        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
        boolean authorized;
        try {
            if (!tenantDomain.equals(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomain);
                AuthorizationManager manager = ServiceReferenceHolder.getInstance().
                        getRealmService().getTenantUserRealm(tenantId).
                        getAuthorizationManager();
                authorized = manager.isUserAuthorized(MultitenantUtils.getTenantAwareUsername(username), permission,
                                                      CarbonConstants.UI_PERMISSION_ACTION);
            } else {
                RemoteAuthorizationManager authorizationManager = RemoteAuthorizationManager.getInstance();
                authorized = authorizationManager.isUserAuthorized(username, permission);
            }
            if (!authorized) {
                throw new APIManagementException("User '" + username + "' does not have the " +
                                                 "required permission: " + permission);
            }
        } catch (UserStoreException e) {
            throw new APIManagementException("Error while checking the user:"+username+ " authorized or not",e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
    /**
     * Checks whether the specified user has the specified permission without throwing
     * any exceptions.
     *
     * @param username   A username
     * @param permission A valid Carbon permission
     * @return true if the user has the specified permission and false otherwise
     */
    public static boolean checkPermissionQuietly(String username, String permission) {
        try {
            checkPermission(username, permission);
            return true;
        } catch (APIManagementException e) {
            return false;
        }
    }

    /**
     * Gets the information of the logged in User.
     *
     * @param cookie Cookie of the previously logged in session.
     * @param serviceUrl Url of the authentication service.
     * @return LoggedUserInfo object containing details of the logged in user.
     */
    public static LoggedUserInfo getLoggedInUserInfo(String cookie,String serviceUrl) throws RemoteException, ExceptionException {
        LoggedUserInfoAdminStub stub = new LoggedUserInfoAdminStub(null,
                serviceUrl + "LoggedUserInfoAdmin");
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        LoggedUserInfo userInfo = stub.getUserInfo();
        return userInfo;
    }

    /**
     * Retrieves the role list of a user
     *
     * @param username   A username
     * @throws APIManagementException If an error occurs
     */
    public static String[] getListOfRoles(String username) throws APIManagementException {
        if (username == null) {
            throw new APIManagementException("Attempt to execute privileged operation as" +
                    " the anonymous user");
        }

        RemoteAuthorizationManager authorizationManager = RemoteAuthorizationManager.getInstance();
        return authorizationManager.getRolesOfUser(username);
    }

    /**
     * Retrieves the list of user roles without throwing any exceptions.
     *
     * @param username   A username
     * @return the list of roles to which the user belongs to.
     */
    public static String[] getListOfRolesQuietly(String username) {
        try {
            return getListOfRoles(username);
        } catch (APIManagementException e) {
            return new String[0];
        }
    }

    /**
     * Sets permission for uploaded file resource.
     *
     * @param filePath Registry path for the uploaded file
     * @throws APIManagementException
     */

    private static void setFilePermission(String filePath) throws APIManagementException {
        try {
            filePath = filePath.replaceFirst("/registry/resource/", "");
            AuthorizationManager accessControlAdmin = ServiceReferenceHolder.getInstance().
                    getRealmService().getTenantUserRealm(MultitenantConstants.SUPER_TENANT_ID).
                    getAuthorizationManager();
            if (!accessControlAdmin.isRoleAuthorized(CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME,
                    filePath, ActionConstants.GET)) {
                accessControlAdmin.authorizeRole(CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME,
                        filePath, ActionConstants.GET);
            }
        } catch (UserStoreException e) {
            throw new APIManagementException("Error while setting up permissions for file location", e);
        }
    }

      /**
        * This method used to get API from governance artifact specific to copyAPI
        *
        * @param artifact API artifact
        * @param registry Registry
        * @return API
        * @throws APIManagementException if failed to get API from artifact
        */
       public static API getAPI(GovernanceArtifact artifact, Registry registry,APIIdentifier oldId)
               throws APIManagementException {

           API api;
           try {
               String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
               String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
               String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
               api = new API(new APIIdentifier(providerName, apiName, apiVersion));
               // set rating
               String artifactPath = GovernanceUtils.getArtifactPath(registry, artifact.getId());
               BigDecimal bigDecimal = new BigDecimal(registry.getAverageRating(artifactPath));
               BigDecimal res = bigDecimal.setScale(1, RoundingMode.HALF_UP);
               api.setRating(res.floatValue());
               //set description
               api.setDescription(artifact.getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION));
               //set last access time
               api.setLastUpdated(registry.get(artifactPath).getLastModified());
               // set url
               api.setUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_URL));
               api.setSandboxUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_SANDBOX_URL));
               api.setStatus(getApiStatus(artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS)));
               api.setThumbnailUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL));
               api.setWsdlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WSDL));
               api.setWadlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WADL));
               api.setTechnicalOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER));
               api.setTechnicalOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER_EMAIL));
               api.setBusinessOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER));
               api.setBusinessOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER_EMAIL));
               api.setEndpointSecured(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_SECURED)));
               api.setEndpointUTUsername(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_USERNAME));
               api.setEndpointUTPassword(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_PASSWORD));
               api.setTransports(artifact.getAttribute(APIConstants.API_OVERVIEW_TRANSPORTS));

               api.setRedirectURL(artifact.getAttribute(APIConstants.API_OVERVIEW_REDIRECT_URL));
               api.setApiOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_OWNER));
               api.setAdvertiseOnly(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ADVERTISE_ONLY)));

               String tenantDomainName = MultitenantUtils.getTenantDomain(replaceEmailDomainBack(providerName));
               int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                                .getTenantId(tenantDomainName);
               
               Set<Tier> availableTier = new HashSet<Tier>();
               String tiers = artifact.getAttribute(APIConstants.API_OVERVIEW_TIER);
               Map<String, Tier> definedTiers = getTiers(tenantId);
               if (tiers != null && !"".equals(tiers)) {
                   String[] tierNames = tiers.split("\\|\\|");
                   for (String tierName : tierNames) {
                       Tier definedTier = definedTiers.get(tierName);
                       if (definedTier != null) {
                           availableTier.add(definedTier);
                       } else {
                           log.warn("Unknown tier: " + tierName + " found on API: " + apiName);
                       }
                   }
               }
               api.addAvailableTiers(availableTier);
               api.setContext(artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT));
               api.setLatest(Boolean.valueOf(artifact.getAttribute(APIConstants.API_OVERVIEW_IS_LATEST)));
               ArrayList<URITemplate> urlPatternsList;

               urlPatternsList = ApiMgtDAO.getAllURITemplates(api.getContext(), oldId.getVersion());
               Set<URITemplate> uriTemplates = new HashSet<URITemplate>(urlPatternsList);

               for (URITemplate uriTemplate : uriTemplates) {
                   uriTemplate.setResourceURI(api.getUrl());
                   uriTemplate.setResourceSandboxURI(api.getSandboxUrl());

               }
               api.setUriTemplates(uriTemplates);

               Set<String> tags = new HashSet<String>();
               org.wso2.carbon.registry.core.Tag[] tag = registry.getTags(artifactPath);
               for (Tag tag1 : tag) {
                   tags.add(tag1.getTagName());
               }
               api.addTags(tags);
               api.setLastUpdated(registry.get(artifactPath).getLastModified());

           } catch (GovernanceException e) {
               String msg = "Failed to get API fro artifact ";
               throw new APIManagementException(msg, e);
           } catch (RegistryException e) {
               String msg = "Failed to get LastAccess time or Rating";
               throw new APIManagementException(msg, e);
           } catch (UserStoreException e){
               String msg = "Failed to get User Realm of API Provider";
               throw new APIManagementException(msg, e);
           }
           return api;
       }
    
    public static boolean checkAccessTokenPartitioningEnabled() {
        return OAuthServerConfiguration.getInstance().isAccessTokenPartitioningEnabled();
    }
    
    public static boolean checkUserNameAssertionEnabled() {
        return OAuthServerConfiguration.getInstance().isUserNameAssertionEnabled();
    }

    public static String[] getAvailableKeyStoreTables() throws APIManagementException {
        String[] keyStoreTables = new String[0];
        Map<String, String>  domainMappings = getAvailableUserStoreDomainMappings();
        if (domainMappings != null) {
            keyStoreTables = new String[domainMappings.size()];
            int i = 0;
            for (Map.Entry<String, String> e : domainMappings.entrySet()) {
                String value = e.getValue();
                keyStoreTables[i] = APIConstants.ACCESS_TOKEN_STORE_TABLE + "_" + value.trim();
                i++;
            }
        }
        return keyStoreTables;
    }

    public static Map<String, String> getAvailableUserStoreDomainMappings() throws
            APIManagementException {
        Map<String, String> userStoreDomainMap = new HashMap<String, String>();
        String domainsStr = OAuthServerConfiguration.getInstance().getAccessTokenPartitioningDomains();
        if (domainsStr != null) {
            String[] userStoreDomainsArr = domainsStr.split(",");
            for (String anUserStoreDomainsArr : userStoreDomainsArr) {
                String[] mapping = anUserStoreDomainsArr.trim().split(":"); //A:foo.com , B:bar.com
                if (mapping.length < 2) {
                    throw new APIManagementException("Domain mapping has not defined");
                }
                userStoreDomainMap.put(mapping[1].trim(), mapping[0].trim()); //key=domain & value=mapping
            }
        }
        return userStoreDomainMap;
    }
    
    public static String getAccessTokenStoreTableFromUserId(String userId) 
            throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        String userStore;
         if(userId != null) {
            String[] strArr = userId.split("/");
            if (strArr != null && strArr.length > 1) {
                userStore = strArr[0];
                Map<String, String> availableDomainMappings = getAvailableUserStoreDomainMappings();
                if (availableDomainMappings != null &&
                        availableDomainMappings.containsKey(userStore)) {
                    accessTokenStoreTable = accessTokenStoreTable + "_" +
                            availableDomainMappings.get(userStore);
                }
            }
         }
        return accessTokenStoreTable;
    }

    public static String getAccessTokenStoreTableFromAccessToken(String apiKey)
            throws APIManagementException {
        String userId = getUserIdFromAccessToken(apiKey); //i.e: 'foo.com/admin' or 'admin'
        return getAccessTokenStoreTableFromUserId(userId);
    }

    public static String getUserIdFromAccessToken(String apiKey) {
        String userId = null;
        String decodedKey = new String(Base64.decodeBase64(apiKey.getBytes()));
        String[] tmpArr = decodedKey.split(":");
        if (tmpArr != null && tmpArr.length == 2) { //tmpArr[0]= userStoreDomain & tmpArr[1] = userId
            userId = tmpArr[1];
        }
        return userId;
    }

    /**
     *  When an input is having '@',replace it with '-AT-' [This is required to persist API data in registry,as registry paths don't allow '@' sign.]
     * @param input inputString
     * @return String modifiedString
     */
    public static String replaceEmailDomain(String input){
        if(input!=null&& input.contains(APIConstants.EMAIL_DOMAIN_SEPARATOR) ){
            input=input.replace(APIConstants.EMAIL_DOMAIN_SEPARATOR,APIConstants.EMAIL_DOMAIN_SEPARATOR_REPLACEMENT);
        }
        return input;
    }

    /**
     * When an input is having '-AT-',replace it with @ [This is required to persist API data between registry and database]
     * @param input inputString
     * @return String modifiedString
     */
    public static String replaceEmailDomainBack(String input) {
        if (input!=null && input.contains(APIConstants.EMAIL_DOMAIN_SEPARATOR_REPLACEMENT)) {
            input = input.replace(APIConstants.EMAIL_DOMAIN_SEPARATOR_REPLACEMENT,
                                  APIConstants.EMAIL_DOMAIN_SEPARATOR);
        }
        return input;
    }

    public static void copyResourcePermissions(String username, String sourceArtifactPath, String targetArtifactPath)
            throws APIManagementException {
        String sourceResourcePath = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
                        + sourceArtifactPath);

        String targetResourcePath = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
                        + targetArtifactPath);

        String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(username));

        try {
            int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomain);
            AuthorizationManager authManager = ServiceReferenceHolder.getInstance().getRealmService().
                    getTenantUserRealm(tenantId).getAuthorizationManager();
            String[] allowedRoles = authManager.getAllowedRolesForResource(sourceResourcePath, ActionConstants.GET);

            if (allowedRoles != null) {

                for (String allowedRole : allowedRoles) {
                    authManager.authorizeRole(allowedRole, targetResourcePath, ActionConstants.GET);
                }
            }

        } catch (UserStoreException e) {
            throw new APIManagementException("Error while adding role permissions to API", e);
        }
    }


    /**
     * This function is to set resource permissions based on its visibility
     *
     * @param visibility   API visibility
     * @param roles        Authorized roles
     * @param artifactPath API resource path
     * @throws APIManagementException Throwing exception
     */
    public static void setResourcePermissions(String username, String visibility, String[] roles, String artifactPath)
            throws APIManagementException {
        try {
        	String resourcePath = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
                    + artifactPath);
        	
        	String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(username));
        	if (!tenantDomain.equals(org.wso2.carbon.utils.multitenancy.
        			MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
        		int tenantId = ServiceReferenceHolder.getInstance().getRealmService().
        				getTenantManager().getTenantId(tenantDomain);
        		AuthorizationManager authManager = ServiceReferenceHolder.getInstance().getRealmService().
        				getTenantUserRealm(tenantId).getAuthorizationManager();
        		if (visibility != null && visibility.equalsIgnoreCase(APIConstants.API_RESTRICTED_VISIBILITY)) {
        			boolean isRoleEveryOne = false;
                    /*If no roles have defined, authorize for everyone role */
        			if (roles != null && roles.length == 1 && roles[0].equals("")) {
                    	authManager.authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath,
                                ActionConstants.GET);
                    	isRoleEveryOne = true;
                    } else {
                    	for (String role : roles) {
                            if (role.equalsIgnoreCase(APIConstants.EVERYONE_ROLE)) {
                                isRoleEveryOne = true;
                            }
                            authManager.authorizeRole(role, resourcePath, ActionConstants.GET);

                        }
                    }
                    if (!isRoleEveryOne) {
                    	authManager.denyRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
                    }
                    authManager.denyRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);
        		} else {
                	authManager.authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath,
                                                       ActionConstants.GET);
                	authManager.authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath,
                                                       ActionConstants.GET);
                }
        	} else {
        		RegistryAuthorizationManager authorizationManager = new RegistryAuthorizationManager
                        (ServiceReferenceHolder.getUserRealm());

                if (visibility != null && visibility.equalsIgnoreCase(APIConstants.API_RESTRICTED_VISIBILITY)) {
                    boolean isRoleEveryOne = false;
                    for (String role : roles) {
                        if (role.equalsIgnoreCase(APIConstants.EVERYONE_ROLE)) {
                            isRoleEveryOne = true;
                        }
                        authorizationManager.authorizeRole(role, resourcePath, ActionConstants.GET);

                    }
                    if (!isRoleEveryOne) {
                        authorizationManager.denyRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
                    }
                    authorizationManager.denyRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);

                } else {
                    authorizationManager.authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath,
                                                       ActionConstants.GET);
                    authorizationManager.authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath,
                                                       ActionConstants.GET);
                }
        	}
            

        } catch (UserStoreException e) {
        	throw new APIManagementException("Error while adding role permissions to API", e);
        } 
    }

	/**
	 * Load the throttling policy  to the registry for tenants
	 * 
	 * @param tenant
	 * @param tenantID
	 * @throws APIManagementException
	 */

	public static void loadTenantAPIPolicy(String tenant, int tenantID)
	                                                                   throws APIManagementException {
		try {
			RegistryService registryService =
			                                  ServiceReferenceHolder.getInstance()
			                                                        .getRegistryService();
			//UserRegistry govRegistry = registryService.getGovernanceUserRegistry(tenant, tenantID);
            UserRegistry govRegistry = registryService.getGovernanceSystemRegistry(tenantID);

			if (govRegistry.resourceExists(APIConstants.API_TIER_LOCATION)) {
				if (log.isDebugEnabled()) {
					log.debug("Tier policies already uploaded to the tenant's registry space");
				}
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("Adding API tier policies to the tenant's registry");
			}
			InputStream inputStream =
			                          APIManagerComponent.class.getResourceAsStream("/tiers/default-tiers.xml");
			byte[] data = IOUtils.toByteArray(inputStream);
			Resource resource = govRegistry.newResource();
			resource.setContent(data);
			govRegistry.put(APIConstants.API_TIER_LOCATION, resource);

		} catch (RegistryException e) {
			throw new APIManagementException(
			                                 "Error while saving policy information to the registry",
			                                 e);
		} catch (IOException e) {
			throw new APIManagementException("Error while reading policy file content", e);
		}
    }

    public static void writeDefinedSequencesToTenantRegistry(int tenantID)
            throws APIManagementException {
        try {
            RegistryService registryService =
                    ServiceReferenceHolder.getInstance()
                            .getRegistryService();
            UserRegistry govRegistry = registryService.getGovernanceSystemRegistry(tenantID);

            if (govRegistry.resourceExists(APIConstants.API_CUSTOM_INSEQUENCE_LOCATION)) {
                if(log.isDebugEnabled()){
                    log.debug("Defined sequences have already been added to the tenant's registry");
                }
                return;
            }

            if(log.isDebugEnabled()){
                log.debug("Adding defined sequences to the tenant's registry.");
            }

            InputStream inSeqStream =
                    APIManagerComponent.class.getResourceAsStream("/definedsequences/in/log_in_message.xml");
            byte[] inSeqData = IOUtils.toByteArray(inSeqStream);
            Resource inSeqResource = govRegistry.newResource();
            inSeqResource.setContent(inSeqData);

            govRegistry.put(APIConstants.API_CUSTOM_INSEQUENCE_LOCATION + "log_in_message.xml", inSeqResource);

            InputStream outSeqStream =
                    APIManagerComponent.class.getResourceAsStream("/definedsequences/out/log_out_message.xml");
            byte[] outSeqData = IOUtils.toByteArray(outSeqStream);
            Resource outSeqResource = govRegistry.newResource();
            outSeqResource.setContent(outSeqData);

            govRegistry.put(APIConstants.API_CUSTOM_OUTSEQUENCE_LOCATION + "log_out_message.xml", outSeqResource);

        } catch (RegistryException e) {
            throw new APIManagementException("Error while saving defined sequences to the tenant's registry ", e);
        } catch (IOException e) {
            throw new APIManagementException("Error while reading defined sequence ", e);
        }
	}

	/**
	 * Load the  API RXT to the registry for tenants
	 * 
	 * @param tenant
	 * @param tenantID
	 * @throws APIManagementException
	 */
	
	public static void loadloadTenantAPIRXT(String tenant, int tenantID)
	                                                                    throws APIManagementException {
		RegistryService registryService = ServiceReferenceHolder.getInstance().getRegistryService();
		UserRegistry registry = null;
		try {
			//registry = registryService.getRegistry(tenant, tenantID);
            registry = registryService.getGovernanceSystemRegistry(tenantID);
		} catch (RegistryException e) {
			throw new APIManagementException("Error when create registry instance ", e);
		}
			
		String rxtDir =
		                CarbonUtils.getCarbonHome() + File.separator + "repository" +
		                        File.separator + "resources" + File.separator + "rxts";
		File file = new File(rxtDir);
		FilenameFilter filenameFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// if the file extension is .rxt return true, else false
				return name.endsWith(".rxt");
			}
		};
		String[] rxtFilePaths = file.list(filenameFilter);
		for (String rxtPath : rxtFilePaths) {
			String resourcePath =
			                      GovernanceConstants.RXT_CONFIGS_PATH +
			                              RegistryConstants.PATH_SEPARATOR + rxtPath;

            //This is  "registry" is a governance registry instance, therefore calculate the relative path to governance.
            String govRelativePath =   RegistryUtils.getRelativePathToOriginal(resourcePath,
                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH);
			try {
				if (registry.resourceExists(govRelativePath)) {
					continue;
				}
				String rxt = FileUtil.readFileToString(rxtDir + File.separator + rxtPath);
				Resource resource = registry.newResource();
				resource.setContent(rxt.getBytes());
				resource.setMediaType(APIConstants.RXT_MEDIA_TYPE);
				registry.put(govRelativePath, resource);
			} catch (IOException e) {
				String msg = "Failed to read rxt files";
				throw new APIManagementException(msg, e);
			} catch (RegistryException e) {
				String msg = "Failed to add rxt to registry ";
				throw new APIManagementException(msg, e);
			}
		}

	}

    /**
     * Converting the user store domain name to uppercase.
     *
     * @param username Username to be modified
     * @return Username with domain name set to uppercase.
     */
    public static String setDomainNameToUppercase(String username) {
        if (username != null) {
            String[] nameParts = username.split(CarbonConstants.DOMAIN_SEPARATOR);
            if (nameParts.length > 1) {
                username = nameParts[0].toUpperCase() + CarbonConstants.DOMAIN_SEPARATOR + nameParts[1];
            }
        }

        return username;
    }

    public void setupSelfRegistration(APIManagerConfiguration config,int tenantId)
            throws APIManagementException {
        boolean enabled = Boolean.parseBoolean(config.getFirstProperty(APIConstants.SELF_SIGN_UP_ENABLED));
        if (!enabled) {
            return;
        }

        String role = config.getFirstProperty(APIConstants.SELF_SIGN_UP_ROLE);
        if (role == null) {
            // Required parameter missing - Throw an exception and interrupt startup
            throw new APIManagementException("Required subscriber role parameter missing " +
                                             "in the self sign up configuration");
        }

        boolean create = Boolean.parseBoolean(config.getFirstProperty(APIConstants.SELF_SIGN_UP_CREATE_ROLE));
        if (create) {
            String[] permissions = new String[]{
                    "/permission/admin/login",
                    APIConstants.Permissions.API_SUBSCRIBE
            };
            try {
                RealmService realmService=ServiceReferenceHolder.getInstance().getRealmService();
                UserRealm realm;
                org.wso2.carbon.user.api.UserRealm tenantRealm;
                UserStoreManager manager;

                if(tenantId<0){
                realm = realmService.getBootstrapRealm();
                manager = realm.getUserStoreManager();
                }else{
                tenantRealm=realmService.getTenantUserRealm(tenantId);
                manager=tenantRealm.getUserStoreManager();
                }
                if (!manager.isExistingRole(role)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating subscriber role: " + role);
                    }
                    Permission[] subscriberPermissions = new Permission[]{new Permission("/permission/admin/login", UserMgtConstants.EXECUTE_ACTION),
                                                                          new Permission(APIConstants.Permissions.API_SUBSCRIBE, UserMgtConstants.EXECUTE_ACTION)};
                    String tenantAdminName = ServiceReferenceHolder.getInstance()
                            .getRealmService().getTenantUserRealm(tenantId).
                                    getRealmConfiguration().getAdminUserName();
                    String[] userList = new String[] { tenantAdminName };
                    manager.addRole(role, userList , subscriberPermissions);
                }
            } catch (UserStoreException e) {
                throw new APIManagementException("Error while creating subscriber role: " + role + " - " +
                                                 "Self registration might not function properly.", e);
            }
        }
    }
    
    public static String removeAnySymbolFromUriTempate(String uriTemplate) {
    	if (uriTemplate != null) {
    		int anySymbolIndex = uriTemplate.indexOf("/*");
    		if (anySymbolIndex != -1) {
    			return uriTemplate.substring(0, anySymbolIndex);
    		}
    	}
    	return uriTemplate;
    }

    public static float getAverageRating(APIIdentifier apiId) throws APIManagementException {
        return ApiMgtDAO.getAverageRating(apiId);
    }

    public static List<Tenant> getAllTenantsWithSuperTenant() throws UserStoreException {
        Tenant[] tenants = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getAllTenants();
        ArrayList<Tenant> tenantArrayList=new ArrayList<Tenant>();
        for(Tenant t:tenants){
            tenantArrayList.add(t);
        }
        Tenant superAdminTenant=new Tenant();
        superAdminTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        superAdminTenant.setId(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID);
        superAdminTenant.setAdminName(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
        tenantArrayList.add(superAdminTenant);
        return tenantArrayList;
    }

    /**
     * In multi tenant environment, publishers should allow only to revoke the tokens generated within his domain.
     * Super tenant should not see the tenant created tokens and vise versa. This method is used to check the logged in
     * user have permissions to revoke a given users tokens.
     * @param loggedInUser current logged in user to publisher
     * @param authorizedUser access token owner
     * @return
     */
    public static boolean isLoggedInUserAuthorizedToRevokeToken(String loggedInUser, String authorizedUser) {
        String loggedUserTenantDomain = MultitenantUtils.getTenantDomain(loggedInUser);
        String authorizedUserTenantDomain = MultitenantUtils.getTenantDomain(authorizedUser);

        if (loggedUserTenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME) &&
                authorizedUserTenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            return true;
        } else if (loggedUserTenantDomain.equals(authorizedUserTenantDomain)) {
            return true;
        }

        return false;
    }
    public static int getApplicationId(String appName,String userId) throws APIManagementException {
        return new ApiMgtDAO().getApplicationId(appName,userId);
    }

    public static boolean isAPIManagementEnabled() {
        return Boolean.parseBoolean(CarbonUtils.getServerConfiguration().getFirstProperty("APIManagement.Enabled"));
    }
    
    public static boolean isLoadAPIContextsAtStartup() {
        return Boolean.parseBoolean(CarbonUtils.getServerConfiguration().getFirstProperty("APIManagement.LoadAPIContextsInServerStartup"));
    }

    public static Set<APIStore> getExternalAPIStores() throws APIManagementException {
        SortedSet<APIStore> apistoreSet=new TreeSet<APIStore>(new APIStoreNameComparator());
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        apistoreSet.addAll(config.getExternalAPIStores());
        if (apistoreSet.size() != 0) {
            return apistoreSet;
        } else {
            return null;
        }

    }

    public static Set<APIStore> getExternalAPIStores(Set<APIStore> inputStores)
            throws APIManagementException {
        SortedSet<APIStore> apiStores = new TreeSet<APIStore>(new APIStoreNameComparator());
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        apiStores.addAll(config.getExternalAPIStores());
        boolean exists = false;
        if (apiStores.size() != 0) {
            for (APIStore store : apiStores) {
                for (APIStore inputStore : inputStores) {
                    if (inputStore.getName().equals(store.getName())) { //If the configured apistore already stored in db,ignore adding it again
                        exists = true;
                    }
                }
                if (!exists) {
                    inputStores.add(store);
                }
                exists=false;
            }

        }
        return inputStores;


    }

    public static boolean isAPIsPublishToExternalAPIStores()
            throws APIManagementException {

        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        return config.getExternalAPIStores().size() != 0;


    }

    public static boolean isAPIGatewayKeyCacheEnabled() {
        try {
            APIManagerConfiguration config = ServiceReferenceHolder.getInstance().getAPIManagerConfigurationService().getAPIManagerConfiguration();
            String serviceURL = config.getFirstProperty(APIConstants.API_GATEWAY_KEY_CACHE_ENABLED);
            return Boolean.parseBoolean(serviceURL);
        } catch (Exception e) {
            log.error("Did not found valid API Validation Information cache configuration. Use default configuration" + e);
        }
        return true;
    }


    public static Cache getAPIContextCache() {
        CacheManager contextCacheManager = Caching.getCacheManager(APIConstants.API_CONTEXT_CACHE_MANAGER).
                getCache(APIConstants.API_CONTEXT_CACHE).getCacheManager();
        if (!isContextCacheInitialized) {
            isContextCacheInitialized = true;
            return contextCacheManager.<String, Boolean>createCacheBuilder(APIConstants.API_CONTEXT_CACHE_MANAGER).
                    setExpiry(CacheConfiguration.ExpiryType.MODIFIED, new CacheConfiguration.Duration(TimeUnit.DAYS,
                            APIConstants.API_CONTEXT_CACHE_EXPIRY_TIME_IN_DAYS)).setStoreByValue(false).build();
        } else {
            return Caching.getCacheManager(APIConstants.API_CONTEXT_CACHE_MANAGER).getCache(APIConstants.API_CONTEXT_CACHE);
        }
    }

    /**
     * Get active tenant domains
     *
     * @return
     * @throws UserStoreException
     */
    public static Set<String> getActiveTenantDomains() throws UserStoreException {
        Set<String> tenantDomains = null;
        Tenant[] tenants = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getAllTenants();
        if (tenants == null || tenants.length == 0) {
            return tenantDomains;
        } else {
            tenantDomains = new HashSet<String>();
            for (Tenant tenant : tenants) {
                if (tenant.isActive()) {
                    tenantDomains.add(tenant.getDomain());
                }
            }
            if (tenantDomains.size() > 0) {
                tenantDomains.add(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            }
            return tenantDomains;
        }

    }


    /**
     * Retrieves the role list of system

     * @throws APIManagementException If an error occurs
     */
    public static String[] getRoleNames(String username) throws APIManagementException {

        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        try {
            if (!tenantDomain.equals(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(tenantDomain);
                UserStoreManager manager = ServiceReferenceHolder.getInstance().
                        getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();

                return manager.getRoleNames();
            } else {
                RemoteAuthorizationManager authorizationManager = RemoteAuthorizationManager.getInstance();
                return authorizationManager.getRoleNames();
            }
        } catch (UserStoreException e) {
            log.error("Error while getting all the roles", e);
            return null;

        }

    }
    
    /**
     * Create API Definition in JSON
     *
     * @param api API
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to generate the content and save
     */
    public static String createSwaggerJSONContent(API api) throws APIManagementException {
    	APIIdentifier identifier = api.getId();    	

		APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        Environment environment = config.getApiGatewayEnvironments().get(0);
        String endpoints = environment.getApiGatewayEndpoint();
        String[] endpointsSet = endpoints.split(",");
        String apiContext = api.getContext();
        String version = identifier.getVersion();
        Set<URITemplate> uriTemplates = api.getUriTemplates();
        String description = api.getDescription();
        String urlPrefix = apiContext + "/" +version;
                        
        if (endpointsSet.length < 1) {
        	throw new APIManagementException("Error in creating JSON representation of the API" + identifier.getApiName());
        }
    	if (description == null || description.equals("")) {
    		description = "";
    	}
    	
    	Map<String, List<Operation>> uriTemplateDefinitions = new HashMap<String, List<Operation>>();
    	List<APIResource> apis = new ArrayList<APIResource>();
    	for (URITemplate template : uriTemplates) {
    		List<Operation> ops;
    		List<Parameter> parameters = null;
    		String path = urlPrefix + 
    				APIUtil.removeAnySymbolFromUriTempate(template.getUriTemplate());
    		/* path exists in uriTemplateDefinitions */
    		if (uriTemplateDefinitions.get(path) != null) {
    			ops = uriTemplateDefinitions.get(path);
    			parameters = new ArrayList<Parameter>();
    			if (!(template.getAuthType().equals(APIConstants.AUTH_NO_AUTHENTICATION))) {
    				Parameter authParam = new Parameter(APIConstants.OperationParameter.AUTH_PARAM_NAME, 
    						APIConstants.OperationParameter.AUTH_PARAM_DESCRIPTION, APIConstants.OperationParameter.AUTH_PARAM_TYPE, true, false, "String");
    				parameters.add(authParam);
    			}
    			String httpVerb = template.getHTTPVerb();
    			/* For GET and DELETE Parameter name - Query Parameters*/
    			if (httpVerb.equals(Constants.Configuration.HTTP_METHOD_GET) ||
    					httpVerb.equals(Constants.Configuration.HTTP_METHOD_DELETE)) {
    				Parameter queryParam = new Parameter(APIConstants.OperationParameter.QUERY_PARAM_NAME, 
    						APIConstants.OperationParameter.QUERY_PARAM_DESCRIPTION, APIConstants.OperationParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(queryParam);
    			} else {/* For POST and PUT Parameter name - Payload*/
    				Parameter payLoadParam = new Parameter(APIConstants.OperationParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.OperationParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.OperationParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(payLoadParam);
    			}
    			Operation op = new Operation(httpVerb, description, description, parameters);
    			ops.add(op);
    		} else {/* path not exists in uriTemplateDefinitions */
    			ops = new ArrayList<Operation>();
    			parameters = new ArrayList<Parameter>();
				if (!(template.getAuthType().equals(APIConstants.AUTH_NO_AUTHENTICATION))) {
    				Parameter authParam = new Parameter(APIConstants.OperationParameter.AUTH_PARAM_NAME, 
    						APIConstants.OperationParameter.AUTH_PARAM_DESCRIPTION, APIConstants.OperationParameter.AUTH_PARAM_TYPE, true, false, "String");
    				parameters.add(authParam);
    			}
				String httpVerb = template.getHTTPVerb();
				/* For GET and DELETE Parameter name - Query Parameters*/
    			if (httpVerb.equals(Constants.Configuration.HTTP_METHOD_GET) ||
    					httpVerb.equals(Constants.Configuration.HTTP_METHOD_DELETE)) {
    				Parameter queryParam = new Parameter(APIConstants.OperationParameter.QUERY_PARAM_NAME, 
    						APIConstants.OperationParameter.QUERY_PARAM_DESCRIPTION, APIConstants.OperationParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(queryParam);
    			} else {/* For POST and PUT Parameter name - Payload*/
    				Parameter payLoadParam = new Parameter(APIConstants.OperationParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.OperationParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.OperationParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(payLoadParam);
    			}
    			Operation op = new Operation(httpVerb, description, description, parameters);
    			ops.add(op);
    			uriTemplateDefinitions.put(path, ops);
    		}
    	}
    	
    	Set<String> resPaths = uriTemplateDefinitions.keySet();
		
		for (String resPath: resPaths) {
			APIResource apiResource = new APIResource(resPath, description, uriTemplateDefinitions.get(resPath));
			apis.add(apiResource);
    	}
			
		APIDefinition apidefinition = new APIDefinition(version, APIConstants.SWAGGER_VERSION, endpointsSet[0], apiContext, apis);
    	    		    		
    	Gson gson = new Gson();
    	return gson.toJson(apidefinition); 
     }
    
    /**
     * Build OMElement from inputstream
     * @param inputStream 
     * @return  OMElement
     * @throws Exception
     * @return 
     */
    public static OMElement buildOMElement(InputStream inputStream) throws Exception {
        XMLStreamReader parser;
        StAXOMBuilder builder;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
             builder = new StAXOMBuilder(parser);            
        }
        catch (XMLStreamException e) {
            String msg = "Error in initializing the parser.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }        
   
        return builder.getDocumentElement();
    }
    
  
	/**
	 * Get stored custom sequences from governanceSystem registry
	 * 
	 * @param sequenceName
	 *            -The sequence to be retrieved
	 * @param tenantId
	 * @param direction
	 *            - Direction indicates in Sequence/outSequence. Values would be
	 *            "in" or "out"
	 * @return
	 * @throws APIManagementException
	 */
	public static OMElement getCustomSequence(String sequenceName, int tenantId,
	                                                 String direction)
	                                                                  throws APIManagementException {
		org.wso2.carbon.registry.api.Collection seqCollection = null;

		try {
			UserRegistry registry = ServiceReferenceHolder.getInstance().getRegistryService()
			                                              .getGovernanceSystemRegistry(tenantId);
			if ("in".equals(direction)) {
				seqCollection = (org.wso2.carbon.registry.api.Collection) registry.get(APIConstants.API_CUSTOM_INSEQUENCE_LOCATION);
			}
			if ("out".equals(direction)) {
				seqCollection = (org.wso2.carbon.registry.api.Collection) registry.get(APIConstants.API_CUSTOM_OUTSEQUENCE_LOCATION);
			}
			if (seqCollection != null) {
				String[] childPaths = seqCollection.getChildren();

				for (int i = 0; i < childPaths.length; i++) {
					Resource sequence = registry.get(childPaths[i]);
					OMElement seqElment = APIUtil.buildOMElement(sequence.getContentStream());
					if (sequenceName.equals(seqElment.getAttributeValue(new QName("name")))) {
						return seqElment;
					}
				}
			}
			
		} catch (Exception e) {
			String msg = "Issue is in accessing the Registry";
			log.error(msg);
			throw new APIManagementException(msg, e);
		}
		return null;
	}
	
	/**
	 * Return the sequence extension name.
	 * eg: admin--testAPi--v1.00
	 * 
	 * @param api
	 * @return 
	 */
	public static String getSequenceExtensionName(API api) {

		String seqExt = api.getId().getProviderName() + "--" + api.getId().getApiName() + ":v" +
		                        api.getId().getVersion();

		return seqExt;

	}

    /**
     *
     * @param token
     * @return
     */
    public static String decryptToken(String token) throws CryptoException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        if(Boolean.parseBoolean(config.getFirstProperty(APIConstants.API_KEY_MANAGER_ENCRYPT_TOKENS))){
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(token));
        }
        return token;
    }

    /**
     *
     * @param token
     * @return
     */
    public static String encryptToken(String token) throws CryptoException{
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        if(Boolean.parseBoolean(config.getFirstProperty(APIConstants.API_KEY_MANAGER_ENCRYPT_TOKENS))){
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(token.getBytes());
        }
        return token;
    }

    public static void loadTenantRegistry(int tenantId){
        TenantRegistryLoader tenantRegistryLoader = APIManagerComponent.getTenantRegistryLoader();
        ServiceReferenceHolder.getInstance().getIndexLoaderService().loadTenantIndex(tenantId);
        tenantRegistryLoader.loadTenantRegistry(tenantId);
        }
}

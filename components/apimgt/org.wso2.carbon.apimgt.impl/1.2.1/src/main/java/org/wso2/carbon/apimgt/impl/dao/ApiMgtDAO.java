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

package org.wso2.carbon.apimgt.impl.dao;


import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.dto.UserApplicationAPIUsage;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.*;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.token.JWTGenerator;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowStatus;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.apimgt.impl.utils.APIVersionComparator;
import org.wso2.carbon.apimgt.impl.utils.LRUCache;
import org.wso2.carbon.apimgt.impl.utils.RemoteUserManagerClient;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.OAuthUtil;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.carbon.apimgt.api.model.Comment;

import javax.cache.Cache;
import javax.cache.Caching;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represent the ApiMgtDAO.
 */
public class ApiMgtDAO {

    private static final Log log = LogFactory.getLog(ApiMgtDAO.class);

    public static JWTGenerator jwtGenerator;
    public static Boolean removeUserNameInJWTForAppToken;

    private static final String ENABLE_JWT_GENERATION = "APIConsumerAuthentication.EnableTokenGeneration";
    private static final String ENABLE_JWT_CACHE = "APIKeyManager.EnableJWTCache";

    // Primary/Secondary Login conifguration
    private static final String USERID_LOGIN = "UserIdLogin";
    private static final String EMAIL_LOGIN = "EmailLogin";
    private static final String PRIMARY_LOGIN = "primary";
    private static final String CLAIM_URI = "ClaimUri";

    public ApiMgtDAO() {
        String enableJWTGeneration = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                .getFirstProperty(ENABLE_JWT_GENERATION);
        removeUserNameInJWTForAppToken = Boolean.parseBoolean(ServiceReferenceHolder.getInstance()
                                                                      .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                                                                      .getFirstProperty(APIConstants.API_KEY_MANAGER_REMOVE_USERNAME_TO_JWT_FOR_APP_TOKEN));
        if (enableJWTGeneration != null && JavaUtils.isTrueExplicitly(enableJWTGeneration)) {
            jwtGenerator = new JWTGenerator();
        }
    }

    /**
     * Get access token key for given userId and API Identifier
     *
     * @param userId          id of the user
     * @param applicationName name of the Application
     * @param identifier      APIIdentifier
     * @param keyType         Type of the key required
     * @return Access token
     * @throws APIManagementException if failed to get Access token
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public String getAccessKeyForAPI(String userId, String applicationName, APIInfoDTO identifier,
                                     String keyType)
            throws APIManagementException, IdentityException {

        String accessKey = null;

        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        //get the tenant id for the corresponding domain
        String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(loginUserName);
        int tenantId = IdentityUtil.getTenantIdOFUser(loginUserName);

        if (log.isDebugEnabled()) {
            log.debug("Searching for: " + identifier.getAPIIdentifier() + ", User: " + tenantAwareUserId +
                      ", ApplicationName: " + applicationName + ", Tenant ID: " + tenantId);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery =
                "SELECT " +
                "   SKM.ACCESS_TOKEN AS ACCESS_TOKEN " +
                "FROM " +
                "   AM_SUBSCRIPTION SP," +
                "   AM_API API," +
                "   AM_SUBSCRIBER SB," +
                "   AM_APPLICATION APP, " +
                "   AM_SUBSCRIPTION_KEY_MAPPING SKM " +
                "WHERE " +
                "   SB.USER_ID=? " +
                "   AND SB.TENANT_ID=? " +
                "   AND API.API_PROVIDER=? " +
                "   AND API.API_NAME=?" +
                "   AND API.API_VERSION=?" +
                "   AND APP.NAME=? " +
                "   AND SKM.KEY_TYPE=? " +
                "   AND API.API_ID = SP.API_ID" +
                "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                "   AND APP.APPLICATION_ID = SP.APPLICATION_ID " +
                "   AND SP.SUBSCRIPTION_ID = SKM.SUBSCRIPTION_ID ";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, tenantAwareUserId);
            ps.setInt(2, tenantId);
            ps.setString(3, APIUtil.replaceEmailDomainBack(identifier.getProviderId()));
            ps.setString(4, identifier.getApiName());
            ps.setString(5, identifier.getVersion());
            ps.setString(6, applicationName);
            ps.setString(7, keyType);

            rs = ps.executeQuery();

            while (rs.next()) {
                accessKey = APIUtil.decryptToken(rs.getString(APIConstants.SUBSCRIPTION_FIELD_ACCESS_TOKEN));
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL query to read the access key for user : "
                            + loginUserName + "of tenant(id) : " + tenantId, e);
        } catch (CryptoException e) {
            handleException("Error when decrypting access key for user : "
                            + loginUserName + "of tenant(id) : " + tenantId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return accessKey;
    }

    public String getAccessKeyForApplication(String userId, String applicationName,
                                             String keyType)
            throws APIManagementException, IdentityException {

        String accessKey = null;

        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(loginUserName);
        }

        //get the tenant id for the corresponding domain
        //String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(loginUserName);
        int tenantId = IdentityUtil.getTenantIdOFUser(loginUserName);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery =
                "SELECT " +
                "   IAT.ACCESS_TOKEN AS ACCESS_TOKEN " +
                "FROM " +
                "   AM_SUBSCRIBER SB," +
                "   AM_APPLICATION APP, " +
                "   AM_APPLICATION_KEY_MAPPING AKM," +
                accessTokenStoreTable + " IAT," +
                "   IDN_OAUTH_CONSUMER_APPS ICA " +
                "WHERE " +
                "   SB.USER_ID=? " +
                "   AND SB.TENANT_ID=? " +
                "   AND APP.NAME=? " +
                "   AND AKM.KEY_TYPE=? " +
                "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                "   AND APP.APPLICATION_ID = AKM.APPLICATION_ID" +
                "   AND ICA.CONSUMER_KEY = AKM.CONSUMER_KEY" +
                "   AND ICA.USERNAME = IAT.AUTHZ_USER" +
                "   AND IAT.CONSUMER_KEY = AKM.CONSUMER_KEY";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, loginUserName);
            ps.setInt(2, tenantId);
            ps.setString(3, applicationName);
            ps.setString(4, keyType);
            rs = ps.executeQuery();

            while (rs.next()) {
                accessKey = APIUtil.decryptToken(rs.getString(APIConstants.SUBSCRIPTION_FIELD_ACCESS_TOKEN));
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL query to read the access key for user : "
                            + loginUserName + "of tenant(id) : " + tenantId, e);
        } catch (CryptoException e) {
            handleException("Error when decrypting access key for user : "
                            + loginUserName + "of tenant(id) : " + tenantId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return accessKey;
    }

    /**
     * Get Subscribed APIs for given userId
     *
     * @param userId id of the user
     * @return APIInfoDTO[]
     * @throws APIManagementException if failed to get Subscribed APIs
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public APIInfoDTO[] getSubscribedAPIsOfUser(String userId) throws APIManagementException,
                                                                      IdentityException {

        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(loginUserName);
        int tenantId = IdentityUtil.getTenantIdOFUser(loginUserName);
        List<APIInfoDTO> apiInfoDTOList = new ArrayList<APIInfoDTO>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   API.API_PROVIDER AS API_PROVIDER," +
                          "   API.API_NAME AS API_NAME," +
                          "   API.API_VERSION AS API_VERSION " +
                          "FROM " +
                          "   AM_SUBSCRIPTION SP, " +
                          "   AM_API API," +
                          "   AM_SUBSCRIBER SB, " +
                          "   AM_APPLICATION APP " +
                          "WHERE " +
                          "   SB.USER_ID = ? " +
                          "   AND SB.TENANT_ID = ? " +
                          "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                          "   AND APP.APPLICATION_ID=SP.APPLICATION_ID " +
                          "   AND API.API_ID = SP.API_ID";
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, tenantAwareUsername);
            ps.setInt(2, tenantId);
            rs = ps.executeQuery();
            while (rs.next()) {
                APIInfoDTO infoDTO = new APIInfoDTO();
                infoDTO.setProviderId(APIUtil.replaceEmailDomain(rs.getString("API_PROVIDER")));
                infoDTO.setApiName(rs.getString("API_NAME"));
                infoDTO.setVersion(rs.getString("API_VERSION"));
                apiInfoDTOList.add(infoDTO);
            }
        } catch (SQLException e) {
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return apiInfoDTOList.toArray(new APIInfoDTO[apiInfoDTOList.size()]);
    }

    /**
     * Get API key information for given API
     *
     * @param apiInfoDTO API info
     * @return APIKeyInfoDTO[]
     * @throws APIManagementException if failed to get key info for given API
     */
    public APIKeyInfoDTO[] getSubscribedUsersForAPI(APIInfoDTO apiInfoDTO)
            throws APIManagementException {

        APIKeyInfoDTO[] apiKeyInfoDTOs = null;
        // api_id store as "providerName_apiName_apiVersion" in AM_SUBSCRIPTION table
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   SB.USER_ID, " +
                          "   SB.TENANT_ID " +
                          "FROM " +
                          "   AM_SUBSCRIBER SB, " +
                          "   AM_APPLICATION APP, " +
                          "   AM_SUBSCRIPTION SP, " +
                          "   AM_API API " +
                          "WHERE " +
                          "   API.API_PROVIDER = ? " +
                          "   AND API.API_NAME = ?" +
                          "   AND API.API_VERSION = ?" +
                          "   AND SP.APPLICATION_ID = APP.APPLICATION_ID " +
                          "   AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID " +
                          "   AND API.API_ID = SP.API_ID";
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(apiInfoDTO.getProviderId()));
            ps.setString(2, apiInfoDTO.getApiName());
            ps.setString(3, apiInfoDTO.getVersion());
            rs = ps.executeQuery();
            List<APIKeyInfoDTO> apiKeyInfoList = new ArrayList<APIKeyInfoDTO>();
            while (rs.next()) {
                String userId = rs.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID);
                //int tenantId = rs.getInt(APIConstants.SUBSCRIBER_FIELD_TENANT_ID);
                // If the tenant Id > 0, get the tenant domain and append it to the username.
                //if (tenantId > 0) {
                //  userId = userId + "@" + APIKeyMgtUtil.getTenantDomainFromTenantId(tenantId);
                //}
                APIKeyInfoDTO apiKeyInfoDTO = new APIKeyInfoDTO();
                apiKeyInfoDTO.setUserId(userId);
                // apiKeyInfoDTO.setStatus(rs.getString(3));
                apiKeyInfoList.add(apiKeyInfoDTO);
            }
            apiKeyInfoDTOs = apiKeyInfoList.toArray(new APIKeyInfoDTO[apiKeyInfoList.size()]);

        } catch (SQLException e) {
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return apiKeyInfoDTOs;
    }

    /**
     * This method is to update the access token
     *
     * @param userId     id of the user
     * @param apiInfoDTO Api info
     * @param statusEnum Status of the access key
     * @throws APIManagementException if failed to update the access token
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public void changeAccessTokenStatus(String userId, APIInfoDTO apiInfoDTO,
                                        String statusEnum)
            throws APIManagementException, IdentityException {
        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userId);
        int tenantId = 0;
        IdentityUtil.getTenantIdOFUser(userId);

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userId);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        String sqlQuery = "UPDATE " +
                          accessTokenStoreTable + " IAT , AM_SUBSCRIBER SB," +
                          " AM_SUBSCRIPTION SP , AM_APPLICATION APP, AM_API API" +
                          " SET IAT.TOKEN_STATE=?" +
                          " WHERE SB.USER_ID=?" +
                          " AND SB.TENANT_ID=?" +
                          " AND API.API_PROVIDER=?" +
                          " AND API.API_NAME=?" +
                          " AND API.API_VERSION=?" +
                          " AND SP.ACCESS_TOKEN=IAT.ACCESS_TOKEN" +
                          " AND SB.SUBSCRIBER_ID=APP.SUBSCRIBER_ID" +
                          " AND APP.APPLICATION_ID = SP.APPLICATION_ID" +
                          " AND API.API_ID = SP.API_ID";
        try {

            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, statusEnum);
            ps.setString(2, tenantAwareUsername);
            ps.setInt(3, tenantId);
            ps.setString(4, APIUtil.replaceEmailDomainBack(apiInfoDTO.getProviderId()));
            ps.setString(5, apiInfoDTO.getApiName());
            ps.setString(6, apiInfoDTO.getVersion());

            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                log.error("Failed to rollback the changeAccessTokenStatus operation", e);
            }
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, null);
        }
    }

    /**
     * Validate the provided key against the given API. First it will validate the key is valid
     * , ACTIVE and not expired.
     *
     * @param context     Requested Context
     * @param version     version of the API
     * @param accessToken Provided Access Token
     * @return APIKeyValidationInfoDTO instance with authorization status and tier information if
     *         authorized.
     * @throws APIManagementException Error when accessing the database or registry.
     */
    public APIKeyValidationInfoDTO validateKey(String context, String version, String accessToken, String requiredAuthenticationLevel)
            throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("A request is received to process the token : " + accessToken + " to access" +
                      " the context URL : " + context);
        }
        APIKeyValidationInfoDTO keyValidationInfoDTO = new APIKeyValidationInfoDTO();
        keyValidationInfoDTO.setAuthorized(false);

        String status;
        String tier;
        String type;
        String userType;
        String subscriberName;
        String subscriptionStatus;
        String applicationId;
        String applicationName;
        String applicationTier;
        String endUserName;
        long validityPeriod;
        long issuedTime;
        long timestampSkew;
        long currentTime;
        String apiName;
        String consumerKey;
        String apiPublisher;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        // First check whether the token is valid, active and not expired.
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String applicationSqlQuery = "SELECT " +
                                     "   IAT.VALIDITY_PERIOD, " +
                                     "   IAT.TIME_CREATED ," +
                                     "   IAT.TOKEN_STATE," +
                                     "   IAT.USER_TYPE," +
                                     "   IAT.AUTHZ_USER," +
                                     "   IAT.TIME_CREATED," +
                                     "   SUB.TIER_ID," +
                                     "   SUBS.USER_ID," +
                                     "   SUB.SUB_STATUS," +
                                     "   APP.APPLICATION_ID," +
                                     "   APP.NAME," +
                                     "   APP.APPLICATION_TIER," +
                                     "   AKM.KEY_TYPE," +
                                     "   API.API_NAME," +
                                     "   AKM.CONSUMER_KEY," +
                                     "   API.API_PROVIDER" +
                                     " FROM " + accessTokenStoreTable + " IAT," +
                                     "   AM_SUBSCRIPTION SUB," +
                                     "   AM_SUBSCRIBER SUBS," +
                                     "   AM_APPLICATION APP," +
                                     "   AM_APPLICATION_KEY_MAPPING AKM," +
                                     "   AM_API API" +
                                     " WHERE " +
                                     "   IAT.ACCESS_TOKEN = ? " +
                                     "   AND API.CONTEXT = ? " +
                                     "   AND API.API_VERSION = ? " +
                                     "   AND IAT.CONSUMER_KEY=AKM.CONSUMER_KEY " +
                                     //"   AND APP.APPLICATION_ID = APP.APPLICATION_ID" +
                                     "   AND SUB.APPLICATION_ID = APP.APPLICATION_ID" +
                                     "   AND APP.SUBSCRIBER_ID = SUBS.SUBSCRIBER_ID" +
                                     "   AND API.API_ID = SUB.API_ID" +
                                     "   AND AKM.APPLICATION_ID=APP.APPLICATION_ID";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(applicationSqlQuery);
            String encryptedAccessToken = APIUtil.encryptToken(accessToken);
            ps.setString(1, encryptedAccessToken);
            ps.setString(2, context);
            ps.setString(3, version);
            rs = ps.executeQuery();
            if (rs.next()) {
                status = rs.getString(APIConstants.IDENTITY_OAUTH2_FIELD_TOKEN_STATE);
                tier = rs.getString(APIConstants.SUBSCRIPTION_FIELD_TIER_ID);
                type = rs.getString(APIConstants.SUBSCRIPTION_KEY_TYPE);
                userType = rs.getString(APIConstants.SUBSCRIPTION_USER_TYPE);
                subscriberName = rs.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID);
                applicationId = rs.getString(APIConstants.APPLICATION_ID);
                applicationName = rs.getString(APIConstants.APPLICATION_NAME);
                applicationTier = rs.getString(APIConstants.APPLICATION_TIER);
                endUserName = rs.getString(APIConstants.IDENTITY_OAUTH2_FIELD_AUTHORIZED_USER);
                issuedTime = rs.getTimestamp(APIConstants.IDENTITY_OAUTH2_FIELD_TIME_CREATED,
                                             Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                validityPeriod = rs.getLong(APIConstants.IDENTITY_OAUTH2_FIELD_VALIDITY_PERIOD);
                timestampSkew = OAuthServerConfiguration.getInstance().
                        getTimeStampSkewInSeconds() * 1000;
                currentTime = System.currentTimeMillis();
                subscriptionStatus=rs.getString(APIConstants.SUBSCRIPTION_FIELD_SUB_STATUS);
                apiName = rs.getString(APIConstants.FIELD_API_NAME);
                consumerKey = rs.getString(APIConstants.FIELD_CONSUMER_KEY);
                apiPublisher = rs.getString(APIConstants.FIELD_API_PUBLISHER);

                /* If Subscription Status is PROD_ONLY_BLOCKED, block production access only */
                if (subscriptionStatus.equals(APIConstants.SubscriptionStatus.BLOCKED)) {
                    keyValidationInfoDTO.setValidationStatus(
                            APIConstants.KeyValidationStatus.API_BLOCKED);
                    keyValidationInfoDTO.setAuthorized(false);
                    return keyValidationInfoDTO;
                }
                else if(APIConstants.SubscriptionStatus.ON_HOLD.equals(subscriptionStatus) ||
                        APIConstants.SubscriptionStatus.REJECTED.equals(subscriptionStatus)){
                    keyValidationInfoDTO.setValidationStatus(APIConstants.KeyValidationStatus.SUBSCRIPTION_INACTIVE);
                    keyValidationInfoDTO.setAuthorized(false);
                    return keyValidationInfoDTO;
                }
                else if (subscriptionStatus.equals(APIConstants.SubscriptionStatus.PROD_ONLY_BLOCKED) &&
                           !APIConstants.API_KEY_TYPE_SANDBOX.equals(type)) {
                    keyValidationInfoDTO.setValidationStatus(
                            APIConstants.KeyValidationStatus.API_BLOCKED);
                    keyValidationInfoDTO.setAuthorized(false);
                    return keyValidationInfoDTO;
                }

                //check if 'requiredAuthenticationLevel' & the one associated with access token matches
                //This check should only be done for 'Application' and 'Application_User' levels
                if(requiredAuthenticationLevel.equals(APIConstants.AUTH_APPLICATION_LEVEL_TOKEN)
                   || requiredAuthenticationLevel.equals(APIConstants.AUTH_APPLICATION_USER_LEVEL_TOKEN)){
                    if(log.isDebugEnabled()){
                        log.debug("Access token's userType : "+userType + ".Required type : "+requiredAuthenticationLevel);
                    }

                    if (!(userType.equalsIgnoreCase(requiredAuthenticationLevel))){
                        keyValidationInfoDTO.setValidationStatus(
                                APIConstants.KeyValidationStatus.API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE);
                        keyValidationInfoDTO.setAuthorized(false);
                        return keyValidationInfoDTO;
                    }
                }

                // Check whether the token is ACTIVE
                if (APIConstants.TokenStatus.ACTIVE.equals(status)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Checking Access token: " + accessToken + " for validity." +
                                  "((currentTime - timestampSkew) > (issuedTime + validityPeriod)) : " +
                                  "((" + currentTime + "-" + timestampSkew + ")" + " > (" + issuedTime + " + " + validityPeriod + "))");
                    }
                    if (validityPeriod!=Long.MAX_VALUE && (currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
                        keyValidationInfoDTO.setValidationStatus(
                                APIConstants.KeyValidationStatus.API_AUTH_ACCESS_TOKEN_EXPIRED);
                        if (log.isDebugEnabled()) {
                            log.debug("Access token: " + accessToken + " has expired. " +
                                      "Reason ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) : " +
                                      "((" + currentTime + "-" + timestampSkew + ")" + " > (" + issuedTime + " + " + validityPeriod + "))");
                        }
                        //update token status as expired
                        updateTokenState(accessToken, conn, ps);
                        conn.commit();
                    } else {
                        keyValidationInfoDTO.setAuthorized(true);
                        keyValidationInfoDTO.setTier(tier);
                        keyValidationInfoDTO.setType(type);
                        keyValidationInfoDTO.setSubscriber(subscriberName);
                        keyValidationInfoDTO.setIssuedTime(issuedTime);
                        keyValidationInfoDTO.setAuthorizedDomains(ApiMgtDAO.getAuthorizedDomainList(accessToken));
                        keyValidationInfoDTO.setValidityPeriod(validityPeriod);
                        keyValidationInfoDTO.setUserType(userType);
                        keyValidationInfoDTO.setEndUserName(endUserName);
                        keyValidationInfoDTO.setApplicationId(applicationId);
                        keyValidationInfoDTO.setApplicationName(applicationName);
                        keyValidationInfoDTO.setApplicationTier(applicationTier);
                        keyValidationInfoDTO.setApiName(apiName);
                        keyValidationInfoDTO.setConsumerKey(APIUtil.decryptToken(consumerKey));
                        keyValidationInfoDTO.setApiPublisher(apiPublisher);

                        if (jwtGenerator != null) {
                            String calleeToken = null;

                            String enableJWTCache = ServiceReferenceHolder.getInstance()
                                    .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                                    .getFirstProperty(ENABLE_JWT_CACHE);

                            Cache jwtCache = Caching.getCacheManager(APIConstants.API_MANAGER_CACHE_MANAGER).
                                    getCache(APIConstants.JWT_CACHE_NAME);

                            //If JWT Caching is enabled.
                            if(enableJWTCache != null && JavaUtils.isTrueExplicitly(enableJWTCache)){
                                String cacheKey = accessToken + ":" + context + ":" + version + ":" + requiredAuthenticationLevel;
                                calleeToken = (String)jwtCache.get(cacheKey);
                                //On a Cache miss
                                if(calleeToken == null){
                                    //Generate Token and update Cache.
                                    calleeToken = generateJWTToken(userType, keyValidationInfoDTO, context, version);
                                    jwtCache.put(cacheKey, calleeToken);
                                }
                            }
                            else{
                                calleeToken = generateJWTToken(userType, keyValidationInfoDTO, context, version);
                            }

                            keyValidationInfoDTO.setEndUserToken(calleeToken);
                        }
                    }
                } else {
                    keyValidationInfoDTO.setValidationStatus(
                            APIConstants.KeyValidationStatus.API_AUTH_ACCESS_TOKEN_INACTIVE);
                    if (log.isDebugEnabled()) {
                        log.debug("Access token: " + accessToken + " is inactive");
                    }
                }
            } else {
                //no record found. Invalid access token received
                keyValidationInfoDTO.setValidationStatus(
                        APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
                if (log.isDebugEnabled()) {
                    log.debug("Access token: " + accessToken + " is invalid");
                }
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL ", e);
        } catch (CryptoException e) {
            handleException("Error when encrypting/decrypting token(s)", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return keyValidationInfoDTO;
    }

    private String generateJWTToken(String userType, APIKeyValidationInfoDTO keyValidationInfoDTO,
                                    String context, String version) throws APIManagementException {

        String jwtToken;
        if (removeUserNameInJWTForAppToken &&
                APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION.equalsIgnoreCase(userType)) {
            jwtToken = jwtGenerator.generateToken(keyValidationInfoDTO, context, version, false);
        } else {
            jwtToken = jwtGenerator.generateToken(keyValidationInfoDTO, context, version, true);
        }
        return jwtToken;
    }

    public long getApplicationAccessTokenRemainingValidityPeriod (String accessToken) throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        long validityPeriod;
        long issuedTime;
        long timestampSkew;
        long currentTime;
        long remainingTime = 0;

        String applicationSqlQuery = "SELECT " +
                                     " IAT.VALIDITY_PERIOD, " +
                                     " IAT.TIME_CREATED " +
                                     " FROM " + accessTokenStoreTable + " IAT" +
                                     " WHERE " +
                                     " IAT.ACCESS_TOKEN = ? " ;

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(applicationSqlQuery);
            ps.setString(1, accessToken);
            rs = ps.executeQuery();
            if (rs.next()) {
                issuedTime = rs.getTimestamp(APIConstants.IDENTITY_OAUTH2_FIELD_TIME_CREATED,
                                             Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                validityPeriod = rs.getLong(APIConstants.IDENTITY_OAUTH2_FIELD_VALIDITY_PERIOD);
                timestampSkew = OAuthServerConfiguration.getInstance().
                        getTimeStampSkewInSeconds() * 1000;
                currentTime = System.currentTimeMillis();
                remainingTime = ((currentTime) - (issuedTime + validityPeriod));
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return remainingTime;
    }

    //This returns the authorized client domains into a List
    public static List<String> getAuthorizedDomainList(String apiKey) throws APIManagementException {
        return Arrays.asList(getAuthorizedDomains(apiKey).split(","));
    }

    private void updateTokenState(String accessToken, Connection conn, PreparedStatement ps)
            throws SQLException, APIManagementException, CryptoException{

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        String encryptedAccessToken = APIUtil.encryptToken(accessToken);
        String UPDATE_TOKE_STATE_SQL =
                "UPDATE " +
                accessTokenStoreTable +
                " SET " +
                "   TOKEN_STATE = ? " +
                "   ,TOKEN_STATE_ID = ? " +
                "WHERE " +
                "   ACCESS_TOKEN = ?";
        ps = conn.prepareStatement(UPDATE_TOKE_STATE_SQL);
        ps.setString(1, "EXPIRED");
        ps.setString(2, UUID.randomUUID().toString());
        ps.setString(3, encryptedAccessToken);
        ps.executeUpdate();
    }

    public void addSubscriber(Subscriber subscriber) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "INSERT" +
                           " INTO AM_SUBSCRIBER (USER_ID, TENANT_ID, EMAIL_ADDRESS, DATE_SUBSCRIBED)" +
                           " VALUES (?,?,?,?)";

            ps = conn.prepareStatement(query, new String[]{"subscriber_id"});

            //ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, subscriber.getName());
            ps.setInt(2, subscriber.getTenantId());
            ps.setString(3, subscriber.getEmail());
            ps.setTimestamp(4, new Timestamp(subscriber.getSubscribedDate().getTime()));
            ps.executeUpdate();

            int subscriberId = 0;
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                //subscriberId = rs.getInt(1);
                subscriberId = Integer.valueOf(rs.getString(1)).intValue();
            }
            subscriber.setId(subscriberId);

            // Add default application
            Application defaultApp = new Application(APIConstants.DEFAULT_APPLICATION_NAME, subscriber);
            defaultApp.setTier(APIConstants.UNLIMITED_TIER);
            addApplication(defaultApp, subscriber.getName(), conn);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Error while rolling back the failed operation", e);
                }
            }
            handleException("Error in adding new subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public void updateSubscriber(Subscriber subscriber) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "UPDATE" +
                           " AM_SUBSCRIBER SET USER_ID=?, TENANT_ID=?, EMAIL_ADDRESS=?, DATE_SUBSCRIBED=?" +
                           " WHERE SUBSCRIBER_ID=?";
            ps = conn.prepareStatement(query);
            ps.setString(1, subscriber.getName());
            ps.setInt(2, subscriber.getTenantId());
            ps.setString(3, subscriber.getEmail());
            ps.setTimestamp(4, new Timestamp(subscriber.getSubscribedDate().getTime()));
            ps.setInt(5, subscriber.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            handleException("Error in updating subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public Subscriber getSubscriber(int subscriberId) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query =
                    "SELECT" +
                    " USER_ID, TENANT_ID, EMAIL_ADDRESS, DATE_SUBSCRIBED " +
                    "FROM " +
                    "AM_SUBSCRIBER" +
                    " WHERE " +
                    "SUBSCRIBER_ID=?";
            ps = conn.prepareStatement(query);
            ps.setInt(1, subscriberId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Subscriber subscriber = new Subscriber(rs.getString("USER_ID"));
                subscriber.setId(subscriberId);
                subscriber.setTenantId(rs.getInt("TENANT_ID"));
                subscriber.setEmail(rs.getString("EMAIL_ADDRESS"));
                subscriber.setSubscribedDate(new java.util.Date(
                        rs.getTimestamp("DATE_SUBSCRIBED").getTime()));
                return subscriber;
            }
        } catch (SQLException e) {
            handleException("Error while retrieving subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return null;
    }

    public int addSubscription(APIIdentifier identifier, String context, int applicationId, String status)
            throws APIManagementException {

        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int subscriptionId = -1;
        int apiId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            ps = conn.prepareStatement(getApiQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();

            if (apiId == -1) {
                String msg = "Unable to get the API ID for: " + identifier;
                log.error(msg);
                throw new APIManagementException(msg);
            }

            //This query to update the AM_SUBSCRIPTION table
            String sqlQuery = "INSERT " +
                              "INTO AM_SUBSCRIPTION (TIER_ID,API_ID,APPLICATION_ID,SUB_STATUS)" +
                              " VALUES (?,?,?,?)";

            //Adding data to the AM_SUBSCRIPTION table
            //ps = conn.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            ps = conn.prepareStatement(sqlQuery, new String[]{"SUBSCRIPTION_ID"});
            if (conn.getMetaData().getDriverName().contains("PostgreSQL")) {
                ps = conn.prepareStatement(sqlQuery, new String[]{"subscription_id"});
            }

            ps.setString(1, identifier.getTier());
            ps.setInt(2, apiId);
            ps.setInt(3, applicationId);
            ps.setString(4, status != null ? status : APIConstants.SubscriptionStatus.UNBLOCKED);

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                //subscriptionId = rs.getInt(1);
                subscriptionId = Integer.valueOf(rs.getString(1)).intValue();
            }
            ps.close();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return subscriptionId;
    }

    public void removeSubscription(APIIdentifier identifier, int applicationId)
            throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int subscriptionId = -1;
        int apiId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            ps = conn.prepareStatement(getApiQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();

            if (apiId == -1) {
                throw new APIManagementException("Unable to get the API ID for: " + identifier);
            }

            //This query to updates the AM_SUBSCRIPTION table
            String sqlQuery = "DELETE FROM AM_SUBSCRIPTION WHERE API_ID = ? AND APPLICATION_ID = ?";

            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            ps.setInt(2, applicationId);
            ps.executeUpdate();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    public void removeSubscriptionById(int subscription_id) throws APIManagementException {
            Connection conn = null;
            ResultSet resultSet = null;
            PreparedStatement ps = null;

            try {
                conn = APIMgtDBUtil.getConnection();
                //Remove entry from AM_SUBSCRIPTION table
                String sqlQuery = "DELETE FROM AM_SUBSCRIPTION WHERE SUBSCRIPTION_ID = ?";

                ps = conn.prepareStatement(sqlQuery);
                ps.setInt(1, subscription_id);
                ps.executeUpdate();

                //Commit transaction
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException e1) {
                        log.error("Failed to rollback remove subscription ", e);
                    }
                }
                handleException("Failed to remove subscription data ", e);
            } finally {
                APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
            }
    }

    public String getSubscriptionStatusById(int subscriptionId) throws APIManagementException {

            Connection conn = null;
            ResultSet resultSet = null;
            PreparedStatement ps = null;
            String subscriptionStatus = null;

            try {
                conn = APIMgtDBUtil.getConnection();
                String getApiQuery = "SELECT SUB_STATUS FROM AM_SUBSCRIPTION WHERE SUBSCRIPTION_ID = ?";
                ps = conn.prepareStatement(getApiQuery);
                ps.setInt(1, subscriptionId);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    subscriptionStatus = resultSet.getString("SUB_STATUS");
                }
                resultSet.close();
                ps.close();
                return subscriptionStatus;
            } catch (SQLException e) {
                handleException("Failed to retrieve subscription status", e);
            } finally {
                APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
            }
        return null;
    }

    /**
     * This method used tot get Subscriber from subscriberId.
     *
     * @param subscriberName id
     * @return Subscriber
     * @throws APIManagementException if failed to get Subscriber from subscriber id
     */
    public Subscriber getSubscriber(String subscriberName) throws APIManagementException {

        Connection conn = null;
        Subscriber subscriber = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        int tenantId;
        try {
            tenantId = IdentityUtil.getTenantIdOFUser(subscriberName);
        } catch (IdentityException e) {
            String msg = "Failed to get tenant id of user : " + subscriberName;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }

        String sqlQuery = "SELECT " +
                          "   SUBSCRIBER_ID, " +
                          "   USER_ID, " +
                          "   TENANT_ID, " +
                          "   EMAIL_ADDRESS, " +
                          "   DATE_SUBSCRIBED " +
                          "FROM " +
                          "   AM_SUBSCRIBER " +
                          "WHERE " +
                          "   USER_ID = ? " +
                          "   AND TENANT_ID = ?";
        try {
            conn = APIMgtDBUtil.getConnection();

            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, subscriberName);
            ps.setInt(2, tenantId);
            result = ps.executeQuery();

            if (result.next()) {
                subscriber = new Subscriber(result.getString(
                        APIConstants.SUBSCRIBER_FIELD_EMAIL_ADDRESS));
                subscriber.setEmail(result.getString("EMAIL_ADDRESS"));
                subscriber.setId(result.getInt("SUBSCRIBER_ID"));
                subscriber.setName(subscriberName);
                subscriber.setSubscribedDate(result.getDate(
                        APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscriber.setTenantId(result.getInt("TENANT_ID"));
            }

        } catch (SQLException e) {
            handleException("Failed to get Subscriber for :" + subscriberName, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, result);
        }
        return subscriber;
    }

    public Set<APIIdentifier> getAPIByConsumerKey(String accessToken)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String getAPISql = "SELECT" +
                           " API.API_PROVIDER," +
                           " API.API_NAME," +
                           " API.API_VERSION " +
                           "FROM" +
                           " AM_SUBSCRIPTION SUB," +
                           " AM_SUBSCRIPTION_KEY_MAPPING SKM, " +
                           " AM_API API " +
                           "WHERE" +
                           " SKM.ACCESS_TOKEN=?" +
                           " AND SKM.SUBSCRIPTION_ID=SUB.SUBSCRIPTION_ID" +
                           " AND API.API_ID = SUB.API_ID";

        Set<APIIdentifier> apiList = new HashSet<APIIdentifier>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getAPISql);
            String encryptedAccessToken = APIUtil.encryptToken(accessToken);
            nestedPS.setString(1, encryptedAccessToken);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiList.add(new APIIdentifier(nestedRS.getString("API_PROVIDER"),
                                              nestedRS.getString("API_NAME"),
                                              nestedRS.getString("API_VERSION")));
            }
        } catch (SQLException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } catch (CryptoException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiList;
    }

    /**
     * This method returns the set of APIs for given subscriber, subscribed under the specified application.
     *
     * @param subscriber subscriber
     * @param applicationName Application Name
     * @return Set<API>
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to get SubscribedAPIs
     */
    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber,String applicationName)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = new LinkedHashSet<SubscribedAPI>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.SUBSCRIPTION_ID" +
                              "   ,API.API_PROVIDER AS API_PROVIDER" +
                              "   ,API.API_NAME AS API_NAME" +
                              "   ,API.API_VERSION AS API_VERSION" +
                              "   ,SUBS.TIER_ID AS TIER_ID" +
                              "   ,APP.APPLICATION_ID AS APP_ID" +
                              "   ,SUBS.LAST_ACCESSED AS LAST_ACCESSED" +
                              "   ,SUBS.SUB_STATUS AS SUB_STATUS" +
                              "   ,APP.NAME AS APP_NAME " +
                              "   ,APP.CALLBACK_URL AS CALLBACK_URL " +
                              "FROM " +
                              "   AM_SUBSCRIBER SUB," +
                              "   AM_APPLICATION APP, " +
                              "   AM_SUBSCRIPTION SUBS, " +
                              "   AM_API API " +
                              "WHERE " +
                              "   SUB.USER_ID = ? " +
                              "   AND SUB.TENANT_ID = ? " +
                              "   AND SUB.SUBSCRIBER_ID=APP.SUBSCRIBER_ID " +
                              "   AND APP.APPLICATION_ID=SUBS.APPLICATION_ID " +
                              "   AND API.API_ID=SUBS.API_ID" +
                              "   AND APP.NAME= ? ";


            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, subscriber.getName());
            int tenantId = IdentityUtil.getTenantIdOFUser(subscriber.getName());
            ps.setInt(2, tenantId);
            ps.setString(3, applicationName);
            result = ps.executeQuery();

            if (result == null) {
                return subscribedAPIs;
            }

            while (result.next()) {
                APIIdentifier apiIdentifier = new APIIdentifier(APIUtil.replaceEmailDomain(result.getString("API_PROVIDER")),
                                                                result.getString("API_NAME"), result.getString("API_VERSION"));

                SubscribedAPI subscribedAPI = new SubscribedAPI(subscriber, apiIdentifier);
                subscribedAPI.setSubStatus(result.getString("SUB_STATUS"));
                subscribedAPI.setTier(new Tier(
                        result.getString(APIConstants.SUBSCRIPTION_FIELD_TIER_ID)));
                subscribedAPI.setLastAccessed(result.getDate(
                        APIConstants.SUBSCRIPTION_FIELD_LAST_ACCESS));

                Application application = new Application(result.getString("APP_NAME"), subscriber);
                subscribedAPI.setApplication(application);
                subscribedAPIs.add(subscribedAPI);
            }

        } catch (SQLException e) {
            handleException("Failed to get SubscribedAPI of :" + subscriber.getName(), e);
        } catch (IdentityException e) {
            handleException("Failed get tenant id of user " + subscriber.getName(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribedAPIs;
    }

    /**
     * This method returns the set of APIs for given subscriber
     *
     * @param subscriber subscriber
     * @return Set<API>
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to get SubscribedAPIs
     */
    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = new LinkedHashSet<SubscribedAPI>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        //identify subscribeduser used email/ordinalusername
        String subscribedUserName = getLoginUserName(subscriber.getName());
        subscriber.setName(subscribedUserName);

        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.SUBSCRIPTION_ID" +
                              "   ,API.API_PROVIDER AS API_PROVIDER" +
                              "   ,API.API_NAME AS API_NAME" +
                              "   ,API.API_VERSION AS API_VERSION" +
                              "   ,SUBS.TIER_ID AS TIER_ID" +
                              "   ,APP.APPLICATION_ID AS APP_ID" +
                              "   ,SUBS.LAST_ACCESSED AS LAST_ACCESSED" +
                              "   ,SUBS.SUB_STATUS AS SUB_STATUS" +
                              "   ,APP.NAME AS APP_NAME " +
                              "   ,APP.CALLBACK_URL AS CALLBACK_URL " +
                              "FROM " +
                              "   AM_SUBSCRIBER SUB," +
                              "   AM_APPLICATION APP, " +
                              "   AM_SUBSCRIPTION SUBS, " +
                              "   AM_API API " +
                              "WHERE " +
                              "   SUB.USER_ID = ? " +
                              "   AND SUB.TENANT_ID = ? " +
                              "   AND SUB.SUBSCRIBER_ID=APP.SUBSCRIBER_ID " +
                              "   AND APP.APPLICATION_ID=SUBS.APPLICATION_ID " +
                              "   AND API.API_ID=SUBS.API_ID";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, subscriber.getName());
            int tenantId = IdentityUtil.getTenantIdOFUser(subscriber.getName());
            ps.setInt(2, tenantId);
            result = ps.executeQuery();

            if (result == null) {
                return subscribedAPIs;
            }

            Map<String, Set<SubscribedAPI>> map = new TreeMap<String, Set<SubscribedAPI>>();
            LRUCache<Integer, Application> applicationCache = new LRUCache<Integer, Application>(100);

            while (result.next()) {
                APIIdentifier apiIdentifier = new APIIdentifier(APIUtil.replaceEmailDomain(result.getString("API_PROVIDER")),
                                                                result.getString("API_NAME"), result.getString("API_VERSION"));

                SubscribedAPI subscribedAPI = new SubscribedAPI(subscriber, apiIdentifier);
                subscribedAPI.setSubStatus(result.getString("SUB_STATUS"));
                String tierName=result.getString(APIConstants.SUBSCRIPTION_FIELD_TIER_ID);
                subscribedAPI.setTier(new Tier(
                        tierName));
                subscribedAPI.setLastAccessed(result.getDate(
                        APIConstants.SUBSCRIPTION_FIELD_LAST_ACCESS));
                //setting NULL for subscriber. If needed, Subscriber object should be constructed &
                // passed in
                int applicationId = result.getInt("APP_ID");
                Application application = applicationCache.get(applicationId);
                if (application == null) {
                    application = new Application(result.getString("APP_NAME"), subscriber);
                    application.setId(result.getInt("APP_ID"));
                    application.setCallbackUrl(result.getString("CALLBACK_URL"));
                    //String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(subscriber.getName());
                    String tenantAwareUserId = subscriber.getName();
                    Set<APIKey> keys = getApplicationKeys(tenantAwareUserId, applicationId);
                    for (APIKey key : keys) {
                        application.addKey(key);
                    }
                    applicationCache.put(applicationId, application);
                }
                subscribedAPI.setApplication(application);

                int subscriptionId = result.getInt(APIConstants.SUBSCRIPTION_FIELD_SUBSCRIPTION_ID);
                Set<APIKey> apiKeys = getAPIKeysBySubscription(subscriptionId);
                for (APIKey key : apiKeys) {
                    subscribedAPI.addKey(key);
                }

                if (!map.containsKey(application.getName())) {
                    map.put(application.getName(), new TreeSet<SubscribedAPI>(new Comparator<SubscribedAPI>() {
                        public int compare(SubscribedAPI o1, SubscribedAPI o2) {
                            int placement = o1.getApiId().getApiName().compareTo(o2.getApiId().getApiName());
                            if (placement == 0) {
                                return new APIVersionComparator().compare(new API(o1.getApiId()),
                                                                          new API(o2.getApiId()));
                            }
                            return placement;
                        }
                    }));
                }
                map.get(application.getName()).add(subscribedAPI);
            }

            for (String application : map.keySet()) {
                Set<SubscribedAPI> apis = map.get(application);
                for (SubscribedAPI api : apis) {
                    subscribedAPIs.add(api);
                }
            }

        } catch (SQLException e) {
            handleException("Failed to get SubscribedAPI of :" + subscriber.getName(), e);
        } catch (IdentityException e) {
            handleException("Failed get tenant id of user " + subscriber.getName(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribedAPIs;
    }

    private Set<APIKey> getAPIKeysBySubscription(int subscriptionId) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String getKeysSql = "SELECT " +
                            " SKM.ACCESS_TOKEN AS ACCESS_TOKEN," +
                            " SKM.KEY_TYPE AS TOKEN_TYPE " +
                            "FROM" +
                            " AM_SUBSCRIPTION_KEY_MAPPING SKM " +
                            "WHERE" +
                            " SKM.SUBSCRIPTION_ID = ?";

        String authorizedDomains;

        Set<APIKey> apiKeys = new HashSet<APIKey>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
            nestedPS.setInt(1, subscriptionId);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                APIKey apiKey = new APIKey();
                String decryptedAccessToken = APIUtil.decryptToken(nestedRS.getString("ACCESS_TOKEN"));
                apiKey.setAccessToken(decryptedAccessToken);
                apiKey.setType(nestedRS.getString("TOKEN_TYPE"));
                apiKeys.add(apiKey);
            }
        } catch (SQLException e) {
            handleException("Failed to get API keys for subscription: " + subscriptionId, e);
        } catch (CryptoException e) {
            handleException("Failed to get API keys for subscription: " + subscriptionId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKeys;
    }

    public String getTokenScope(String consumerKey) throws APIManagementException {
        String tokenScope = null;

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    tokenScope = getTokenScope(consumerKey, getScopeSql(keyStoreTable));
                    if (tokenScope != null) {
                        break;
                    }
                }
            }
        } else {
            tokenScope = getTokenScope(consumerKey, getScopeSql(null));
        }
        return tokenScope;
    }

    private String getTokenScope(String consumerKey, String getScopeSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String tokenScope = null;

        try {

            consumerKey = APIUtil.encryptToken(consumerKey);
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getScopeSql);
            nestedPS.setString(1, consumerKey);
            ResultSet nestedRS = nestedPS.executeQuery();
            if (nestedRS.next()) {
                tokenScope = nestedRS.getString("TOKEN_SCOPE");
            }
        } catch (SQLException e) {
            handleException("Failed to get token scope from consumer key: " + consumerKey, e);
        } catch (CryptoException e) {
            handleException("Error while encrypting consumer key", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }

        return tokenScope;
    }

    private String getScopeSql(String accessTokenStoreTable) {
        String tokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT" +
               " IAT.TOKEN_SCOPE AS TOKEN_SCOPE " +
               "FROM " +
               tokenStoreTable + " IAT," +
               " IDN_OAUTH_CONSUMER_APPS ICA " +
               "WHERE" +
               " IAT.CONSUMER_KEY = ?" +
               " AND IAT.CONSUMER_KEY = ICA.CONSUMER_KEY" +
               " AND IAT.AUTHZ_USER = ICA.USERNAME";
    }

    public Boolean isAccessTokenExists(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE ACCESS_TOKEN= ? ";
        Boolean tokenExists = false;
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            String encryptedAccessToken = APIUtil.encryptToken(accessToken);
            getToken.setString(1, encryptedAccessToken);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                tokenExists = true;
            }
        } catch (SQLException e) {
            handleException("Failed to check availability of the access token. ", e);
        } catch (CryptoException e) {
            handleException("Failed to check availability of the access token. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenExists;
    }

    public Boolean isAccessTokenRevoked(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT TOKEN_STATE " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE ACCESS_TOKEN= ? ";
        Boolean tokenExists = false;
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            String encryptedAccessToken = APIUtil.encryptToken(accessToken);
            getToken.setString(1, encryptedAccessToken);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                if (!getTokenRS.getString("TOKEN_STATE").equals("REVOKED")) {
                    tokenExists = true;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to check availability of the access token. ", e);
        } catch (CryptoException e) {
            handleException("Failed to check availability of the access token. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenExists;
    }

    public APIKey getAccessTokenData(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        APIKey apiKey=new APIKey();

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                             "TIME_CREATED,VALIDITY_PERIOD " +
                             "FROM " + accessTokenStoreTable  +
                             " WHERE ACCESS_TOKEN= ? AND TOKEN_STATE='ACTIVE' ";
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, APIUtil.encryptToken(accessToken));
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {

                String decryptedAccessToken = APIUtil.decryptToken(getTokenRS.getString("ACCESS_TOKEN")); // todo - check redundant decryption
                apiKey.setAccessToken(decryptedAccessToken);
                apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                String consumerKey = getTokenRS.getString("CONSUMER_KEY");
                apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                apiKey.setValidityPeriod(getTokenRS.getLong("VALIDITY_PERIOD"));

            }
        } catch (SQLException e) {
            handleException("Failed to get the access token data. ", e);
        } catch (CryptoException e) {
            handleException("Failed to get the access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKey;
    }

    public Map<Integer, APIKey> getAccessTokens(String query)
            throws APIManagementException {
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();
        if (APIUtil.checkAccessTokenPartitioningEnabled()
            && APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    Map<Integer, APIKey> tokenDataMapTmp = getAccessTokens(query,
                                                                           getTokenSql(keyStoreTable));
                    tokenDataMap.putAll(tokenDataMapTmp);
                }
            }
        } else {
            tokenDataMap = getAccessTokens(query, getTokenSql(null));
        }
        return tokenDataMap;
    }

    private Map<Integer, APIKey> getAccessTokens(String query, String getTokenSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                String accessToken = APIUtil.decryptToken(getTokenRS.getString("ACCESS_TOKEN"));
                String regex = "(?i)[a-zA-Z0-9_.-|]*" + query.trim() + "(?i)[a-zA-Z0-9_.-|]*";
                Pattern pattern;
                Matcher matcher;
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(accessToken);
                Integer i = 0;
                if (matcher.matches()) {
                    APIKey apiKey = new APIKey();
                    apiKey.setAccessToken(accessToken);
                    apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                    apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                    apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                    String consumerKey = getTokenRS.getString("CONSUMER_KEY");
                    apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                    apiKey.setValidityPeriod(getTokenRS.getLong("VALIDITY_PERIOD"));
                    tokenDataMap.put(i, apiKey);
                    i++;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } catch (CryptoException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);

        }
        return tokenDataMap;
    }

    private String getTokenSql (String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
               "TIME_CREATED,VALIDITY_PERIOD " +
               "FROM " + tokenStoreTable + " WHERE TOKEN_STATE='ACTIVE' ";
    }

    public Map<Integer, APIKey> getAccessTokensByUser(String user, String loggedInUser)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(user);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                             "TIME_CREATED,VALIDITY_PERIOD " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE AUTHZ_USER= ? AND TOKEN_STATE='ACTIVE' ";
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, user);
            ResultSet getTokenRS = getToken.executeQuery();
            Integer i = 0;
            while (getTokenRS.next()) {
                String authorizedUser = getTokenRS.getString("AUTHZ_USER");
                if (APIUtil.isLoggedInUserAuthorizedToRevokeToken(loggedInUser, authorizedUser)) {
                    String accessToken = APIUtil.decryptToken(getTokenRS.getString("ACCESS_TOKEN"));
                    APIKey apiKey = new APIKey();
                    apiKey.setAccessToken(accessToken);
                    apiKey.setAuthUser(authorizedUser);
                    apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                    apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                    String consumerKey = getTokenRS.getString("CONSUMER_KEY");
                    apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                    apiKey.setValidityPeriod(getTokenRS.getLong("VALIDITY_PERIOD"));
                    tokenDataMap.put(i, apiKey);
                    i++;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } catch (CryptoException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenDataMap;
    }

    public Map<Integer, APIKey> getAccessTokensByDate(String date, boolean latest, String loggedInUser)
            throws APIManagementException {
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    Map<Integer, APIKey> tokenDataMapTmp = getAccessTokensByDate
                            (date, latest, getTokenByDateSqls(keyStoreTable), loggedInUser);
                    tokenDataMap.putAll(tokenDataMapTmp);
                }
            }
        } else {
            tokenDataMap = getAccessTokensByDate(date, latest, getTokenByDateSqls(null), loggedInUser);
        }

        return tokenDataMap;
    }

    public Map<Integer, APIKey> getAccessTokensByDate(String date, boolean latest, String[] querySql, String loggedInUser)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            java.util.Date searchDate = fmt.parse(date);
            Date sqlDate = new Date(searchDate.getTime());
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken;
            if (latest) {
                getToken = connection.prepareStatement(querySql[0]);
            } else {
                getToken = connection.prepareStatement(querySql[1]);
            }
            getToken.setDate(1, sqlDate);

            ResultSet getTokenRS = getToken.executeQuery();
            Integer i = 0;
            while (getTokenRS.next()) {
                String authorizedUser = getTokenRS.getString("AUTHZ_USER");
                if (APIUtil.isLoggedInUserAuthorizedToRevokeToken(loggedInUser, authorizedUser)) {
                    String accessToken = APIUtil.decryptToken(getTokenRS.getString("ACCESS_TOKEN"));
                    APIKey apiKey = new APIKey();
                    apiKey.setAccessToken(accessToken);
                    apiKey.setAuthUser(authorizedUser);
                    apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                    apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                    String consumerKey = getTokenRS.getString("CONSUMER_KEY");
                    apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                    apiKey.setValidityPeriod(getTokenRS.getLong("VALIDITY_PERIOD"));
                    tokenDataMap.put(i, apiKey);
                    i++;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } catch (ParseException e) {
            handleException("Failed to get access token data. ", e);
        } catch (CryptoException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenDataMap;
    }

    public String[] getTokenByDateSqls (String accessTokenStoreTable) {
        String[] querySqlArr = new String[2];
        String tokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        querySqlArr[0] = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                         "TIME_CREATED,VALIDITY_PERIOD " +
                         "FROM " + tokenStoreTable  +
                         " WHERE TOKEN_STATE='ACTIVE' AND TIME_CREATED >= ? ";

        querySqlArr[1] = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                         "TIME_CREATED,VALIDITY_PERIOD " +
                         "FROM " + tokenStoreTable +
                         " WHERE TOKEN_STATE='ACTIVE' AND TIME_CREATED <= ? ";

        return querySqlArr;
    }

    private Set<APIKey> getApplicationKeys(String username, int applicationId)
            throws APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(username);
        }

        Set<APIKey> apiKeys = new HashSet<APIKey>();

        try{
            APIKey productionKey = getProductionKeyOfApplication(username, applicationId, accessTokenStoreTable);
            if(productionKey != null){
                apiKeys.add(productionKey);
            }

            APIKey sandboxKey = getSandboxKeyOfApplication(username, applicationId, accessTokenStoreTable);
            if(sandboxKey != null){
                apiKeys.add(sandboxKey);
            }
        } catch (SQLException e) {
            handleException("Failed to get keys for application: " + applicationId, e);
        } catch (CryptoException e) {
            handleException("Failed to get keys for application: " + applicationId, e);
        }
        return apiKeys;
    }

    private APIKey getProductionKeyOfApplication(String userName, int applicationId, String accessTokenStoreTable)
                                                throws SQLException, CryptoException, APIManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        //The part of the sql query that remain common across databases.
        String statement =
                " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                        " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                        " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                        " IAT.VALIDITY_PERIOD AS VALIDITY_PERIOD," +
                        " AKM.KEY_TYPE AS TOKEN_TYPE " +
                        "FROM" +
                        " AM_APPLICATION_KEY_MAPPING AKM," +
                        accessTokenStoreTable + " IAT," +
                        " IDN_OAUTH_CONSUMER_APPS ICA " +
                        "WHERE" +
                        " AKM.APPLICATION_ID = ? AND" +
                        " ICA.USERNAME = ? AND" +
                        " IAT.USER_TYPE = ? AND" +
                        " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                        " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY AND" +
                        " IAT.TOKEN_SCOPE = 'PRODUCTION' AND" +
                        " ICA.USERNAME = IAT.AUTHZ_USER AND" +
                        " (IAT.TOKEN_STATE = 'ACTIVE' OR" +
                        " IAT.TOKEN_STATE = 'EXPIRED' OR" +
                        " IAT.TOKEN_STATE = 'REVOKED')" +
                        " ORDER BY IAT.TIME_CREATED DESC";

        String sql = null, oracleSQL = null, mySQLSQL = null, msSQL = null,postgreSQL = null;

        //Construct database specific sql statements.
        oracleSQL = "SELECT ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                        " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                        " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                        " IAT.VALIDITY_PERIOD AS VALIDITY_PERIOD," +
                        " AKM.KEY_TYPE AS TOKEN_TYPE " +
                        " FROM" +
                        " AM_APPLICATION_KEY_MAPPING AKM, " +
                        accessTokenStoreTable + " IAT," +
                        " IDN_OAUTH_CONSUMER_APPS ICA " +
                        " WHERE" +
                        " AKM.APPLICATION_ID = ? AND" +
                        " ICA.USERNAME = ? AND" +
                        " IAT.USER_TYPE = ? AND" +
                        " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                        " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY AND" +
                        " IAT.TOKEN_SCOPE = 'PRODUCTION' AND" +
                        " ICA.USERNAME = IAT.AUTHZ_USER AND" +
                        " (IAT.TOKEN_STATE = 'ACTIVE' OR" +
                        " IAT.TOKEN_STATE = 'EXPIRED' OR" +
                        " IAT.TOKEN_STATE = 'REVOKED')" +
                        " AND ROWNUM < 2 " +
                        " ORDER BY IAT.TIME_CREATED DESC";

        mySQLSQL = "SELECT" + statement + " LIMIT 1";

        msSQL = "SELECT TOP 1" + statement;

        postgreSQL = "SELECT * FROM (SELECT" + statement + ") AS TOKEN LIMIT 1";

        String authorizedDomains;
        String accessToken;

        try{
            connection = APIMgtDBUtil.getConnection();

            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = mySQLSQL;
            }
            else if(connection.getMetaData().getDriverName().contains("MS SQL")){
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = postgreSQL;
            } else {
                sql = oracleSQL;
            }

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, applicationId);
            preparedStatement.setString(2, userName.toLowerCase());
            preparedStatement.setString(3, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                APIKey apiKey = new APIKey();
                accessToken = APIUtil.decryptToken(resultSet.getString("ACCESS_TOKEN"));
                String consumerKey = resultSet.getString("CONSUMER_KEY");
                apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                String consumerSecret = resultSet.getString("CONSUMER_SECRET");
                apiKey.setConsumerSecret(APIUtil.decryptToken(consumerSecret));
                apiKey.setAccessToken(accessToken);
                authorizedDomains = getAuthorizedDomains(accessToken);
                apiKey.setType(resultSet.getString("TOKEN_TYPE"));
                apiKey.setAuthorizedDomains(authorizedDomains);
                apiKey.setValidityPeriod(resultSet.getLong("VALIDITY_PERIOD"));
                return apiKey;
            }
            return null;
        }finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
    }

    private APIKey getSandboxKeyOfApplication(String userName, int applicationId, String accessTokenStoreTable)
            throws SQLException, CryptoException, APIManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        //The part of the sql query that remain common across databases.
        String statement =
                " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                        " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                        " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                        " IAT.VALIDITY_PERIOD AS VALIDITY_PERIOD," +
                        " AKM.KEY_TYPE AS TOKEN_TYPE " +
                        "FROM" +
                        " AM_APPLICATION_KEY_MAPPING AKM," +
                        accessTokenStoreTable + " IAT," +
                        " IDN_OAUTH_CONSUMER_APPS ICA " +
                        "WHERE" +
                        " AKM.APPLICATION_ID = ? AND" +
                        " ICA.USERNAME = ? AND" +
                        " IAT.USER_TYPE = ? AND" +
                        " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                        " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY AND" +
                        " IAT.TOKEN_SCOPE = 'SANDBOX' AND" +
                        " ICA.USERNAME = IAT.AUTHZ_USER AND" +
                        " (IAT.TOKEN_STATE = 'ACTIVE' OR" +
                        " IAT.TOKEN_STATE = 'EXPIRED')" +
                        " ORDER BY IAT.TIME_CREATED DESC";

        String sql = null, oracleSQL = null, mySQLSQL = null, msSQL = null,postgreSQL = null;

        //Construct database specific sql statements.
        oracleSQL =  "SELECT ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                        " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                        " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                        " IAT.VALIDITY_PERIOD AS VALIDITY_PERIOD," +
                        " AKM.KEY_TYPE AS TOKEN_TYPE " +
                        "FROM" +
                        " AM_APPLICATION_KEY_MAPPING AKM," +
                        accessTokenStoreTable + " IAT," +
                        " IDN_OAUTH_CONSUMER_APPS ICA " +
                        "WHERE" +
                        " AKM.APPLICATION_ID = ? AND" +
                        " ICA.USERNAME = ? AND" +
                        " IAT.USER_TYPE = ? AND" +
                        " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                        " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY AND" +
                        " IAT.TOKEN_SCOPE = 'SANDBOX' AND" +
                        " ICA.USERNAME = IAT.AUTHZ_USER AND" +
                        " (IAT.TOKEN_STATE = 'ACTIVE' OR" +
                        " IAT.TOKEN_STATE = 'EXPIRED')" +
                        " AND ROWNUM < 2 " +
                        " ORDER BY IAT.TIME_CREATED DESC ";

        mySQLSQL = "SELECT" + statement + " LIMIT 1";

        msSQL = "SELECT TOP 1" + statement;

        postgreSQL = "SELECT * FROM (SELECT" + statement + ") AS TOKEN LIMIT 1";

        String authorizedDomains;
        String accessToken;

        try{
            connection = APIMgtDBUtil.getConnection();

            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = mySQLSQL;
            }
            else if(connection.getMetaData().getDriverName().contains("MS SQL")){
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = postgreSQL;
            } else {
                sql = oracleSQL;
            }

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, applicationId);
            preparedStatement.setString(2, userName);
            preparedStatement.setString(3, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                APIKey apiKey = new APIKey();
                accessToken = APIUtil.decryptToken(resultSet.getString("ACCESS_TOKEN"));
                String consumerKey = resultSet.getString("CONSUMER_KEY");
                apiKey.setConsumerKey(APIUtil.decryptToken(consumerKey));
                String consumerSecret = resultSet.getString("CONSUMER_SECRET");
                apiKey.setConsumerSecret(APIUtil.decryptToken(consumerSecret));
                apiKey.setAccessToken(accessToken);
                authorizedDomains = getAuthorizedDomains(accessToken);
                apiKey.setType(resultSet.getString("TOKEN_TYPE"));
                apiKey.setAuthorizedDomains(authorizedDomains);
                apiKey.setValidityPeriod(resultSet.getLong("VALIDITY_PERIOD"));
                return apiKey;
            }
            return null;
        }finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
    }

    public Set<String> getApplicationKeys(int applicationId)
            throws APIManagementException {
        Set<String> apiKeys = new HashSet<String>();
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    apiKeys = getApplicationKeys(applicationId, getKeysSql(keyStoreTable));
                    if (apiKeys != null) {
                        break;
                    }
                }
            }
        } else {
            apiKeys = getApplicationKeys(applicationId, getKeysSql(null));
        }
        return apiKeys;
    }

    public void updateTierPermissions(String tierName, String permissionType, String roles, int tenantId) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        int tierPermissionId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getTierPermissionQuery = "SELECT TIER_PERMISSIONS_ID FROM AM_TIER_PERMISSIONS WHERE TIER = ? AND TENANT_ID = ?";
            ps = conn.prepareStatement(getTierPermissionQuery);
            ps.setString(1, tierName);
            ps.setInt(2, tenantId);
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                tierPermissionId = resultSet.getInt("TIER_PERMISSIONS_ID");
            }
            resultSet.close();
            ps.close();

            if (tierPermissionId == -1) {
                String query = "INSERT INTO" +
                               " AM_TIER_PERMISSIONS (TIER, PERMISSIONS_TYPE, ROLES, TENANT_ID)" +
                               " VALUES(?, ?, ?, ?)";
                ps = conn.prepareStatement(query);
                ps.setString(1, tierName);
                ps.setString(2, permissionType);
                ps.setString(3, roles);
                ps.setInt(4, tenantId);
                ps.execute();
            } else {
                String query = "UPDATE" +
                               " AM_TIER_PERMISSIONS SET TIER = ?, PERMISSIONS_TYPE = ?, ROLES = ?" +
                               " WHERE TIER_PERMISSIONS_ID = ? AND TENANT_ID = ?";
                ps = conn.prepareStatement(query);
                ps.setString(1, tierName);
                ps.setString(2, permissionType);
                ps.setString(3, roles);
                ps.setInt(4, tierPermissionId);
                ps.setInt(5, tenantId);
                ps.executeUpdate();
            }
            conn.commit();

        } catch (SQLException e) {
            handleException("Error in updating tier permissions: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public Set<TierPermissionDTO> getTierPermissions(int tenantId) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        Set<TierPermissionDTO> tierPermissions = new HashSet<TierPermissionDTO>();

        try {
            conn = APIMgtDBUtil.getConnection();
            String getTierPermissionQuery = "SELECT TIER , PERMISSIONS_TYPE , ROLES  FROM AM_TIER_PERMISSIONS WHERE " +
            							"TENANT_ID = ?";
            ps = conn.prepareStatement(getTierPermissionQuery);
            ps.setInt(1, tenantId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                TierPermissionDTO tierPermission = new TierPermissionDTO();
                tierPermission.setTierName(resultSet.getString("TIER"));
                tierPermission.setPermissionType(resultSet.getString("PERMISSIONS_TYPE"));
                String roles = resultSet.getString("ROLES");
                if (roles != null && !roles.equals("")) {
                    String roleList[] = roles.split(",");
                    tierPermission.setRoles(roleList);
                }
                tierPermissions.add(tierPermission);
            }
            resultSet.close();
            ps.close();
        } catch (SQLException e) {
            handleException("Failed to get Tier permission information " , e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return tierPermissions;
    }

    public TierPermissionDTO getTierPermission(String tierName, int tenantId) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        TierPermissionDTO tierPermission = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String getTierPermissionQuery = "SELECT PERMISSIONS_TYPE , ROLES  FROM AM_TIER_PERMISSIONS" +
                                            " WHERE TIER = ? AND TENANT_ID = ?";
            ps = conn.prepareStatement(getTierPermissionQuery);
            ps.setString(1, tierName);
            ps.setInt(2, tenantId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                tierPermission = new TierPermissionDTO();
                tierPermission.setTierName(tierName);
                tierPermission.setPermissionType(resultSet.getString("PERMISSIONS_TYPE"));
                String roles = resultSet.getString("ROLES");
                if (roles != null) {
                    String roleList[] = roles.split(",");
                    tierPermission.setRoles(roleList);
                }
            }
            resultSet.close();
            ps.close();
        } catch (SQLException e) {
            handleException("Failed to get Tier permission information for Tier " + tierName , e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return tierPermission;
    }

    private Set<String> getApplicationKeys(int applicationId, String getKeysSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Set<String> apiKeys = new HashSet<String>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
            nestedPS.setInt(1, applicationId);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiKeys.add(APIUtil.decryptToken(nestedRS.getString("ACCESS_TOKEN")));
            }
        } catch (SQLException e) {
            handleException("Failed to get keys for application: " + applicationId, e);
        } catch (CryptoException e) {
            handleException("Failed to get keys for application: " + applicationId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKeys;
    }

    private String getKeysSql(String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT " +
               " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
               " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
               " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
               " AKM.KEY_TYPE AS TOKEN_TYPE " +
               "FROM" +
               " AM_APPLICATION_KEY_MAPPING AKM," +
               tokenStoreTable + " IAT," +
               " IDN_OAUTH_CONSUMER_APPS ICA " +
               "WHERE" +
               " AKM.APPLICATION_ID = ? AND" +
               " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
               " ICA.CONSUMER_KEY = IAT.CONSUMER_KEY";
    }

    /**
     * Get access token data based on application ID
     *
     * @param subscriptionId Subscription Id
     * @return access token data
     * @throws APIManagementException
     */
    public Map<String, String> getAccessTokenData(int subscriptionId)
            throws APIManagementException {
        Map<String, String> apiKeys = new HashMap<String, String>();

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    apiKeys = getAccessTokenData(subscriptionId,
                                                 getKeysSqlUsingSubscriptionId(keyStoreTable));
                    if (apiKeys != null) {
                        break;
                    }
                }
            }
        } else {
            apiKeys = getAccessTokenData(subscriptionId, getKeysSqlUsingSubscriptionId(null));
        }
        return apiKeys;
    }

    private Map<String, String> getAccessTokenData(int subscriptionId, String getKeysSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<String, String> apiKeys = new HashMap<String, String>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
            nestedPS.setInt(1, subscriptionId);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiKeys.put("token", APIUtil.decryptToken(nestedRS.getString("ACCESS_TOKEN")));
                apiKeys.put("status", nestedRS.getString("TOKEN_STATE"));
            }
        } catch (SQLException e) {
            handleException("Failed to get keys for application: " + subscriptionId, e);
        } catch (CryptoException e) {
            handleException("Failed to get keys for application: " + subscriptionId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKeys;
    }

    private String getKeysSqlUsingSubscriptionId(String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT " +
               " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
               " IAT.TOKEN_STATE AS TOKEN_STATE" +
               " FROM" +
               " AM_APPLICATION_KEY_MAPPING AKM," +
               " AM_SUBSCRIPTION SM," +
               tokenStoreTable + " IAT," +
               " IDN_OAUTH_CONSUMER_APPS ICA " +
               "WHERE" +
               " SM.SUBSCRIPTION_ID = ? AND" +
               " SM.APPLICATION_ID= AKM.APPLICATION_ID AND" +
               " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
               " ICA.CONSUMER_KEY = IAT.CONSUMER_KEY";
    }

    /**
     * This method returns the set of Subscribers for given provider
     *
     * @param providerName name of the provider
     * @return Set<Subscriber>
     * @throws APIManagementException if failed to get subscribers for given provider
     */
    public Set<Subscriber> getSubscribersOfProvider(String providerName)
            throws APIManagementException {

        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.USER_ID AS USER_ID," +
                              "   SUBS.EMAIL_ADDRESS AS EMAIL_ADDRESS, " +
                              "   SUBS.DATE_SUBSCRIBED AS DATE_SUBSCRIBED " +
                              "FROM " +
                              "   AM_SUBSCRIBER  SUBS," +
                              "   AM_APPLICATION  APP, " +
                              "   AM_SUBSCRIPTION SUB, " +
                              "   AM_API API " +
                              "WHERE  " +
                              "   SUB.APPLICATION_ID = APP.APPLICATION_ID " +
                              "   AND SUBS. SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                              "   AND API.API_ID = SUB.API_ID " +
                              "   AND API.API_PROVIDER = ?";


            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(providerName));
            result = ps.executeQuery();

            while (result.next()) {
                // Subscription table should have API_VERSION AND API_PROVIDER
                Subscriber subscriber =
                        new Subscriber(result.getString(
                                APIConstants.SUBSCRIBER_FIELD_EMAIL_ADDRESS));
                subscriber.setName(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(result.getDate(
                        APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscribers.add(subscriber);
            }

        } catch (SQLException e) {
            handleException("Failed to subscribers for :" + providerName, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribers;
    }

    public Set<Subscriber> getSubscribersOfAPI(APIIdentifier identifier)
            throws APIManagementException {

        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            String sqlQuery = "SELECT DISTINCT " +
                              "SB.USER_ID, SB.DATE_SUBSCRIBED " +
                              "FROM AM_SUBSCRIBER SB, AM_SUBSCRIPTION SP,AM_APPLICATION APP,AM_API API" +
                              " WHERE API.API_PROVIDER=? " +
                              "AND API.API_NAME=? " +
                              "AND API.API_VERSION=? " +
                              "AND SP.APPLICATION_ID=APP.APPLICATION_ID" +
                              " AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID " +
                              " AND API.API_ID = SP.API_ID";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            result = ps.executeQuery();
            if (result == null) {
                return subscribers;
            }
            while (result.next()) {
                Subscriber subscriber =
                        new Subscriber(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(
                        result.getTimestamp(APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscribers.add(subscriber);
            }

        } catch (SQLException e) {
            handleException("Failed to get subscribers for :" + identifier.getApiName(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribers;
    }

    public long getAPISubscriptionCountByAPI(APIIdentifier identifier)
            throws APIManagementException {

        String sqlQuery = "SELECT" +
                          " COUNT(SUB.SUBSCRIPTION_ID) AS SUB_ID" +
                          " FROM AM_SUBSCRIPTION SUB, AM_API API " +
                          " WHERE API.API_PROVIDER=? " +
                          " AND API.API_NAME=?" +
                          " AND API.API_VERSION=?" +
                          " AND API.API_ID=SUB.API_ID";
        long subscriptions = 0;

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            result = ps.executeQuery();
            if (result == null) {
                return subscriptions;
            }
            while (result.next()) {
                subscriptions = result.getLong("SUB_ID");
            }
        } catch (SQLException e) {
            handleException("Failed to get subscription count for API", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscriptions;
    }

    /**
     * This method is used to update the subscriber
     *
     * @param identifier    APIIdentifier
     * @param context       Context of the API
     * @param applicationId Application id
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to update subscriber
     */
    public void updateSubscriptions(APIIdentifier identifier, String context, int applicationId)
            throws APIManagementException {
        addSubscription(identifier, context, applicationId, APIConstants.SubscriptionStatus.UNBLOCKED);
    }
    /**
     * This method is used to update the subscription
     *
     * @param identifier    APIIdentifier
     * @param subStatus    Subscription Status[BLOCKED/UNBLOCKED]
     * @param applicationId Application id
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to update subscriber
     */
    public void updateSubscription(APIIdentifier identifier, String subStatus, int applicationId)
            throws APIManagementException {

        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int apiId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            ps = conn.prepareStatement(getApiQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();

            if (apiId == -1) {
                String msg = "Unable to get the API ID for: " + identifier;
                log.error(msg);
                throw new APIManagementException(msg);
            }

            //This query to update the AM_SUBSCRIPTION table
            String sqlQuery ="UPDATE AM_SUBSCRIPTION SET SUB_STATUS = ? WHERE API_ID = ? AND APPLICATION_ID = ?";

            //Updating data to the AM_SUBSCRIPTION table
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, subStatus);
            ps.setInt(2, apiId);
            ps.setInt(3, applicationId);
            ps.execute();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to update subscription data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    public void updateSubscriptionStatus(int subscriptionId, String status) throws APIManagementException{

        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        try {
            conn = APIMgtDBUtil.getConnection();

            //This query is to update the AM_SUBSCRIPTION table
            String sqlQuery ="UPDATE AM_SUBSCRIPTION SET SUB_STATUS = ? WHERE SUBSCRIPTION_ID = ?";

            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, status);
            ps.setInt(2, subscriptionId);
            ps.execute();

            //Commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback subscription status update ", e);
                }
            }
            handleException("Failed to update subscription status ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    /**
     * Update refreshed ApplicationAccesstoken's usertype
     * @param keyType
     * @param accessToken
     * @param validityPeriod
     * @return
     * @throws IdentityException
     * @throws APIManagementException
     */
    public void updateRefreshedApplicationAccessToken(String keyType, String newAccessToken,
                                                      long validityPeriod) throws IdentityException,
                                                                                  APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(newAccessToken);
        }
        // Update Access Token
        String sqlUpdateNewAccessToken = "UPDATE " + accessTokenStoreTable +
                                         " SET USER_TYPE=?, VALIDITY_PERIOD=? " +
                                         " WHERE ACCESS_TOKEN=? AND TOKEN_SCOPE=? ";

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            prepStmt = connection.prepareStatement(sqlUpdateNewAccessToken);
            prepStmt.setString(1, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            if (validityPeriod < 0) {
                prepStmt.setLong(2, Long.MAX_VALUE);
            } else {
                prepStmt.setLong(2, validityPeriod * 1000);
            }
            prepStmt.setString(3, APIUtil.encryptToken(newAccessToken));
            prepStmt.setString(4, keyType);

            prepStmt.execute();
            prepStmt.close();

            connection.commit();

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
        } catch (CryptoException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

    }

    public void updateAccessAllowDomains(String accessToken, String[] accessAllowDomains)
            throws APIManagementException {
        String consumerKey = findConsumerKeyFromAccessToken(accessToken);
        String sqlDeleteAccessAllowDomains = "DELETE " +
                                             " FROM AM_APP_KEY_DOMAIN_MAPPING " +
                                             " WHERE CONSUMER_KEY=?";

        String sqlAddAccessAllowDomains = "INSERT" +
                                          " INTO AM_APP_KEY_DOMAIN_MAPPING (CONSUMER_KEY, AUTHZ_DOMAIN) " +
                                          " VALUES (?,?)";

        Connection connection = null ;
        PreparedStatement prepStmt = null;
        try {

            consumerKey = APIUtil.encryptToken(consumerKey);
            connection = APIMgtDBUtil.getConnection();
            //first delete the existing domain list for access token
            prepStmt = connection.prepareStatement(sqlDeleteAccessAllowDomains);
            prepStmt.setString(1, consumerKey);
            prepStmt.execute();
            prepStmt.close();

            //add the new domain list for access token
            if (accessAllowDomains != null && !accessAllowDomains[0].trim().equals("")) {
                for (int i = 0; i < accessAllowDomains.length; i++) {
                    prepStmt = connection.prepareStatement(sqlAddAccessAllowDomains);
                    prepStmt.setString(1, consumerKey);
                    prepStmt.setString(2, accessAllowDomains[i].trim());
                    prepStmt.execute();
                    prepStmt.close();
                }
            } else {
                prepStmt = connection.prepareStatement(sqlAddAccessAllowDomains);
                prepStmt.setString(1, consumerKey);
                prepStmt.setString(2, "ALL");
                prepStmt.execute();
                prepStmt.close();
            }
            connection.commit();
        } catch (SQLException e) {
            handleException("Failed to update the access allow domains.", e);
        } catch (CryptoException e) {
            handleException("Error while encrypting consumer-key", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }


    /**
     * @param consumerKey     ConsumerKey
     * @param applicationName Application name
     * @param userId          User Id
     * @param tenantId        Tenant Id of the user
     * @param apiInfoDTO      Application Info DTO
     * @param keyType         Type (scope) of the key
     * @return accessToken
     * @throws IdentityException if failed to register accessToken
     */
    public String registerAccessToken(String consumerKey, String applicationName, String userId,
                                      int tenantId, APIInfoDTO apiInfoDTO, String keyType)
            throws IdentityException, APIManagementException {

        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        String accessToken = OAuthUtil.getRandomNumber();

        if (APIUtil.checkUserNameAssertionEnabled()) {
            //use ':' for token & userName separation
            String accessTokenStrToEncode = accessToken + ":" + loginUserName;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            if (APIUtil.checkAccessTokenPartitioningEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(loginUserName);
            }
        }

        // Add Access Token
        String sqlAddAccessToken = "INSERT" +
                                   " INTO " + accessTokenStoreTable +
                                   "(ACCESS_TOKEN, CONSUMER_KEY, TOKEN_STATE, TOKEN_SCOPE) " +
                                   " VALUES (?,?,?,?)";

        String getSubscriptionId = "SELECT SUBS.SUBSCRIPTION_ID " +
                                   "FROM " +
                                   "  AM_SUBSCRIPTION SUBS, " +
                                   "  AM_APPLICATION APP, " +
                                   "  AM_SUBSCRIBER SUB, " +
                                   "  AM_API API " +
                                   "WHERE " +
                                   "  SUB.USER_ID = ?" +
                                   "  AND SUB.TENANT_ID = ?" +
                                   "  AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID" +
                                   "  AND APP.NAME = ?" +
                                   "  AND API.API_PROVIDER = ?" +
                                   "  AND API.API_NAME = ?" +
                                   "  AND API.API_VERSION = ?" +
                                   "  AND APP.APPLICATION_ID = SUBS.APPLICATION_ID" +
                                   "  AND API.API_ID = SUBS.API_ID";

        String addSubscriptionKeyMapping = "INSERT " +
                                           "INTO AM_SUBSCRIPTION_KEY_MAPPING (SUBSCRIPTION_ID, ACCESS_TOKEN, KEY_TYPE) " +
                                           "VALUES (?,?,?)";

        //String apiId = apiInfoDTO.getProviderId()+"_"+apiInfoDTO.getApiName()+"_"+apiInfoDTO.getVersion();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            consumerKey = APIUtil.encryptToken(consumerKey);
            connection = APIMgtDBUtil.getConnection();
            String encryptedAccessToken = APIUtil.encryptToken(accessToken);
            //Add access token
            prepStmt = connection.prepareStatement(sqlAddAccessToken);
            prepStmt.setString(1, encryptedAccessToken);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, APIConstants.TokenStatus.ACTIVE);
            prepStmt.setString(4, keyType);
            prepStmt.execute();
            prepStmt.close();

            //Update subscription with new key context mapping
            int subscriptionId = -1;
            prepStmt = connection.prepareStatement(getSubscriptionId);
            prepStmt.setString(1,loginUserName);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, applicationName);
            prepStmt.setString(4, APIUtil.replaceEmailDomainBack(apiInfoDTO.getProviderId()));
            prepStmt.setString(5, apiInfoDTO.getApiName());
            prepStmt.setString(6, apiInfoDTO.getVersion());
            ResultSet getSubscriptionIdResult = prepStmt.executeQuery();
            while (getSubscriptionIdResult.next()) {
                subscriptionId = getSubscriptionIdResult.getInt(1);
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(addSubscriptionKeyMapping);
            prepStmt.setInt(1, subscriptionId);
            prepStmt.setString(2, encryptedAccessToken);
            prepStmt.setString(3, keyType);
            prepStmt.execute();
            prepStmt.close();

            connection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
            //  throw new IdentityException("Error when storing the access code for consumer key : " + consumerKey);
        } catch (CryptoException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessToken;
    }

    public String registerApplicationAccessToken(String consumerKey, String applicationName,
                                                 String userId,
                                                 int tenantId, String keyType,
                                                 String[] accessAllowDomains, String validityTime)
            throws IdentityException, APIManagementException {

        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        String accessToken = OAuthUtil.getRandomNumber();

        if (APIUtil.checkUserNameAssertionEnabled()) {
            //use ':' for token & userName separation
            String accessTokenStrToEncode = accessToken + ":" + loginUserName ;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            if (APIUtil.checkAccessTokenPartitioningEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(loginUserName);
            }
        }

        // Add Access Token
        String sqlAddAccessToken = "INSERT" +
                                   " INTO " +  accessTokenStoreTable +"(ACCESS_TOKEN, CONSUMER_KEY, TOKEN_STATE, TOKEN_SCOPE, AUTHZ_USER, USER_TYPE, TIME_CREATED, VALIDITY_PERIOD) " +
                                   " VALUES (?,?,?,?,?,?,?,?)";

        String getApplicationId = "SELECT APP.APPLICATION_ID " +
                                  "FROM " +
                                  "  AM_APPLICATION APP, " +
                                  "  AM_SUBSCRIBER SUB " +
                                  "WHERE " +
                                  "  SUB.USER_ID = ?" +
                                  "  AND SUB.TENANT_ID = ?" +
                                  "  AND APP.NAME = ?" +
                                  "  AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID";

        String addApplicationKeyMapping = "INSERT " +
                                          "INTO AM_APPLICATION_KEY_MAPPING (APPLICATION_ID, CONSUMER_KEY, KEY_TYPE) " +
                                          "VALUES (?,?,?)";

        String sqlAddAccessAllowDomains = "INSERT" +
                                          " INTO AM_APP_KEY_DOMAIN_MAPPING (CONSUMER_KEY, AUTHZ_DOMAIN) " +
                                          " VALUES (?,?)";

        Connection connection = null;
        PreparedStatement prepStmt = null;
        long validityPeriod = getApplicationAccessTokenValidityPeriod();
        if(validityTime != null && !"".equals(validityTime)){
            //When validity time getting passed from Jaggery, it append .0 to the actual value.
            // Hence final value passed to backend contains decimals (eg: 3600.0)
            validityPeriod = (long)Double.parseDouble(validityTime);
        }

        try {
            consumerKey = APIUtil.encryptToken(consumerKey);
            connection = APIMgtDBUtil.getConnection();
            //Add access token
            prepStmt = connection.prepareStatement(sqlAddAccessToken);
            prepStmt.setString(1, APIUtil.encryptToken(accessToken));
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, APIConstants.TokenStatus.ACTIVE);
            prepStmt.setString(4, keyType);
            prepStmt.setString(5, loginUserName.toLowerCase());
            prepStmt.setString(6, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            prepStmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()),
                                  Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            if (validityPeriod < 0) {
                prepStmt.setLong(8, Long.MAX_VALUE);
            } else {
                prepStmt.setLong(8, validityPeriod * 1000);
            }
            prepStmt.execute();
            prepStmt.close();

            int applicationId = -1;
            prepStmt = connection.prepareStatement(getApplicationId);
            prepStmt.setString(1, loginUserName);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, applicationName);
            ResultSet getApplicationIdResult = prepStmt.executeQuery();
            while (getApplicationIdResult.next()) {
                applicationId = getApplicationIdResult.getInt(1);
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(addApplicationKeyMapping);
            prepStmt.setInt(1, applicationId);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, keyType);
            prepStmt.execute();
            prepStmt.close();

            if (accessAllowDomains != null && !accessAllowDomains[0].trim().equals("")) {
                for (int i = 0; i < accessAllowDomains.length; i++) {
                    prepStmt = connection.prepareStatement(sqlAddAccessAllowDomains);
                    prepStmt.setString(1, consumerKey);
                    prepStmt.setString(2, accessAllowDomains[i].trim());
                    prepStmt.execute();
                    prepStmt.close();
                }
            } else {
                prepStmt = connection.prepareStatement(sqlAddAccessAllowDomains);
                prepStmt.setString(1, consumerKey);
                prepStmt.setString(2, "ALL");
                prepStmt.execute();
                prepStmt.close();
            }
            connection.commit();
        } catch (SQLException e) {
            handleException("Error while generating the application access token for the application :"+applicationName, e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
            //  throw new IdentityException("Error when storing the access code for consumer key : " + consumerKey);
        } catch (CryptoException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessToken;
    }


    /**
     * @param apiIdentifier APIIdentifier
     * @param userId        User Id
     * @return true if user subscribed for given APIIdentifier
     * @throws APIManagementException if failed to check subscribed or not
     */
    public boolean isSubscribed(APIIdentifier apiIdentifier, String userId)
            throws APIManagementException {
        boolean isSubscribed = false;
        //identify loggedinuser
        String loginUserName = getLoginUserName(userId);

        String apiId = apiIdentifier.getProviderName() + "_" + apiIdentifier.getApiName() + "_" +
                       apiIdentifier.getVersion();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   SUBS.TIER_ID ," +
                          "   API.API_PROVIDER ," +
                          "   API.API_NAME ," +
                          "   API.API_VERSION ," +
                          "   SUBS.LAST_ACCESSED ," +
                          "   SUBS.APPLICATION_ID " +
                          "FROM " +
                          "   AM_SUBSCRIPTION SUBS," +
                          "   AM_SUBSCRIBER SUB, " +
                          "   AM_APPLICATION  APP, " +
                          "   AM_API API " +
                          "WHERE " +
                          "   API.API_PROVIDER  = ?" +
                          "   AND API.API_NAME = ?" +
                          "   AND API.API_VERSION = ?" +
                          "   AND SUB.USER_ID = ?" +
                          "   AND SUB.TENANT_ID = ? " +
                          "   AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID" +
                          "   AND API.API_ID = SUBS.API_ID";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
            ps.setString(2, apiIdentifier.getApiName());
            ps.setString(3, apiIdentifier.getVersion());
            ps.setString(4, loginUserName);
            int tenantId;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(loginUserName);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + loginUserName;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            ps.setInt(5, tenantId);

            rs = ps.executeQuery();

            if (rs.next()) {
                isSubscribed = true;
            }
        } catch (SQLException e) {
            handleException("Error while checking if user has subscribed to the API ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return isSubscribed;
    }

    /**
     * @param providerName Name of the provider
     * @return UserApplicationAPIUsage of given provider
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to get
     *          UserApplicationAPIUsage for given provider
     */
    public UserApplicationAPIUsage[] getAllAPIUsageByProvider(String providerName)
            throws APIManagementException {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;


        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.SUBSCRIPTION_ID AS SUBSCRIPTION_ID, " +
                              "   SUBS.APPLICATION_ID AS APPLICATION_ID, " +
                              "   SUBS.SUB_STATUS AS SUB_STATUS, " +
                              "   SUBS.TIER_ID AS TIER_ID, " +
                              "   API.API_PROVIDER AS API_PROVIDER, " +
                              "   API.API_NAME AS API_NAME, " +
                              "   API.API_VERSION AS API_VERSION, " +
                              "   SUBS.LAST_ACCESSED AS LAST_ACCESSED, " +
                              "   SUB.USER_ID AS USER_ID, " +
                              "   APP.NAME AS APPNAME " +
                              "FROM " +
                              "   AM_SUBSCRIPTION SUBS, " +
                              "   AM_APPLICATION APP, " +
                              "   AM_SUBSCRIBER SUB, " +
                              "   AM_API API " +
                              "WHERE " +
                              "   SUBS.APPLICATION_ID = APP.APPLICATION_ID " +
                              "   AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID " +
                              "   AND API.API_PROVIDER = ? " +
                              "   AND API.API_ID = SUBS.API_ID " +
                              "ORDER BY " +
                              "   APP.NAME";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(providerName));
            result = ps.executeQuery();

            Map<String, UserApplicationAPIUsage> userApplicationUsages = new TreeMap<String, UserApplicationAPIUsage>();
            while (result.next()) {
                int subId = result.getInt("SUBSCRIPTION_ID");
                Map<String, String> keyData = getAccessTokenData(subId);
                String accessToken = keyData.get("token");
                String tokenStatus = keyData.get("status");
                String userId = result.getString("USER_ID");
                String application = result.getString("APPNAME");
                int appId = result.getInt("APPLICATION_ID");
                String subStatus = result.getString("SUB_STATUS");
                String key = userId + "::" + application;
                UserApplicationAPIUsage usage = userApplicationUsages.get(key);
                if (usage == null) {
                    usage = new UserApplicationAPIUsage();
                    usage.setUserId(userId);
                    usage.setApplicationName(application);
                    usage.setAppId(appId);
                    usage.setAccessToken(accessToken);
                    usage.setAccessTokenStatus(tokenStatus);
                    userApplicationUsages.put(key, usage);
                }
                APIIdentifier apiId=new APIIdentifier(result.getString("API_PROVIDER"),
                                                      result.getString("API_NAME"), result.getString("API_VERSION"));
                SubscribedAPI apiSubscription=new SubscribedAPI(new Subscriber(userId),apiId);
                apiSubscription.setSubStatus(subStatus);
                usage.addApiSubscriptions(apiSubscription);

            }
            return userApplicationUsages.values().toArray(
                    new UserApplicationAPIUsage[userApplicationUsages.size()]);

        } catch (SQLException e) {
            handleException("Failed to find API Usage for :" + providerName, e);
            return null;
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
    }

    /**
     * return the subscriber for given access token
     *
     * @param accessToken AccessToken
     * @return Subscriber
     * @throws APIManagementException if failed to get subscriber for given access token
     */
    public Subscriber getSubscriberById(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Subscriber subscriber = null;
        String query = " SELECT" +
                       " SB.USER_ID, SB.DATE_SUBSCRIBED" +
                       " FROM AM_SUBSCRIBER SB , AM_SUBSCRIPTION SP, AM_APPLICATION APP, AM_SUBSCRIPTION_KEY_MAPPING SKM" +
                       " WHERE SKM.ACCESS_TOKEN=?" +
                       " AND SP.APPLICATION_ID=APP.APPLICATION_ID" +
                       " AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID" +
                       " AND SP.SUBSCRIPTION_ID=SKM.SUBSCRIPTION_ID";

        try {
            connection = APIMgtDBUtil.getConnection();
            ps = connection.prepareStatement(query);
            ps.setString(1, APIUtil.encryptToken(accessToken));

            result = ps.executeQuery();
            while (result.next()) {
                subscriber = new Subscriber(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(result.getDate(APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
            }

        } catch (SQLException e) {
            handleException("Failed to get Subscriber for accessToken", e);
        } catch (CryptoException e) {
            handleException("Failed to get Subscriber for accessToken", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscriber;
    }

    public String[] getOAuthCredentials(String accessToken, String tokenType)
            throws APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String consumerKey = null;
        String consumerSecret = null;
        String sqlStmt = "SELECT " +
                         " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                         " ICA.CONSUMER_SECRET AS CONSUMER_SECRET " +
                         "FROM " +
                         " IDN_OAUTH_CONSUMER_APPS ICA," +
                         accessTokenStoreTable + " IAT" +
                         " WHERE " +
                         " IAT.ACCESS_TOKEN = ? AND" +
                         " IAT.TOKEN_SCOPE = ? AND" +
                         " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, APIUtil.encryptToken(accessToken));
            prepStmt.setString(2, tokenType);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                consumerKey = rs.getString("CONSUMER_KEY");
                consumerSecret = rs.getString("CONSUMER_SECRET");

                consumerKey = APIUtil.decryptToken(consumerKey);
                consumerSecret = APIUtil.decryptToken(consumerSecret);
            }

        } catch (SQLException e) {
            handleException("Error when adding a new OAuth consumer.", e);
        } catch (CryptoException e) {
            handleException("Error while encrypting/decrypting tokens/app credentials.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
        return new String[]{consumerKey, consumerSecret};
    }

    public String[] addOAuthConsumer(String username, int tenantId, String appName, String callbackUrl)
            throws IdentityOAuthAdminException, APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        //identify loggedinuser
        String loginUserName = getLoginUserName(username);
        String sqlStmt = "INSERT INTO IDN_OAUTH_CONSUMER_APPS " +
                         "(CONSUMER_KEY, CONSUMER_SECRET, USERNAME, TENANT_ID, OAUTH_VERSION, APP_NAME, CALLBACK_URL) VALUES (?,?,?,?,?,?, ?) ";
        String consumerKey;
        String consumerSecret = OAuthUtil.getRandomNumber();

        do {
            consumerKey = OAuthUtil.getRandomNumber();
        }
        while (isDuplicateConsumer(consumerKey));

        try {

            consumerKey = APIUtil.encryptToken(consumerKey);
            consumerSecret = APIUtil.encryptToken(consumerSecret);

            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, consumerKey);
            prepStmt.setString(2, consumerSecret);
            prepStmt.setString(3, loginUserName.toLowerCase());
            prepStmt.setInt(4, tenantId);
            prepStmt.setString(5, OAuthConstants.OAuthVersions.VERSION_2);
            prepStmt.setString(6, appName);
            prepStmt.setString(7, callbackUrl);
            prepStmt.execute();

            connection.commit();

        } catch (SQLException e) {
            handleException("Error when adding a new OAuth consumer.", e);
        } catch (CryptoException e) {
            handleException("Error while attempting to encrypt consumer-key, consumer-secret.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
        try {
            return new String[]{APIUtil.decryptToken(consumerKey), APIUtil.decryptToken(consumerSecret)};
        } catch (CryptoException e) {
            handleException("Error while decrypting consumer-key, consumer-secret", e);
        }
        return null;
    }


    private void updateOAuthConsumerApp(String appName, String callbackUrl)
            throws IdentityOAuthAdminException, APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sqlStmt = "UPDATE IDN_OAUTH_CONSUMER_APPS " +
                         "SET CALLBACK_URL = ? WHERE APP_NAME = ?";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, callbackUrl);
            prepStmt.setString(2, appName);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            handleException("Error when updating OAuth consumer App for " + appName, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    private boolean isDuplicateConsumer(String consumerKey) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        String sqlQuery = "SELECT * FROM IDN_OAUTH_CONSUMER_APPS " +
                          "WHERE CONSUMER_KEY=?";

        boolean isDuplicateConsumer = false;

        try {
            consumerKey = APIUtil.encryptToken(consumerKey);
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, consumerKey);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isDuplicateConsumer = true;
            }
        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } catch (CryptoException e) {
            handleException("Error while encrypting consumer-key", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rSet);
        }
        return isDuplicateConsumer;
    }


    public int addApplication(Application application, String userId)
            throws APIManagementException {
        Connection conn = null;
        int applicationId = 0;
        String loginUserName = getLoginUserName(userId);
        try {
            conn = APIMgtDBUtil.getConnection();
            applicationId = addApplication(application,loginUserName, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add Application ", e);
                }
            }
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
		return applicationId;
    }

    public void addRating(APIIdentifier apiId, int rating,String user)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            addRating(apiId,rating,user, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add Application ", e);
                }
            }
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    /**
     * @param apiIdentifier API Identifier
     * @param userId      User Id
     * @throws APIManagementException if failed to add Application
     */
    public void addRating(APIIdentifier apiIdentifier,int rating,String userId,Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        PreparedStatement psSelect = null;

        try {
            int tenantId;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            //Get subscriber Id
            Subscriber subscriber = getSubscriber(userId, tenantId, conn);
            if (subscriber == null) {
                String msg = "Could not load Subscriber records for: " + userId;
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //Get API Id
            int apiId=-1;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId==-1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
                throw new APIManagementException(msg);
            }
            ResultSet rs = null;
            boolean userRatingExists=false;
            //This query to check the ratings already exists for the user in the AM_API_RATINGS table
            String sqlQuery = "SELECT " +
                              "RATING FROM AM_API_RATINGS " +
                              " WHERE API_ID= ? AND SUBSCRIBER_ID=? ";

            psSelect = conn.prepareStatement(sqlQuery);
            psSelect.setInt(1, apiId);
            psSelect.setInt(2, subscriber.getId());
            rs = psSelect.executeQuery();

            while (rs.next()) {
                userRatingExists = true;
            }
            psSelect.close();
            String sqlAddQuery;
            if (!userRatingExists) {
                //This query to update the AM_API_RATINGS table
                sqlAddQuery = "INSERT " +
                              "INTO AM_API_RATINGS (RATING,API_ID, SUBSCRIBER_ID)" +
                              " VALUES (?,?,?)";

            } else {
                //This query to insert into the AM_API_RATINGS table
                sqlAddQuery = "UPDATE " +
                              "AM_API_RATINGS SET RATING=? " +
                              "WHERE API_ID= ? AND SUBSCRIBER_ID=?";
            }
            // Adding data to the AM_API_RATINGS  table
            ps = conn.prepareStatement(sqlAddQuery);
            ps.setInt(1, rating);
            ps.setInt(2, apiId);
            ps.setInt(3, subscriber.getId());
            ps.executeUpdate();
            ps.close();


        } catch (SQLException e) {
            handleException("Failed to add API rating of the user:"+userId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }
    }

    public void removeAPIRating(APIIdentifier apiId, String user)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            removeAPIRating(apiId, user, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add Application ", e);
                }
            }
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    /**
     * @param apiIdentifier API Identifier
     * @param userId        User Id
     * @throws APIManagementException if failed to add Application
     */
    public void removeAPIRating(APIIdentifier apiIdentifier, String userId, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        PreparedStatement psSelect = null;

        try {
            int tenantId;
            int rateId = -1;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            //Get subscriber Id
            Subscriber subscriber = getSubscriber(userId, tenantId, conn);
            if (subscriber == null) {
                String msg = "Could not load Subscriber records for: " + userId;
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //Get API Id
            int apiId = -1;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId == -1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
                throw new APIManagementException(msg);
            }
            ResultSet rs;
            //This query to check the ratings already exists for the user in the AM_API_RATINGS table
            String sqlQuery = "SELECT " +
                              "RATING_ID FROM AM_API_RATINGS " +
                              " WHERE API_ID= ? AND SUBSCRIBER_ID=? ";

            psSelect = conn.prepareStatement(sqlQuery);
            psSelect.setInt(1, apiId);
            psSelect.setInt(2, subscriber.getId());
            rs = psSelect.executeQuery();

            while (rs.next()) {
                rateId = rs.getInt("RATING_ID");
            }
            psSelect.close();
            String sqlAddQuery;
            if (rateId != -1) {
                //This query to delete the specific rate row from the AM_API_RATINGS table
                sqlAddQuery = "DELETE " +
                              "FROM AM_API_RATINGS" +
                              " WHERE RATING_ID =? ";
                // Adding data to the AM_API_RATINGS  table
                ps = conn.prepareStatement(sqlAddQuery);
                ps.setInt(1, rateId);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            handleException("Failed to delete API rating", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }
    }

    public int getUserRating(APIIdentifier apiId, String user)
            throws APIManagementException {
        Connection conn = null;
        int userRating=0;
        try {
            conn = APIMgtDBUtil.getConnection();
            userRating = getUserRating(apiId, user, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback getting user ratings ", e);
                }
            }
            handleException("Failed to get user ratings", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
        return userRating;
    }

    /**
     * @param apiIdentifier API Identifier
     * @param userId        User Id
     * @throws APIManagementException if failed to add Application
     */
    public int getUserRating(APIIdentifier apiIdentifier, String userId, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        int userRating=0;
        try {
            int tenantId;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            //Get subscriber Id
            Subscriber subscriber = getSubscriber(userId, tenantId, conn);
            if (subscriber == null) {
                String msg = "Could not load Subscriber records for: " + userId;
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //Get API Id
            int apiId = -1;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId == -1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //This query to update the AM_API_RATINGS table
            String sqlQuery = "SELECT RATING" +
                              " FROM AM_API_RATINGS " +
                              " WHERE SUBSCRIBER_ID  = ? AND API_ID= ? ";
            // Adding data to the AM_API_RATINGS  table
            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, subscriber.getId());
            ps.setInt(2, apiId);
            rs = ps.executeQuery();

            while (rs.next()) {
                userRating = rs.getInt("RATING");
            }
            ps.close();

        } catch (SQLException e) {
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }
        return userRating;
    }

    public static float getAverageRating(APIIdentifier apiId)
            throws APIManagementException {
        Connection conn = null;
        float avrRating = 0;
        try {
            conn = APIMgtDBUtil.getConnection();
            avrRating = getAverageRating(apiId, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback getting user ratings ", e);
                }
            }
            handleException("Failed to get user ratings", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
        return avrRating;
    }

    /**
     * @param apiIdentifier API Identifier
     * @param userId        User Id
     * @throws APIManagementException if failed to add Application
     */
    public static float getAverageRating(APIIdentifier apiIdentifier, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        float avrRating = 0;
        try {
            //Get API Id
            int apiId = -1;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId == -1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //This query to update the AM_API_RATINGS table
            String sqlQuery = "SELECT CAST( SUM(RATING) AS DECIMAL)/COUNT(RATING) AS RATING " +
                              " FROM AM_API_RATINGS" +
                              " WHERE API_ID =? GROUP BY API_ID ";

            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            rs = ps.executeQuery();

            while (rs.next()) {
                avrRating = rs.getFloat("RATING");
            }
            ps.close();

        } catch (SQLException e) {
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }

        BigDecimal decimal = new BigDecimal(avrRating);
        return Float.valueOf(decimal.setScale(1, BigDecimal.ROUND_UP).toString());
    }

    /**
     * @param application Application
     * @param userId      User Id
     * @throws APIManagementException if failed to add Application
     */
    public int addApplication(Application application, String userId, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        int applicationId = 0;
        try {
            int tenantId;
           
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            //Get subscriber Id
            Subscriber subscriber = getSubscriber(userId, tenantId, conn);
            if (subscriber == null) {
                String msg = "Could not load Subscriber records for: " + userId;
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //This query to update the AM_APPLICATION table
            String sqlQuery = "INSERT " +
                              "INTO AM_APPLICATION (NAME, SUBSCRIBER_ID, APPLICATION_TIER, CALLBACK_URL, DESCRIPTION, APPLICATION_STATUS)" +
                              " VALUES (?,?,?,?,?,?)";
            // Adding data to the AM_APPLICATION  table
            //ps = conn.prepareStatement(sqlQuery);
            ps = conn.prepareStatement(sqlQuery, new String[]{"APPLICATION_ID"});
            if (conn.getMetaData().getDriverName().contains("PostgreSQL")) {
                ps = conn.prepareStatement(sqlQuery, new String[]{"application_id"});
            }
            
            ps.setString(1, application.getName());
            ps.setInt(2, subscriber.getId());
            ps.setString(3, application.getTier());
            ps.setString(4, application.getCallbackUrl());
            ps.setString(5, application.getDescription());
            
            if(application.getName() == APIConstants.DEFAULT_APPLICATION_NAME){
            	 ps.setString(6, APIConstants.ApplicationStatus.APPLICATION_APPROVED);
            }else{
            	ps.setString(6, APIConstants.ApplicationStatus.APPLICATION_CREATED);
            }
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {              
                applicationId = Integer.valueOf(rs.getString(1)).intValue();
            }
            
            ps.close();         
        } catch (SQLException e) {
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }
		return applicationId;
		
    }

    public void updateApplication(Application application) throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        try {
            conn = APIMgtDBUtil.getConnection();

            //This query to update the AM_APPLICATION table
            String sqlQuery = "UPDATE " +
                              "AM_APPLICATION" +
                              " SET NAME = ? " +
                              ", APPLICATION_TIER = ? " +
                              ", CALLBACK_URL = ? " +
                              ", DESCRIPTION = ? " +
                              "WHERE" +
                              " APPLICATION_ID = ?";
            // Adding data to the AM_APPLICATION  table
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, application.getName());
            ps.setString(2, application.getTier());
            ps.setString(3, application.getCallbackUrl());
            ps.setString(4, application.getDescription());
            ps.setInt(5, application.getId());

            ps.executeUpdate();
            ps.close();
            // finally commit transaction
            conn.commit();

            updateOAuthConsumerApp(application.getName(), application.getCallbackUrl());

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the update Application ", e);
                }
            }
            handleException("Failed to update Application", e);
        } catch (IdentityOAuthAdminException e) {
            handleException("Failed to update OAuth Consumer Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    /**
     * Update the status of the Application creation process
     * @param applicationId
     * @param status
     * @throws APIManagementException
     */
    public void updateApplicationStatus(int applicationId, String status) throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        try {
            conn = APIMgtDBUtil.getConnection();
           
            String updateSqlQuery = "UPDATE " +
                              " AM_APPLICATION" +
                              " SET APPLICATION_STATUS = ? " +
                              "WHERE" +
                              " APPLICATION_ID = ?";
         
            ps = conn.prepareStatement(updateSqlQuery);
            ps.setString(1, status);            
            ps.setInt(2, applicationId);

            ps.executeUpdate();
            ps.close();
         
            conn.commit();        

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the update Application ", e);
                }
            }
            handleException("Failed to update Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

	/**
	 * get the status of the Application creation process
	 * 
	 * @param applicationId
	 * @return
	 * @throws APIManagementException
	 */
	public String getApplicationStatus(String appName,String userId) throws APIManagementException {
		Connection conn = null;
		ResultSet resultSet = null;
		PreparedStatement ps = null;
		String status = null;
        int applicationId=getApplicationId(appName,userId);
		try {
			conn = APIMgtDBUtil.getConnection();
			String sqlQuery = "SELECT   APPLICATION_STATUS FROM   AM_APPLICATION " + "WHERE "
			                          + "   APPLICATION_ID= ?";

			ps = conn.prepareStatement(sqlQuery);
			ps.setInt(1, applicationId);
			resultSet = ps.executeQuery();
			while (resultSet.next()) {
				status = resultSet.getString("APPLICATION_STATUS");
			}
			ps.close();
			conn.commit();
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					log.error("Failed to rollback the update Application ", e);
				}
			}
			handleException("Failed to update Application", e);
		} finally {
			APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
		}
		return status;
	}

    /**
     * @param username Subscriber
     * @return ApplicationId for given appname.
     * @throws APIManagementException if failed to get Applications for given subscriber.
     */
    public int getApplicationId(String appName, String username) throws APIManagementException {
        if (username == null) {
            return 0;
        }
        Subscriber subscriber = getSubscriber(username);

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int appId = 0;

        String sqlQuery = "SELECT " +
                          "   APPLICATION_ID " +

                          "FROM " +
                          "   AM_APPLICATION " +
                          "WHERE " +
                          "   SUBSCRIBER_ID  = ? AND  NAME= ?";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setInt(1, subscriber.getId());
            prepStmt.setString(2, appName);
            rs = prepStmt.executeQuery();


            while (rs.next()) {
                appId = rs.getInt("APPLICATION_ID");
            }

        } catch (SQLException e) {
            handleException("Error when getting the application id from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return appId;
    }

    /**
     * Find the name of the application by Id
     * @param applicationId - applicatoin id
     * @return - application name
     * @throws APIManagementException
     */
    public String getApplicationNameFromId(int applicationId) throws APIManagementException{

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String appName = null;

        String sqlQuery = "SELECT NAME " +
                          "FROM AM_APPLICATION " +
                          "WHERE " +
                          "APPLICATION_ID = ?";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setInt(1, applicationId);
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                appName = rs.getString("NAME");
            }

        } catch (SQLException e) {
            handleException("Error when getting the application name for id " + applicationId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return appName;
    }

    /**
     * @param subscriber Subscriber
     * @return Applications for given subscriber.
     * @throws APIManagementException if failed to get Applications for given subscriber.
     */
    public Application[] getApplications(Subscriber subscriber) throws APIManagementException {
        if (subscriber == null) {
            return null;
        }
        //if subscribed user used email find the ordinal  username
        String subscribedUser = getLoginUserName(subscriber.getName());
        subscriber.setName(subscribedUser);

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Application[] applications = null;

        String sqlQuery = "SELECT " +
                          "   APPLICATION_ID " +
                          "   ,NAME" +
                          "   ,APPLICATION_TIER" +
                          "   ,SUBSCRIBER_ID  " +
                          "   ,CALLBACK_URL  " +
                          "   ,DESCRIPTION  " +
                          "   ,APPLICATION_STATUS  " +
                          "FROM " +
                          "   AM_APPLICATION " +
                          "WHERE " +
                          "   SUBSCRIBER_ID  = ?";

        try {
            int tenantId;
            connection = APIMgtDBUtil.getConnection();
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(subscriber.getName());
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + subscriber.getName();
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }

            //getSubscriberId
            if (subscriber.getId() == 0) {
                Subscriber subs;
                subs = getSubscriber(subscriber.getName(), tenantId, connection);
                if (subs == null) {
                    return null;
                } else {
                    subscriber = subs;
                }
            }

            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setInt(1, subscriber.getId());
            rs = prepStmt.executeQuery();

            ArrayList<Application> applicationsList = new ArrayList<Application>();
            //  String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(subscriber.getName());
            String tenantAwareUserId = subscriber.getName();
            Application application;
            while (rs.next()) {
                application = new Application(rs.getString("NAME"), subscriber);
                application.setId(rs.getInt("APPLICATION_ID"));
                application.setTier(rs.getString("APPLICATION_TIER"));
                application.setCallbackUrl(rs.getString("CALLBACK_URL"));
                application.setDescription(rs.getString("DESCRIPTION"));
                application.setStatus(rs.getString("APPLICATION_STATUS"));
                Set<APIKey> keys = getApplicationKeys(tenantAwareUserId, rs.getInt("APPLICATION_ID"));
                for (APIKey key : keys) {
                    application.addKey(key);
                }
                applicationsList.add(application);

            }
            Collections.sort(applicationsList, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            applications = applicationsList.toArray(new Application[applicationsList.size()]);

        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return applications;
    }

    public void deleteApplication(Application application) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String getSubscriptionsQuery = "SELECT" +
                                       " SUBSCRIPTION_ID " +
                                       "FROM" +
                                       " AM_SUBSCRIPTION " +
                                       "WHERE" +
                                       " APPLICATION_ID = ?";

        String deleteKeyMappingQuery = "DELETE FROM AM_SUBSCRIPTION_KEY_MAPPING WHERE SUBSCRIPTION_ID = ?";
        String deleteSubscriptionsQuery = "DELETE FROM AM_SUBSCRIPTION WHERE APPLICATION_ID = ?";
        String deleteApplicationKeyQuery = "DELETE FROM AM_APPLICATION_KEY_MAPPING WHERE APPLICATION_ID = ?";
        String deleteApplicationQuery = "DELETE FROM AM_APPLICATION WHERE APPLICATION_ID = ?";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(getSubscriptionsQuery);
            prepStmt.setInt(1, application.getId());
            rs = prepStmt.executeQuery();

            List<Integer> subscriptions = new ArrayList<Integer>();
            while (rs.next()) {
                subscriptions.add(rs.getInt("SUBSCRIPTION_ID"));
            }
            prepStmt.close();
            rs.close();

            prepStmt = connection.prepareStatement(deleteKeyMappingQuery);
            for (Integer subscriptionId : subscriptions) {
                prepStmt.setInt(1, subscriptionId);
                prepStmt.execute();
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteSubscriptionsQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteApplicationKeyQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteApplicationQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();

            connection.commit();
        } catch (SQLException e) {
            handleException("Error while removing application details from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }


    /**
     * returns a subscriber record for given username,tenant Id
     *
     * @param username   UserName
     * @param tenantId   Tenant Id
     * @param connection
     * @return Subscriber
     * @throws APIManagementException if failed to get subscriber
     */
    private Subscriber getSubscriber(String username, int tenantId, Connection connection)
            throws APIManagementException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Subscriber subscriber = null;
        String sqlQuery = "SELECT " +
                          "   SUB.SUBSCRIBER_ID AS SUBSCRIBER_ID" +
                          "   ,SUB.USER_ID AS USER_ID " +
                          "   ,SUB.TENANT_ID AS TENANT_ID" +
                          "   ,SUB.EMAIL_ADDRESS AS EMAIL_ADDRESS" +
                          "   ,SUB.DATE_SUBSCRIBED AS DATE_SUBSCRIBED " +
                          "FROM " +
                          "   AM_SUBSCRIBER SUB " +
                          "WHERE " +
                          "SUB.USER_ID = ? " +
                          "AND SUB.TENANT_ID = ?";


        try {
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, username);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                subscriber = new Subscriber(rs.getString("USER_ID"));
                subscriber.setEmail(rs.getString("EMAIL_ADDRESS"));
                subscriber.setId(rs.getInt("SUBSCRIBER_ID"));
                subscriber.setSubscribedDate(rs.getDate("DATE_SUBSCRIBED"));
                subscriber.setTenantId(rs.getInt("TENANT_ID"));
                return subscriber;
            }
        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, null, rs);
        }
        return subscriber;
    }

    public void recordAPILifeCycleEvent(APIIdentifier identifier, APIStatus oldStatus,
                                        APIStatus newStatus, String userId)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            recordAPILifeCycleEvent(identifier, oldStatus, newStatus, userId, conn);
        } catch (SQLException e) {
            handleException("Failed to record API state change", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    public void recordAPILifeCycleEvent(APIIdentifier identifier, APIStatus oldStatus,
                                        APIStatus newStatus, String userId, Connection conn)
            throws APIManagementException {
        //Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        int tenantId;
        int apiId = -1;
        try {
            tenantId = IdentityUtil.getTenantIdOFUser(userId);
        } catch (IdentityException e) {
            String msg = "Failed to get tenant id of user : " + userId;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }

        if (oldStatus == null && !newStatus.equals(APIStatus.CREATED)) {
            String msg = "Invalid old and new state combination";
            log.error(msg);
            throw new APIManagementException(msg);
        } else if (oldStatus != null && oldStatus.equals(newStatus)) {
            String msg = "No measurable differences in API state";
            log.error(msg);
            throw new APIManagementException(msg);
        }

        String getAPIQuery = "SELECT " +
                             "API.API_ID FROM AM_API API" +
                             " WHERE " +
                             "API.API_PROVIDER = ?" +
                             " AND API.API_NAME = ?" +
                             " AND API.API_VERSION = ?";

        String sqlQuery = "INSERT " +
                          "INTO AM_API_LC_EVENT (API_ID, PREVIOUS_STATE, NEW_STATE, USER_ID, TENANT_ID, EVENT_DATE)" +
                          " VALUES (?,?,?,?,?,?)";

        try {
            //conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(getAPIQuery);
            ps.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();
            if (apiId == -1) {
                String msg = "Unable to find the API: " + identifier + " in the database";
                log.error(msg);
                throw new APIManagementException(msg);
            }

            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            if (oldStatus != null) {
                ps.setString(2, oldStatus.getStatus());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, newStatus.getStatus());
            ps.setString(4, userId);
            ps.setInt(5, tenantId);
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the API state change record", e);
                }
            }
            handleException("Failed to record API state change", e);
        } finally {
           // APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    public List<LifeCycleEvent> getLifeCycleEvents(APIIdentifier apiId)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT" +
                          " LC.API_ID AS API_ID," +
                          " LC.PREVIOUS_STATE AS PREVIOUS_STATE," +
                          " LC.NEW_STATE AS NEW_STATE," +
                          " LC.USER_ID AS USER_ID," +
                          " LC.EVENT_DATE AS EVENT_DATE " +
                          "FROM" +
                          " AM_API_LC_EVENT LC, " +
                          " AM_API API " +
                          "WHERE" +
                          " API.API_PROVIDER = ?" +
                          " AND API.API_NAME = ?" +
                          " AND API.API_VERSION = ?" +
                          " AND API.API_ID = LC.API_ID";

        List<LifeCycleEvent> events = new ArrayList<LifeCycleEvent>();

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(apiId.getProviderName()));
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                LifeCycleEvent event = new LifeCycleEvent();
                event.setApi(apiId);
                String oldState = rs.getString("PREVIOUS_STATE");
                event.setOldStatus(oldState != null ? APIStatus.valueOf(oldState) : null);
                event.setNewStatus(APIStatus.valueOf(rs.getString("NEW_STATE")));
                event.setUserId(rs.getString("USER_ID"));
                event.setDate(rs.getTimestamp("EVENT_DATE"));
                events.add(event);
            }

            Collections.sort(events, new Comparator<LifeCycleEvent>() {
                public int compare(LifeCycleEvent o1, LifeCycleEvent o2) {
                    return o1.getDate().compareTo(o2.getDate());
                }
            });
        } catch (SQLException e) {
            handleException("Error when executing the SQL : " + sqlQuery, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return events;
    }

    public void makeKeysForwardCompatible(String provider, String apiName, String oldVersion,
                                          String newVersion, String context)
            throws APIManagementException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String getSubscriptionDataQuery = "SELECT" +
                                          " SUB.SUBSCRIPTION_ID AS SUBSCRIPTION_ID," +
                                          " SUB.TIER_ID AS TIER_ID," +
                                          " SUB.APPLICATION_ID AS APPLICATION_ID," +
                                          " API.CONTEXT AS CONTEXT," +
                                          " SKM.ACCESS_TOKEN AS ACCESS_TOKEN," +
                                          " SKM.KEY_TYPE AS KEY_TYPE " +
                                          "FROM" +
                                          " AM_SUBSCRIPTION SUB," +
                                          " AM_SUBSCRIPTION_KEY_MAPPING SKM, " +
                                          " AM_API API " +
                                          "WHERE" +
                                          " API.API_PROVIDER = ?" +
                                          " AND API.API_NAME = ?" +
                                          " AND API.API_VERSION = ?" +
                                          " AND SKM.SUBSCRIPTION_ID = SUB.SUBSCRIPTION_ID" +
                                          " AND API.API_ID = SUB.API_ID";

        String addSubKeyMapping = "INSERT INTO" +
                                  " AM_SUBSCRIPTION_KEY_MAPPING (SUBSCRIPTION_ID, ACCESS_TOKEN, KEY_TYPE)" +
                                  " VALUES (?,?,?)";

        String getApplicationDataQuery = "SELECT" +
                                         " SUB.SUBSCRIPTION_ID AS SUBSCRIPTION_ID," +
                                         " SUB.TIER_ID AS TIER_ID," +
                                         " APP.APPLICATION_ID AS APPLICATION_ID," +
                                         " API.CONTEXT AS CONTEXT " +
                                         "FROM" +
                                         " AM_SUBSCRIPTION SUB," +
                                         " AM_APPLICATION APP," +
                                         " AM_API API " +
                                         "WHERE" +
                                         " API.API_PROVIDER = ?" +
                                         " AND API.API_NAME = ?" +
                                         " AND API.API_VERSION = ?" +
                                         " AND SUB.APPLICATION_ID = APP.APPLICATION_ID" +
                                         " AND API.API_ID = SUB.API_ID";

        try {
            // Retrieve all the existing subscription for the old version
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(getSubscriptionDataQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(provider));
            prepStmt.setString(2, apiName);
            prepStmt.setString(3, oldVersion);
            rs = prepStmt.executeQuery();

            List<SubscriptionInfo> subscriptionData = new ArrayList<SubscriptionInfo>();
            Set<Integer> subscribedApplications = new HashSet<Integer>();
            while (rs.next()) {
                SubscriptionInfo info = new SubscriptionInfo();
                info.subscriptionId = rs.getInt("SUBSCRIPTION_ID");
                info.tierId = rs.getString("TIER_ID");
                info.context = rs.getString("CONTEXT");
                info.applicationId = rs.getInt("APPLICATION_ID");
                info.accessToken = rs.getString("ACCESS_TOKEN");  // no decryption needed.
                info.tokenType = rs.getString("KEY_TYPE");
                subscriptionData.add(info);
            }
            prepStmt.close();
            rs.close();

            Map<Integer, Integer> subscriptionIdMap = new HashMap<Integer, Integer>();
            APIIdentifier apiId = new APIIdentifier(provider, apiName, newVersion);
            for (SubscriptionInfo info : subscriptionData) {
                if (!subscriptionIdMap.containsKey(info.subscriptionId)) {
                    apiId.setTier(info.tierId);
                    int subscriptionId = addSubscription(apiId, context, info.applicationId, APIConstants.SubscriptionStatus.UNBLOCKED);
                    if (subscriptionId == -1) {
                        String msg = "Unable to add a new subscription for the API: " + apiName +
                                     ":v" + newVersion;
                        log.error(msg);
                        throw new APIManagementException(msg);
                    }
                    subscriptionIdMap.put(info.subscriptionId, subscriptionId);
                }

                int subscriptionId = subscriptionIdMap.get(info.subscriptionId);
                prepStmt = connection.prepareStatement(addSubKeyMapping);
                prepStmt.setInt(1, subscriptionId);
                prepStmt.setString(2, info.accessToken);
                prepStmt.setString(3, info.tokenType);
                prepStmt.execute();
                prepStmt.close();

                subscribedApplications.add(info.applicationId);
            }

            prepStmt = connection.prepareStatement(getApplicationDataQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(provider));
            prepStmt.setString(2, apiName);
            prepStmt.setString(3, oldVersion);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                int applicationId = rs.getInt("APPLICATION_ID");
                if (!subscribedApplications.contains(applicationId)) {
                    apiId.setTier(rs.getString("TIER_ID"));
                    addSubscription(apiId, rs.getString("CONTEXT"), applicationId, APIConstants.SubscriptionStatus.UNBLOCKED);
                }
            }

            connection.commit();
        } catch (SQLException e) {
            handleException("Error when executing the SQL queries", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    public void addAPI(API api) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "INSERT INTO AM_API (API_PROVIDER, API_NAME, API_VERSION, CONTEXT) " +
                       "VALUES (?,?,?,?)";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query, new String[]{"api_id"});
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(api.getId().getProviderName()));
            prepStmt.setString(2, api.getId().getApiName());
            prepStmt.setString(3, api.getId().getVersion());
            prepStmt.setString(4, api.getContext());
            prepStmt.execute();


            rs = prepStmt.getGeneratedKeys();
            int applicationId = -1;
            if (rs.next()) {
                applicationId = rs.getInt(1);
            }
            addURLTemplates(applicationId, api, connection);
            recordAPILifeCycleEvent(api.getId(), null, APIStatus.CREATED, APIUtil.replaceEmailDomainBack(api.getId().getProviderName()), connection);
            connection.commit();
        } catch (SQLException e) {
            handleException("Error while adding the API: " + api.getId() + " to the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    /**
     * Persists WorkflowDTO to Database
     * @param workflow
     * @throws APIManagementException
     */
    public void addWorkflowEntry(WorkflowDTO workflow) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "INSERT INTO AM_WORKFLOWS (WF_REFERENCE, WF_TYPE, WF_STATUS, WF_CREATED_TIME, " +
                "WF_STATUS_DESC, TENANT_ID, TENANT_DOMAIN, WF_EXTERNAL_REFERENCE ) VALUES (?,?,?,?,?,?,?,?)";
        try {
            Timestamp cratedDateStamp = new Timestamp(workflow.getCreatedTime());

            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflow.getWorkflowReference());
            prepStmt.setString(2, workflow.getWorkflowType());
            prepStmt.setString(3, workflow.getStatus().toString());
            prepStmt.setTimestamp(4, cratedDateStamp);
            prepStmt.setString(5, workflow.getWorkflowDescription());
            prepStmt.setInt(6, workflow.getTenantId());
            prepStmt.setString(7, workflow.getTenantDomain());
            prepStmt.setString(8, workflow.getExternalWorkflowReference());

            prepStmt.execute();

            connection.commit();
        } catch (SQLException e) {
            handleException("Error while adding Workflow : " + workflow.getExternalWorkflowReference() + " to the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    public void updateWorkflowStatus(WorkflowDTO workflowDTO) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "UPDATE AM_WORKFLOWS SET WF_STATUS = ?, WF_STATUS_DESC = ?, WF_UPDATED_TIME = ? " +
                       "WHERE WF_EXTERNAL_REFERENCE = ?";
        try {
            Timestamp updatedTimeStamp = new Timestamp(workflowDTO.getUpdatedTime());

            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowDTO.getStatus().toString());
            prepStmt.setString(2, workflowDTO.getWorkflowDescription());
            prepStmt.setTimestamp(3, updatedTimeStamp);
            prepStmt.setString(4, workflowDTO.getExternalWorkflowReference());

            prepStmt.execute();

            connection.commit();
        }catch (SQLException e) {
            handleException("Error while updating Workflow Status of workflow " +
                    workflowDTO.getExternalWorkflowReference(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    /**
     * Returns a workflow object for a given external workflow reference.
     * @param workflowReference
     * @return
     * @throws APIManagementException
     */
    public WorkflowDTO retrieveWorkflow(String workflowReference) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        WorkflowDTO workflowDTO = null;

        String query = "SELECT * FROM AM_WORKFLOWS WHERE WF_EXTERNAL_REFERENCE=?";
        try {

            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowReference);

            rs = prepStmt.executeQuery();

            while (rs.next()) {
                workflowDTO = new WorkflowDTO();
                workflowDTO.setStatus(WorkflowStatus.valueOf(rs.getString("WF_STATUS")));
                workflowDTO.setExternalWorkflowReference(rs.getString("WF_EXTERNAL_REFERENCE"));
                workflowDTO.setCreatedTime(rs.getTimestamp("WF_CREATED_TIME").getTime());
                workflowDTO.setWorkflowReference(rs.getString("WF_REFERENCE"));
                workflowDTO.setTenantDomain(rs.getString("TENANT_DOMAIN"));
                workflowDTO.setTenantId(rs.getInt("TENANT_ID"));
                workflowDTO.setWorkflowType(rs.getString("WF_TYPE"));
                workflowDTO.setWorkflowDescription(rs.getString("WF_STATUS_DESC"));
            }

        }catch (SQLException e) {
            handleException("Error while retrieving workflow details for " +
                    workflowReference, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }

        return workflowDTO;
    }


    /**
     * Adds URI templates define for an API
     *
     * @param apiId
     * @param api
     * @param connection
     * @throws APIManagementException
     */
    public void addURLTemplates(int apiId, API api, Connection connection) throws APIManagementException {
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmt = null;

        String query = "INSERT INTO AM_API_URL_MAPPING (API_ID,HTTP_METHOD,AUTH_SCHEME,URL_PATTERN,THROTTLING_TIER) VALUES (?,?,?,?,?)";
        try {
            //connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);

            Iterator<URITemplate> uriTemplateIterator = api.getUriTemplates().iterator();
            URITemplate uriTemplate;
            for (; uriTemplateIterator.hasNext(); ) {
                uriTemplate = uriTemplateIterator.next();
                prepStmt.setInt(1, apiId);
                prepStmt.setString(2, uriTemplate.getHTTPVerb());
                prepStmt.setString(3, uriTemplate.getAuthType());
                prepStmt.setString(4, uriTemplate.getUriTemplate());
                prepStmt.setString(5, uriTemplate.getThrottlingTier());
                prepStmt.addBatch();
            }
            prepStmt.executeBatch();
            prepStmt.clearBatch();

        } catch (SQLException e) {
            handleException("Error while adding URL template(s) to the database for API : " + api.getId().toString(), e);
        }
    }

    /**
     * update URI templates define for an API
     *
     * @param api
     * @param connection
     * @throws APIManagementException
     */
    public void updateURLTemplates(API api, Connection connection)
            throws APIManagementException {
        int apiId = getAPIID(api.getId(),connection);
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmt = null;
        String deleteOldMappingsQuery = "DELETE FROM AM_API_URL_MAPPING WHERE API_ID = ?";
        try {
            prepStmt = connection.prepareStatement(deleteOldMappingsQuery);
            prepStmt.setInt(1,apiId);
            prepStmt.execute();
        } catch (SQLException e) {
            handleException("Error while deleting URL template(s) for API : " + api.getId().toString(), e);
        }
        addURLTemplates(apiId,api,connection);
    }

    /**
     * returns all URL templates define for all active(PUBLISHED) APIs.
     */
    public static ArrayList<URITemplate> getAllURITemplates(String apiContext, String version)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        ArrayList<URITemplate> uriTemplates = new ArrayList<URITemplate>();

        //TODO : FILTER RESULTS ONLY FOR ACTIVE APIs
        String query =
                "SELECT AUM.HTTP_METHOD,AUTH_SCHEME,URL_PATTERN,THROTTLING_TIER FROM AM_API_URL_MAPPING AUM, AM_API API " +
                "WHERE API.CONTEXT= ? " +
                "AND API.API_VERSION = ? " +
                "AND AUM.API_ID = API.API_ID " +
                "ORDER BY " +
                "URL_MAPPING_ID";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, apiContext);
            prepStmt.setString(2, version);

            rs = prepStmt.executeQuery();

            URITemplate uriTemplate;
            while (rs.next()) {
                uriTemplate = new URITemplate();
                uriTemplate.setHTTPVerb(rs.getString("HTTP_METHOD"));
                uriTemplate.setAuthType(rs.getString("AUTH_SCHEME"));
                uriTemplate.setUriTemplate(rs.getString("URL_PATTERN"));
                uriTemplate.setThrottlingTier(rs.getString("THROTTLING_TIER"));
                uriTemplates.add(uriTemplate);
            }
        } catch (SQLException e) {
            handleException("Error while fetching all URL Templates", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return uriTemplates;
    }


    public void updateAPI(API api) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "UPDATE AM_API SET CONTEXT = ? WHERE API_PROVIDER = ? AND API_NAME = ? AND" +
                       " API_VERSION = ? ";
        try {
            connection = APIMgtDBUtil.getConnection();
            if(api.isApiHeaderChanged()){
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, api.getContext());
                prepStmt.setString(2, APIUtil.replaceEmailDomainBack(api.getId().getProviderName()));
                prepStmt.setString(3, api.getId().getApiName());
                prepStmt.setString(4, api.getId().getVersion());
                prepStmt.execute();
            }
            updateURLTemplates(api, connection);
            connection.commit();

        } catch (SQLException e) {
            handleException("Error while updating the API: " + api.getId() + " in the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    public static int getAPIID(APIIdentifier apiId, Connection connection) throws APIManagementException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;
        String getAPIQuery = "SELECT " +
                             "API.API_ID FROM AM_API API" +
                             " WHERE " +
                             "API.API_PROVIDER = ?" +
                             " AND API.API_NAME = ?" +
                             " AND API.API_VERSION = ?";

        try {
            prepStmt = connection.prepareStatement(getAPIQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(apiId.getProviderName()));
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("API_ID");
            }
            if (id == -1) {
                String msg = "Unable to find the API: " + apiId + " in the database";
                log.error(msg);
                throw new APIManagementException(msg);
            }
        } catch (SQLException e) {
            handleException("Error while locating API: " + apiId + " from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, null, rs);
        }
        return id;
    }



    public void deleteAPI(APIIdentifier apiId) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;

        String deleteLCEventQuery = "DELETE FROM AM_API_LC_EVENT WHERE API_ID=? ";
        String deleteCommentQuery = "DELETE FROM AM_API_COMMENTS WHERE API_ID=? ";
        String deleteRatingsQuery = "DELETE FROM AM_API_RATINGS WHERE API_ID=? ";
        String deleteSubscriptionQuery = "DELETE FROM AM_SUBSCRIPTION WHERE API_ID=?";
        String deleteExternalAPIStoresQuery = "DELETE FROM AM_EXTERNAL_STORES WHERE API_ID=?";
        String deleteAPIQuery = "DELETE FROM AM_API WHERE API_PROVIDER=? AND API_NAME=? AND API_VERSION=? ";

        try {
            connection = APIMgtDBUtil.getConnection();
            id = getAPIID(apiId,connection);
            prepStmt = connection.prepareStatement(deleteSubscriptionQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();
            //Delete all comments associated with given API
            prepStmt = connection.prepareStatement(deleteCommentQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();

            prepStmt = connection.prepareStatement(deleteRatingsQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();

            prepStmt = connection.prepareStatement(deleteLCEventQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();
            //Delete all external APIStore details associated with a given API
            prepStmt = connection.prepareStatement(deleteExternalAPIStoresQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();

            prepStmt = connection.prepareStatement(deleteAPIQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(apiId.getProviderName()));
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            prepStmt.execute();

            connection.commit();

        } catch (SQLException e) {
            handleException("Error while removing the API: " + apiId + " from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }


    /**
     * Change access token status in to revoked in database level.
     *
     * @param key API Key to be revoked
     * @throws APIManagementException on error in revoking access token
     */
    public void revokeAccessToken(String key) throws APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(key);
        }
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "UPDATE " + accessTokenStoreTable + " SET TOKEN_STATE='REVOKED' WHERE ACCESS_TOKEN= ? ";
            ps = conn.prepareStatement(query);
            ps.setString(1, APIUtil.encryptToken(key));
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            handleException("Error in revoking access token: " + e.getMessage(), e);
        } catch (CryptoException e) {
            handleException("Error in revoking access token: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }


    /**
     * Get APIIdentifiers Associated with access token - access token associated with application
     * which has multiple APIs. so this returns all APIs associated with a access token
     *
     * @param accessToken String access token
     * @return APIIdentifier set for all API's associated with given access token
     * @throws APIManagementException error in getting APIIdentifiers
     */
    public Set<APIIdentifier> getAPIByAccessToken(String accessToken)
            throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String getAPISql = "SELECT AMA.API_ID,API_NAME,API_PROVIDER,API_VERSION FROM " +
                           "AM_API AMA," + accessTokenStoreTable +" ACT, AM_APPLICATION_KEY_MAPPING AKM, " +
                           "AM_SUBSCRIPTION AMS WHERE ACT.ACCESS_TOKEN=? " +
                           "AND ACT.CONSUMER_KEY=AKM.CONSUMER_KEY AND AKM.APPLICATION_ID=AMS.APPLICATION_ID AND " +
                           "AMA.API_ID=AMS.API_ID";
        Set<APIIdentifier> apiList = new HashSet<APIIdentifier>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getAPISql);
            nestedPS.setString(1, APIUtil.encryptToken(accessToken));
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiList.add(new APIIdentifier(APIUtil.replaceEmailDomain(nestedRS.getString("API_PROVIDER")),
                                              nestedRS.getString("API_NAME"),
                                              nestedRS.getString("API_VERSION")));
            }
        } catch (SQLException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } catch (CryptoException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiList;
    }


    /**
     * Get all applications associated with given tier
     *
     * @param tier String tier name
     * @return Application object array associated with tier
     * @throws APIManagementException on error in getting applications array
     */
    public Application[] getApplicationsByTier(String tier) throws APIManagementException {
        if (tier == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Application[] applications = null;

        String sqlQuery = "SELECT DISTINCT AMS.APPLICATION_ID,NAME,SUBSCRIBER_ID FROM AM_SUBSCRIPTION AMS,AM_APPLICATION AMA " +
                          "WHERE TIER_ID=? " +
                          "AND AMS.APPLICATION_ID=AMA.APPLICATION_ID";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, tier);
            rs = prepStmt.executeQuery();
            ArrayList<Application> applicationsList = new ArrayList<Application>();
            Application application;
            while (rs.next()) {
                application = new Application(rs.getString("NAME"), getSubscriber(rs.getString("SUBSCRIBER_ID")));
                application.setId(rs.getInt("APPLICATION_ID"));
            }
            Collections.sort(applicationsList, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            applications = applicationsList.toArray(new Application[applicationsList.size()]);

        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return applications;
    }

    private static void handleException(String msg, Throwable t) throws APIManagementException {
        log.error(msg, t);
        throw new APIManagementException(msg, t);
    }

    /**
     * Generates fresh JWT token for given information of validation information
     *
     * @param context         String context for API
     * @param version         version of API
     * @param subscriberName  subscribed user name
     * @param applicationName application name api belongs
     * @param tier            tier name
     * @param endUserName     name of end user
     * @return signed JWT token string
     * @throws APIManagementException error in generating token
     */
    public String createJWTTokenString(String context, String version, String subscriberName,
                                       String applicationName, String tier, String endUserName)
            throws APIManagementException {
        String calleeToken = null;
        APIKeyValidationInfoDTO keyValidationInfoDTO = new APIKeyValidationInfoDTO();
        keyValidationInfoDTO.setSubscriber(subscriberName);
        keyValidationInfoDTO.setApplicationName(applicationName);
        keyValidationInfoDTO.setTier(tier);
        keyValidationInfoDTO.setEndUserName(endUserName);
        if (jwtGenerator != null) {
            calleeToken = jwtGenerator.generateToken(keyValidationInfoDTO, context, version, true);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("JWT generator not properly initialized. JWT token will not present in validation info");
            }
        }
        return calleeToken;
    }

    public static HashMap<String, String> getURITemplatesPerAPIAsString(APIIdentifier identifier)
            throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int apiId = -1;
        HashMap<String, String> urlMappings = new LinkedHashMap<String, String>();
        try {
            conn = APIMgtDBUtil.getConnection();
            apiId = getAPIID(identifier, conn);

            String sqlQuery =
                    "SELECT " +
                    "URL_PATTERN" +
                    ",HTTP_METHOD" +
                    ",AUTH_SCHEME" +
                    ",THROTTLING_TIER " +
                    "FROM " +
                    "AM_API_URL_MAPPING " +
                    "WHERE " +
                    "API_ID = ? " +
                    "ORDER BY " +
                    "URL_MAPPING_ID ASC ";


            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String uriPattern = resultSet.getString("URL_PATTERN");
                String httpMethod = resultSet.getString("HTTP_METHOD");
                String authScheme = resultSet.getString("AUTH_SCHEME");
                String throttlingTier = resultSet.getString("THROTTLING_TIER");
                urlMappings.put(uriPattern + "::" + httpMethod + "::" + authScheme + "::" + throttlingTier, null);
                // urlMappings.put(uriPattern + "::" + httpMethod + "::" + authScheme, null);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return urlMappings;
    }


    public static boolean isDomainRestricted(String apiKey, String clientDomain)
            throws APIManagementException {
        boolean restricted = true;
        if (clientDomain != null) {
            clientDomain = clientDomain.trim();
        }
        List<String> authorizedDomains = Arrays.asList(getAuthorizedDomains(apiKey).split(","));
        if (authorizedDomains.contains("ALL") || authorizedDomains.contains(clientDomain)) {
            restricted = false;
        }
        return restricted;
    }

    public static String getAuthorizedDomains(String accessToken) throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        String authorizedDomains = "";
        String accessAllowDomainsSql = "SELECT a.AUTHZ_DOMAIN " +
                                       " FROM AM_APP_KEY_DOMAIN_MAPPING  a " +
                                       " INNER JOIN " + accessTokenStoreTable + " b " +
                                       " ON a.CONSUMER_KEY = b.CONSUMER_KEY " +
                                       " WHERE b.ACCESS_TOKEN = ? ";

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(accessAllowDomainsSql);
            prepStmt.setString(1, APIUtil.encryptToken(accessToken));
            rs = prepStmt.executeQuery();
            boolean first = true;
            while (rs.next()) {  //if(rs.next==true) -> domain != null
                String domain = rs.getString(1);
                if (first) {
                    authorizedDomains = domain;
                    first = false;
                } else {
                    authorizedDomains = authorizedDomains + "," + domain;
                }
            }
            prepStmt.close();
        } catch (SQLException e) {
            throw new APIManagementException
                    ("Error in retrieving access allowing domain list from table.", e);
        } catch (CryptoException e) {
            throw new APIManagementException
                    ("Error in retrieving access allowing domain list from table.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return authorizedDomains;
    }

    public static String findConsumerKeyFromAccessToken(String accessToken)
            throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
            APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection connection = null;
        PreparedStatement smt = null;
        ResultSet rs = null;
        String consumerKey = null;
        try {
            String getConsumerKeySql = "SELECT CONSUMER_KEY " +
                                       " FROM " + accessTokenStoreTable +
                                       " WHERE ACCESS_TOKEN=?";
            connection = APIMgtDBUtil.getConnection();
            smt = connection.prepareStatement(getConsumerKeySql);
            smt.setString(1, APIUtil.encryptToken(accessToken));
            rs = smt.executeQuery();
            while (rs.next()) {
                consumerKey = rs.getString(1);
            }
            if(consumerKey != null){
                consumerKey = APIUtil.decryptToken(consumerKey);
            }
        } catch (SQLException e) {
            handleException("Error while getting authorized domians.", e);
        } catch (CryptoException e) {
            handleException("Error while getting authorized domians.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(smt, connection, rs);
        }
        return consumerKey;
    }

    /**
     * Adds a comment for an API
     * @param identifier	API Identifier
     * @param commentText	Commented Text
     * @param user			User who did the comment
     * @return 				Comment ID
     */
    public int addComment(APIIdentifier identifier, String commentText, String user)
            throws APIManagementException {

        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement prepStmt = null;
        int commentId = -1;
        int apiId = -1;

        try {
            connection = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            prepStmt = connection.prepareStatement(getApiQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            prepStmt.setString(2, identifier.getApiName());
            prepStmt.setString(3, identifier.getVersion());
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            prepStmt.close();

            if (apiId == -1) {
                String msg = "Unable to get the API ID for: " + identifier;
                log.error(msg);
                throw new APIManagementException(msg);
            }

            /*This query to update the AM_API_COMMENTS table */
            String addCommentQuery = "INSERT " +
                                     "INTO AM_API_COMMENTS (COMMENT_TEXT,COMMENTED_USER,DATE_COMMENTED,API_ID)" +
                                     " VALUES (?,?,?,?)";

            /*Adding data to the AM_API_COMMENTS table*/
            prepStmt = connection.prepareStatement(addCommentQuery, new String[]{"comment_id"});

            prepStmt.setString(1, commentText);
            prepStmt.setString(2, user);
            prepStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()),
                                  Calendar.getInstance());
            prepStmt.setInt(4, apiId);

            prepStmt.executeUpdate();
            ResultSet rs = prepStmt.getGeneratedKeys();
            while (rs.next()) {
                commentId = Integer.valueOf(rs.getString(1)).intValue();
            }
            prepStmt.close();

            /* finally commit transaction */
            connection.commit();

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add comment ", e);
                }
            }
            handleException("Failed to add comment data, for  " + identifier.getApiName() + "-"
                            +identifier.getVersion(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, resultSet);
        }
        return commentId;
    }

    /**
     * Returns all the Comments on an API
     * @param identifier	API Identifier
     * @return				Comment Array
     * @throws APIManagementException
     */
    public Comment[] getComments(APIIdentifier identifier) throws APIManagementException {
        List<Comment> commentList = new ArrayList<Comment>();
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement prepStmt = null;

        String sqlQuery = "SELECT " +
                          "   AM_API_COMMENTS.COMMENT_TEXT AS COMMENT_TEXT," +
                          "   AM_API_COMMENTS.COMMENTED_USER AS COMMENTED_USER," +
                          "   AM_API_COMMENTS.DATE_COMMENTED AS DATE_COMMENTED " +
                          "FROM " +
                          "   AM_API_COMMENTS, " +
                          "   AM_API API " +
                          "WHERE " +
                          "   API.API_PROVIDER = ? " +
                          "   AND API.API_NAME = ? " +
                          "   AND API.API_VERSION  = ? " +
                          "   AND API.API_ID = AM_API_COMMENTS.API_ID";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, APIUtil.replaceEmailDomainBack(identifier.getProviderName()));
            prepStmt.setString(2, identifier.getApiName());
            prepStmt.setString(3, identifier.getVersion());
            resultSet = prepStmt.executeQuery();
            while (resultSet.next()) {
                Comment comment = new Comment();
                comment.setText(resultSet.getString("COMMENT_TEXT"));
                comment.setUser(resultSet.getString("COMMENTED_USER"));
                comment.setCreatedTime(new java.util.Date(resultSet.getTimestamp("DATE_COMMENTED").getTime()));
                commentList.add(comment);
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Failed to retrieve comments ", e);
            }
            handleException("Failed to retrieve comments for  " + identifier.getApiName() + "-"
                            + identifier.getVersion(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, resultSet);
        }
        return commentList.toArray(new Comment[commentList.size()]);
    }

    public static boolean isContextExist(String context) {
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement prepStmt = null;

        String sql = "SELECT CONTEXT FROM AM_API " +
                     " WHERE CONTEXT= ?";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, context);
            resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                if (resultSet.getString(1) != null) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to retrieve the API Context ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, resultSet);
        }
        return false;
    }

    public static List<String> getAllAvailableContexts () {
        List<String> contexts = new ArrayList<String> ();
        Connection connection = null;
        ResultSet resultSet = null;
        PreparedStatement prepStmt = null;

        String sql = "SELECT CONTEXT FROM AM_API ";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sql);
            resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                contexts.add(resultSet.getString("CONTEXT"));
            }
        } catch (SQLException e) {
            log.error("Failed to retrieve the API Context ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, resultSet);
        }
        return contexts;
    }

    private static class SubscriptionInfo {
        private int subscriptionId;
        private String tierId;
        private String context;
        private int applicationId;
        private String accessToken;
        private String tokenType;
    }

    /**
     * Identify whether the loggedin user used his ordinal username or email
     *
     * @param userId
     * @return
     */
    private boolean isUserLoggedInEmail(String userId) {

        if (userId.contains("@")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Identify whether the loggedin user used his Primary Login name or Secondary login name
     *
     * @param userId
     * @return
     */
    private boolean isSecondaryLogin(String userId) {

        Map<String, Map<String, String>> loginConfiguration = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration().getLoginConfiguration();
        if (loginConfiguration.get(EMAIL_LOGIN) != null) {
            Map<String, String> emailConf = loginConfiguration.get(EMAIL_LOGIN);
            if ("true".equalsIgnoreCase(emailConf.get(PRIMARY_LOGIN))) {
                if (isUserLoggedInEmail(userId)) {
                    return false;
                } else {
                    return true;
                }
            }
            if ("false".equalsIgnoreCase(emailConf.get(PRIMARY_LOGIN))) {
                if (isUserLoggedInEmail(userId)) {
                    return true;
                } else {
                    return false;
                }
            }

        }
        if (loginConfiguration.get(USERID_LOGIN) != null) {
            Map<String, String> userIdConf = loginConfiguration
                    .get(USERID_LOGIN);
            if ("true".equalsIgnoreCase(userIdConf.get(PRIMARY_LOGIN))) {
                if (isUserLoggedInEmail(userId)) {
                    return true;
                } else {
                    return false;
                }
            }
            if ("false".equalsIgnoreCase(userIdConf.get(PRIMARY_LOGIN))) {
                if (isUserLoggedInEmail(userId)) {
                    return false;
                } else {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * Get the primaryLogin name using secondary login name. Primary secondary
     * Configuration is provided in the identitiy.xml. In the userstore, it is
     * users responsibility TO MAINTAIN THE SECONDARY LOGIN NAME AS UNIQUE for
     * each and every users. If it is not unique, we will pick the very first
     * entry from the userlist.
     *
     * @param login
     * @return
     */
    private String getPrimaryloginFromSecondary(String login) {
        Map<String, Map<String, String>> loginConfiguration = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration().getLoginConfiguration();
        String claimURI = null, username = null;
        if (isUserLoggedInEmail(login)) {
            Map<String, String> emailConf = loginConfiguration.get(EMAIL_LOGIN);
            claimURI = emailConf.get(CLAIM_URI);
        } else {
            Map<String, String> userIdConf = loginConfiguration
                    .get(USERID_LOGIN);
            claimURI = userIdConf.get(CLAIM_URI);
        }

        try {
            RemoteUserManagerClient rmUserlient = new RemoteUserManagerClient(login);
            String user[] = rmUserlient.getUserList(claimURI, login);
            if (user.length > 0) {
                username = user[0].toString();
            }
        } catch (Exception e) {
            log.error("Error while retriivng the primaryLogin name using seconadry loginanme : "
                      + login, e);
        }
        return username;
    }

    /**
     * identify the login username is primary or secondary
     *
     * @param userID
     * @return
     */
    private String getLoginUserName(String userID) {
        String primaryLogin = userID;
        if (isSecondaryLogin(userID)) {
            primaryLogin = getPrimaryloginFromSecondary(userID);
        }
        return primaryLogin;
    }

    private long getApplicationAccessTokenValidityPeriod() {
        return OAuthServerConfiguration.getInstance().getApplicationAccessTokenValidityPeriodInSeconds();

    }

    /**
     * Store external APIStore details to which APIs successfully published
     * @param apiId APIIdentifier
     * @param apiStoreSet APIStores set
     * @return   added/failed
     * @throws APIManagementException
     */
    public boolean addExternalAPIStoresDetails(APIIdentifier apiId,Set<APIStore> apiStoreSet)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            addExternalAPIStoresDetails(apiId,apiStoreSet, conn);
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback storing external apistore details ", e);
                }
            }
            log.error("Failed to store external apistore details", e);
            return false;
        } catch (APIManagementException e) {
            log.error("Failed to store external apistore details", e);
            return false;
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }

    }

    /**
     * Save external APIStores details to which APIs published
     * @param apiIdentifier API Identifier
     * @throws APIManagementException if failed to add Application
     */
    public void addExternalAPIStoresDetails(APIIdentifier apiIdentifier,
                                            Set<APIStore> apiStoreSet, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps;

        try {
            conn = APIMgtDBUtil.getConnection();
            //This query to add external APIStores to database table
            String sqlQuery = "INSERT" +
                              " INTO AM_EXTERNAL_STORES (API_ID, STORE_ID,STORE_DISPLAY_NAME, STORE_ENDPOINT,STORE_TYPE)" +
                              " VALUES (?,?,?,?,?)";

            ps = conn.prepareStatement(sqlQuery);
            //Get API Id
            int apiId;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId==-1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
            }

            Iterator it = apiStoreSet.iterator();
            while (it.hasNext()) {
                Object storeObject = it.next();
                APIStore store = (APIStore) storeObject;
                ps.setInt(1, apiId);
                ps.setString(2, store.getName());
                ps.setString(3, store.getDisplayName());
                ps.setString(4, store.getEndpoint());
                ps.setString(5, store.getType());
                ps.addBatch();
            }

            ps.executeBatch();
            ps.clearBatch();


        } catch (SQLException e) {
            log.error("Error while adding External APIStore details to the database for API : ", e);

        }

    }

    public void updateExternalAPIStoresDetails(APIIdentifier apiId,Set<APIStore> apiStoreSet)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            updateExternalAPIStoresDetails(apiId,apiStoreSet, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback updating external apistore details ", e);
                }
            }
            log.error("Failed to update external apistore details", e);

        } catch (APIManagementException e) {
            log.error("Failed to updating external apistore details", e);

        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }

    }

    /**
     * Updateexternal APIStores details to which APIs published
     * @param apiIdentifier API Identifier
     * @throws APIManagementException if failed to add Application
     */
    public void updateExternalAPIStoresDetails(APIIdentifier apiIdentifier,
                                               Set<APIStore> apiStoreSet, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps;

        try {
            conn = APIMgtDBUtil.getConnection();
            //This query to add external APIStores to database table
            String sqlQuery = "UPDATE " +
                              "AM_EXTERNAL_STORES"  +
                              " SET " +
                              "   STORE_ENDPOINT = ? " +
                              "   ,STORE_TYPE = ? " +
                              "WHERE " +
                              "   API_ID = ? AND STORE_ID=?";



            ps = conn.prepareStatement(sqlQuery);
            //Get API Id
            int apiId;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId==-1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
            }

            Iterator it = apiStoreSet.iterator();
            while (it.hasNext()) {
                Object storeObject = it.next();
                APIStore store = (APIStore) storeObject;
                ps.setString(1, store.getEndpoint());
                ps.setString(2, store.getType());
                ps.setInt(3, apiId);
                ps.setString(4, store.getName());
                ps.addBatch();
            }

            ps.executeBatch();
            ps.clearBatch();


        } catch (SQLException e) {
            log.error("Error while updating External APIStore details to the database for API : ", e);

        }

    }

    /**
     * Return external APIStore details on successfully APIs published
     * @param apiId  APIIdentifier
     * @return  Set of APIStore
     * @throws APIManagementException
     */
    public Set<APIStore> getExternalAPIStoresDetails(APIIdentifier apiId)
            throws APIManagementException {
        Connection conn = null;
        Set<APIStore> storesSet = new HashSet<APIStore>();
        try {
            conn = APIMgtDBUtil.getConnection();
            storesSet=getExternalAPIStoresDetails(apiId, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback getting external apistore details ", e);
                }
            }
            log.error("Failed to get external apistore details", e);
        } catch (APIManagementException e) {
            log.error("Failed to get external apistore details", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
        return storesSet;

    }

    /**
     * Get external APIStores details which are stored in database
     *
     * @param apiIdentifier API Identifier
     * @throws APIManagementException if failed to get external APIStores
     */
    public Set<APIStore> getExternalAPIStoresDetails(APIIdentifier apiIdentifier
            , Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Set<APIStore> storesSet = new HashSet<APIStore>();
        try {
            conn = APIMgtDBUtil.getConnection();
            //This query to add external APIStores to database table
            String sqlQuery = "SELECT " +
                              "   ES.STORE_ID, " +
                              "   ES.STORE_DISPLAY_NAME, " +
                              "   ES.STORE_ENDPOINT, " +
                              "   ES.STORE_TYPE " +
                              "FROM " +
                              "   AM_EXTERNAL_STORES ES " +
                              "WHERE " +
                              "   ES.API_ID = ? ";


            ps = conn.prepareStatement(sqlQuery);
            //Get API Id
            int apiId;
            apiId = getAPIID(apiIdentifier, conn);
            if (apiId == -1) {
                String msg = "Could not load API record for: " + apiIdentifier.getApiName();
                log.error(msg);
                throw new APIManagementException(msg);
            }
            ps.setInt(1, apiId);
            rs = ps.executeQuery();
            while (rs.next()) {
                APIStore store = new APIStore();
                store.setName(rs.getString("STORE_ID"));
                store.setDisplayName(rs.getString("STORE_DISPLAY_NAME"));
                store.setEndpoint(rs.getString("STORE_ENDPOINT"));
                store.setType(rs.getString("STORE_TYPE"));
                store.setPublished(true);
                storesSet.add(store);
            }


        } catch (SQLException e) {
            handleException("Error while getting External APIStore details from the database for  the API : " + apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion(), e);

        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return storesSet;
    }


}

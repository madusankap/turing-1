/*
 * Copyright WSO2 Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.template.APITemplateBuilder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.RESTAPIAdminClient;
import org.wso2.carbon.apimgt.impl.clients.SequenceAdminServiceClient;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;

import javax.xml.namespace.QName;

public class APIGatewayManager {

	private static final Log log = LogFactory.getLog(APIGatewayManager.class);

	private static APIGatewayManager instance;

	private List<Environment> environments;

	private boolean debugEnabled = log.isDebugEnabled();

	private APIGatewayManager() {
		APIManagerConfiguration config = ServiceReferenceHolder.getInstance()
		                                                       .getAPIManagerConfigurationService()
		                                                       .getAPIManagerConfiguration();
		environments = config.getApiGatewayEnvironments();
	}

	public synchronized static APIGatewayManager getInstance() {
		if (instance == null) {
			instance = new APIGatewayManager();
		}
		return instance;
	}

	/**
	 * Publishes an API to all configured Gateways.
	 * 
	 * @param api
	 *            - The API to be published
	 * @param builder
	 *            - The template builder
	 * @param tenantDomain
	 *            - Tenant Domain of the publisher
	 * @throws Exception
	 *             - Thrown when publishing to at least one Gateway fails. A
	 *             single failure will stop all
	 *             subsequent attempts to publish to other Gateways.
	 */
	public void publishToGateway(API api, APITemplateBuilder builder, String tenantDomain)
	                                                                                      throws Exception {
		for (Environment environment : environments) {
			RESTAPIAdminClient client = new RESTAPIAdminClient(api.getId(), environment);
			// If the API exists in the Gateway
			if (client.getApi(tenantDomain) != null) {

				// If the Gateway type is 'production' and the production url
				// has been removed
				// Or if the Gateway type is 'sandbox' and the sandbox url has
				// been removed.
				if ((APIConstants.GATEWAY_ENV_TYPE_PRODUCTION.equals(environment.getType()) && api.getUrl() == null) ||
				    (APIConstants.GATEWAY_ENV_TYPE_SANDBOX.equals(environment.getType()) && api.getSandboxUrl() == null)) {
					if (debugEnabled) {
						log.debug("Removing API " + api.getId().getApiName() +
						          " from Environment " + environment.getName() +
						          " since its relevant URL has been removed.");
					}
					// We need to remove the api from the environment since its
					// relevant url has been removed.
					client.deleteApi(tenantDomain);
					undeployCustomSequences(api,tenantDomain, environment);
				} else {
					if (debugEnabled) {
						log.debug("API exists, updating existing API " + api.getId().getApiName() +
						          " in environment " + environment.getName());
					}
					client.updateApi(builder, tenantDomain);
					updateCustomSequences(api, tenantDomain, environment);
				}
			} else {
				// If the Gateway type is 'production' and a production url has
				// not been specified
				// Or if the Gateway type is 'sandbox' and a sandbox url has not
				// been specified
				if ((APIConstants.GATEWAY_ENV_TYPE_PRODUCTION.equals(environment.getType()) && api.getUrl() == null) ||
				    (APIConstants.GATEWAY_ENV_TYPE_SANDBOX.equals(environment.getType()) && api.getSandboxUrl() == null)) {

					if (debugEnabled) {
						log.debug("Not adding API to environment " + environment.getName() +
						          " since its endpoint URL " + "cannot be found");
					}
					// Do not add the API, continue loop.
					continue;
				} else {
					if (debugEnabled) {
						log.debug("API does not exist, adding new API " + api.getId().getApiName() +
						          " in environment " + environment.getName());
					}
					client.addApi(builder, tenantDomain);
					deployCustomSequences(api, tenantDomain, environment);
				}
			}
		}
	}

	/**
	 * Removed an API from the configured Gateways
	 * 
	 * @param api
	 *            - The API to be removed
	 * @param tenantDomain
	 *            - Tenant Domain of the publisher
	 * @throws Exception
	 *             - Thrown if a failure occurs while removing the API from the
	 *             Gateway. A single failure will
	 *             stop all subsequent attempts to remove from other Gateways.
	 */
	public void removeFromGateway(API api, String tenantDomain) throws Exception {
		for (Environment environment : environments) {
			RESTAPIAdminClient client = new RESTAPIAdminClient(api.getId(), environment);
			if (client.getApi(tenantDomain) != null) {
				if (debugEnabled) {
					log.debug("Removing API " + api.getId().getApiName() + " From environment " +
					          environment.getName());
				}
				client.deleteApi(tenantDomain);
				undeployCustomSequences(api, tenantDomain,environment);
			}
		}
	}

	/**
	 * Checks whether the API has been published.
	 * 
	 * @param api
	 *            - The API to be cheked.
	 * @param tenantDomain
	 *            - Tenant Domain of the publisher
	 * @return True if the API is available in at least one Gateway. False if
	 *         available in none.
	 * @throws Exception
	 *             - Thrown if a check to at least one Gateway fails.
	 */
	public boolean isAPIPublished(API api, String tenantDomain) throws Exception {
		for (Environment environment : environments) {
			RESTAPIAdminClient client = new RESTAPIAdminClient(api.getId(), environment);
			// If the API exists in at least one environment, consider as
			// published and return true.
			if (client.getApi(tenantDomain) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the specified in/out sequences from api object
	 * 
	 * @param api
	 *            -API object
	 * @param tenantDomain
	 * @param environment
	 * @throws APIManagementException
	 * @throws AxisFault
	 */
    private void deployCustomSequences(API api, String tenantDomain, Environment environment)
            throws APIManagementException,
                   AxisFault {

        if (isSequenceDefined(api.getInSequence()) || isSequenceDefined(api.getOutSequence())) {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                if(tenantDomain != null && !tenantDomain.equals("")){
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                }
                else{
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain
                            (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
                }
                int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

                if (isSequenceDefined(api.getInSequence())) {
//                    direction = "in";
//                    String inSeqExt = APIUtil.getSequenceExtensionName(api) + "--In";
//                    String inSequenceName = api.getInSequence();
//                    OMElement inSequence = APIUtil.getCustomSequence(inSequenceName, tenantId, direction);
//                    if (inSequence != null) {
//                        inSequence.getAttribute(new QName("name")).setAttributeValue(inSeqExt);
//                        seqClient.addSequence(inSequence, tenantDomain);
//                    }
                    deployInSequence(api, tenantId, tenantDomain, environment);
                }

                if (isSequenceDefined(api.getOutSequence())) {
//                    direction = "out";
//                    String outSeqExt = APIUtil.getSequenceExtensionName(api) + "--Out";
//                    String outSequenceName = api.getOutSequence();
//                    OMElement outSequence = APIUtil.getCustomSequence(outSequenceName, tenantId, direction);
//                    if (outSequence != null) {
//                        outSequence.getAttribute(new QName("name")).setAttributeValue(outSeqExt);
//                        seqClient.addSequence(outSequence, tenantDomain);
//
//                    }
                    deployOutSequence(api, tenantId, tenantDomain, environment);
                }

            } catch (Exception e) {
                String msg = "Error in deploying the sequence to gateway";
                log.error(msg, e);
                throw new APIManagementException(msg);
            }
            finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

    }

    private void deployInSequence(API api, int tenantId, String tenantDomain, Environment environment)
            throws APIManagementException, AxisFault {

        String inSeqExt = APIUtil.getSequenceExtensionName(api) + "--In";
        String inSequenceName = api.getInSequence();
        OMElement inSequence = APIUtil.getCustomSequence(inSequenceName, tenantId, "in");

       SequenceAdminServiceClient sequenceAdminServiceClient = new SequenceAdminServiceClient(environment);

        if (inSequence != null) {
            inSequence.getAttribute(new QName("name")).setAttributeValue(inSeqExt);
            sequenceAdminServiceClient.addSequence(inSequence, tenantDomain);
        }
    }

    private void deployOutSequence(API api, int tenantId, String tenantDomain, Environment environment)
            throws APIManagementException, AxisFault {

        String outSeqExt  = APIUtil.getSequenceExtensionName(api) + "--Out";
        String outSequenceName = api.getOutSequence();
        OMElement outSequence = APIUtil.getCustomSequence(outSequenceName, tenantId, "out");

        SequenceAdminServiceClient sequenceAdminServiceClient = new SequenceAdminServiceClient(environment);

        if (outSequence != null) {
            outSequence.getAttribute(new QName("name")).setAttributeValue(outSeqExt);
            sequenceAdminServiceClient.addSequence(outSequence, tenantDomain);
        }
    }

	/**
	 * Undeploy the sequences deployed in synapse
	 * 
	 * @param api
	 * @param tenantDomain
	 * @param environment
	 * @throws APIManagementException
	 */
    private void undeployCustomSequences(API api, String tenantDomain, Environment environment) {

        if (isSequenceDefined(api.getInSequence()) || isSequenceDefined(api.getOutSequence())) {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                if(tenantDomain != null && !tenantDomain.equals("")){
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                }
                else{
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain
                            (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
                }
                int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

                SequenceAdminServiceClient seqClient = new SequenceAdminServiceClient(environment);

                if (isSequenceDefined(api.getInSequence())) {
                    String inSequence = APIUtil.getSequenceExtensionName(api) + "--In";
                    seqClient.deleteSequence(inSequence, tenantDomain);
                }
                if (isSequenceDefined(api.getOutSequence())) {
                    String outSequence = APIUtil.getSequenceExtensionName(api) + "--Out";
                    seqClient.deleteSequence(outSequence, tenantDomain);
                }
            } catch (Exception e) {
                String msg = "Error in deleting the sequence from gateway";
                log.error(msg, e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    /**
	 * Update the custom sequences in gateway
	 * @param api
	 * @param tenantDomain
	 * @param environment
	 * @throws APIManagementException
	 */
	private void updateCustomSequences(API api, String tenantDomain, Environment environment)
	                                                                                         throws APIManagementException {

        //If sequences have been added, updated or removed.
        if (isSequenceDefined(api.getInSequence()) || isSequenceDefined(api.getOutSequence()) ||
                isSequenceDefined(api.getOldInSequence()) || isSequenceDefined(api.getOldOutSequence())) {

            try {
                PrivilegedCarbonContext.startTenantFlow();
                if(tenantDomain != null && !tenantDomain.equals("")){
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                }
                else{
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain
                            (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
                }
                int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

                SequenceAdminServiceClient seqClient = new SequenceAdminServiceClient(environment);

                //If an inSequence has been added, updated or removed.
                if (isSequenceDefined(api.getInSequence()) || isSequenceDefined(api.getOldInSequence())) {
                    String inSequenceKey = APIUtil.getSequenceExtensionName(api) + "--In";
                    //If sequence already exists
                    if (seqClient.isExistingSequence(inSequenceKey, tenantDomain)) {
                        //Delete existing sequence
                        seqClient.deleteSequence(inSequenceKey, tenantDomain);
                    }
                    //If an inSequence has been added or updated.
                    if(isSequenceDefined(api.getInSequence())){
                        //Deploy the inSequence
                        deployInSequence(api, tenantId, tenantDomain, environment);
                    }
                }

                //If an outSequence has been added, updated or removed.
                if (isSequenceDefined(api.getOutSequence()) || isSequenceDefined(api.getOldOutSequence())) {
                    String outSequence = APIUtil.getSequenceExtensionName(api) + "--Out";
                    //If the outSequence exists.
                    if (seqClient.isExistingSequence(outSequence, tenantDomain)) {
                        //Delete existing outSequence
                        seqClient.deleteSequence(outSequence, tenantDomain);
                    }

                    //If an outSequence has been added or updated.
                    if (isSequenceDefined(api.getOutSequence())){
                        //Deploy outSequence
                        deployOutSequence(api, tenantId, tenantDomain, environment);
                    }
                }
            } catch (Exception e) {
                String msg = "Error in updating the sequence at the Gateway";
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

    }

    private boolean isSequenceDefined(String sequence){
        if(sequence != null && !"none".equals(sequence)){
            return true;
        }
        return false;
    }

}

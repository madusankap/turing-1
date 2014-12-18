/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.impl.workflow;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.ApplicationRegistrationWorkflowDTO;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.keymgt.client.SubscriberKeyMgtClient;
import org.wso2.carbon.apimgt.keymgt.stub.types.carbon.ApplicationKeysDTO;

public abstract class AbstractApplicationRegistrationWorkflowExecutor extends WorkflowExecutor{

    private static final Log log = LogFactory.getLog(AbstractApplicationRegistrationWorkflowExecutor.class);

    public String getWorkflowType(){
       return WorkflowConstants.WF_TYPE_AM_APPLICATION_REGISTRATION_PRODUCTION;
    }

    public void execute(WorkflowDTO workFlowDTO) throws WorkflowException {
        log.debug("Executing AbstractApplicationRegistrationWorkflowExecutor...");
        ApiMgtDAO dao = new ApiMgtDAO();
        try {
            dao.createApplicationRegistrationEntry((ApplicationRegistrationWorkflowDTO) workFlowDTO, false);
            super.execute(workFlowDTO);
        } catch (APIManagementException e) {
            log.error("Error while creating Application Registration entry.", e);
            throw new WorkflowException("Error while creating Application Registration entry.", e);
        }
    }

    public void complete(WorkflowDTO workFlowDTO) throws WorkflowException {
        log.debug("Completing AbstractApplicationRegistrationWorkflowExecutor...");
        try {
            String status = null;
            if ("CREATED".equals(workFlowDTO.getStatus().toString())) {
                status = APIConstants.AppRegistrationStatus.REGISTRATION_CREATED;
            } else if ("REJECTED".equals(workFlowDTO.getStatus().toString())) {
                status = APIConstants.AppRegistrationStatus.REGISTRATION_REJECTED;
            } else if ("APPROVED".equals(workFlowDTO.getStatus().toString())) {
                status = APIConstants.AppRegistrationStatus.REGISTRATION_APPROVED;
            }

            ApiMgtDAO dao = new ApiMgtDAO();

            ApplicationRegistrationWorkflowDTO regWorkFlowDTO = (ApplicationRegistrationWorkflowDTO)workFlowDTO;
            dao.populateAppRegistrationWorkflowDTO(regWorkFlowDTO);
            if(((ApplicationRegistrationWorkflowDTO) workFlowDTO).getApplication() != null){
                dao.updateApplicationRegistration(status,regWorkFlowDTO.getKeyType(),regWorkFlowDTO.getApplication().getId());
            }
            super.complete(workFlowDTO);
            if(((ApplicationRegistrationWorkflowDTO) workFlowDTO).getApplication() == null){
                throw new WorkflowException("Couldn't find application to complete the Registration process.");
            }
        } catch (APIManagementException e) {
            log.error("Error while completing Application Registration entry.", e);
            throw new WorkflowException("Error while completing Application Registration entry.", e);
        }
    }

    protected void generateKeysForApplication(ApplicationRegistrationWorkflowDTO workflowDTO) throws APIManagementException {
        log.debug("Registering Application and creating an Access Token... ");
        Application application = workflowDTO.getApplication();
        Subscriber subscriber = application.getSubscriber();

        if (application == null || subscriber == null || workflowDTO.getAllowedDomains() == null) {
            ApiMgtDAO dao = new ApiMgtDAO();
            dao.populateAppRegistrationWorkflowDTO(workflowDTO);
        }

        SubscriberKeyMgtClient keyMgtClient = APIUtil.getKeyManagementClient();

        try {

            ApplicationKeysDTO keysDTO =  keyMgtClient.getApplicationAccessKey(subscriber.getName(), application.getName(),
                    workflowDTO.getKeyType(), application.getCallbackUrl(),
                    workflowDTO.getAllowedDomains(), Long.toString(workflowDTO.getValidityTime()));
            workflowDTO.setKeyDetails(keysDTO);
        } catch (Exception e) {
            APIUtil.handleException("Error occurred while executing SubscriberKeyMgtClient.", e);
        }
    }
}

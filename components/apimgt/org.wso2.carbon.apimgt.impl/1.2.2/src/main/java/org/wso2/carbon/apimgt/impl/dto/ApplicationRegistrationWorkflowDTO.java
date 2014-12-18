/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.impl.dto;

import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowConstants;
import org.wso2.carbon.apimgt.keymgt.stub.types.carbon.ApplicationKeysDTO;

/**
 * DTO class for Application Registration workflow.
 */

public class ApplicationRegistrationWorkflowDTO extends WorkflowDTO {
    private Application application;
    private String userName;
    private String keyType;
    private String domainList;
    private long validityTime;
    private String[] allowedDomains;
    private ApplicationKeysDTO keyDetails;


    public void setDomainList(String[] accessAllowDomains) {
        StringBuilder builder = new StringBuilder();
        if (accessAllowDomains != null && !accessAllowDomains[0].trim().equals("")) {
            for (String domain : accessAllowDomains) {
                builder.append(domain).append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            domainList = builder.toString();
        } else {
            domainList = "ALL";
        }
        allowedDomains = accessAllowDomains;
    }

    public void setDomainList(String domainList){
        this.domainList = domainList;
    }

    public String getDomainList() {
        return domainList;
    }

    public String[] getAllowedDomains() {
        if (allowedDomains != null) {
            return allowedDomains;
        } else if (domainList != null) {
            return domainList.split(",");
        }
        return null;
    }

    public ApplicationKeysDTO getKeyDetails() {
        return keyDetails;
    }

    public void setKeyDetails(ApplicationKeysDTO keyDetails) {
        this.keyDetails = keyDetails;
    }

    public long getValidityTime() {
        return validityTime;
    }

    public void setValidityTime(long validityTime) {
        this.validityTime = validityTime;
    }

    public ApplicationRegistrationWorkflowDTO() {
        super.setWorkflowType(WorkflowConstants.WF_TYPE_AM_APPLICATION_REGISTRATION_PRODUCTION);
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}

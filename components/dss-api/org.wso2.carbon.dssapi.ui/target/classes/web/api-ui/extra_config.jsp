<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.dssapi.ui.APIPublisherClient" %>
<%@ page import="org.wso2.carbon.service.mgt.xsd.ServiceMetaData" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.context.CarbonContext" %>
<%@ page import="org.osgi.framework.BundleContext" %>
<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    String serviceName = request.getParameter("serviceName");
    boolean APIAvailability = false;
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String webContext = (String) request.getAttribute(CarbonConstants.WEB_CONTEXT);
    CarbonContext context = CarbonContext.getThreadLocalCarbonContext();


    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    APIPublisherClient client = null;
    ServiceMetaData service = null;
    try {
        client = new APIPublisherClient(cookie, backendServerURL, configContext);
        service = client.getServiceData(serviceName).getServices()[0];

        APIAvailability = client.isAPIAvailable(service);
    } catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<fmt:bundle basename="org.wso2.carbon.dataservices.ui.i18n.Resources">
    <table class="styledLeft" id="apiOperationsTable" style="margin-left: 0px;" width="100%">
        <thead>
        <tr>
            <th colspan="2" align="left"><fmt:message key="api.management.menu"/></th>
        </tr>
        </thead>
        <%
            if (APIAvailability) {
        %>
            <tr class="tableOddRow">
                <td>Published on </td>
                <td>Last Updated</td>
            </tr>
            <tr class="tableEvenRow">
                <td colspan="2"><input type="button" value="Update API" onclick="changeState(false)" /> <input type="button" value="Unpublish API" /> </td>
            </tr>
        <tr class="tableOddRow">
            <td colspan="2"></td>
        </tr>
        <tr class="tableEvenRow">
            <td>User</td>
            <td>Action</td>
        </tr>

        <%
            } else {
        %>
            <tr class="tableOddRow">
                <td>API Name : <input type="text" name="apiName" /> </td>
                <td>Version : <input type="text" name="apiVersion" /> </td>
            </tr>
            <tr class="tableEvenRow">
                <td colspan="2"><input type="button" value="Publish as an API" onclick="changeState(true)"></td>
            </tr>
        <%
            }
        %>
    </table>

    <script type="text/javascript">
        jQuery.noConflict();
        function changeState(active) {
            try {
                var url = '../api-ui/change_state.jsp?serviceName=<%=serviceName%>&isPublishRequest=' + active;
                jQuery.ajax({
                    url: url,
                    async: false,
                    type: "GET",
                    cache: false,
                    success: function () {
                        <%
                            //String successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                            //CarbonUIMessage.sendCarbonUIMessage(successMessage,CarbonUIMessage.INFO,request,response,"https://10.100.5.179:9443/carbon/service-mgt/service_info.jsp?serviceName="+serviceName);
                         %>
                        var successMessage = "API change request sent and currently processing. Please reload the page after a few seconds.";
                        alert(successMessage);
                        location.reload(true);
                    }
                });
            } catch (exception) {
                alert(exception);
            }
        }
    </script>
</fmt:bundle>
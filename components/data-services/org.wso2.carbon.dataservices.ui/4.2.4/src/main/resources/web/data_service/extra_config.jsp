<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.dssapi.ui.APIPublisherClient" %>
<%@ page import="org.wso2.carbon.service.mgt.xsd.ServiceMetaData" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%
    String serviceName = request.getParameter("serviceName");
    boolean APIAvailability = false;
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    APIPublisherClient client=null;
    ServiceMetaData service=null;
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
	<tr>
		<td colspan="2"><a
			href="../ds/serviceDetails.jsp?serviceName=<%=serviceName%>"
			class="icon-link-nofloat"
			style="background-image: url(images/edit2.gif);"> <fmt:message
			key="dataservice.edit.via.wizard" /> </a></td>
	</tr>
    <%--<tr>
		<td colspan="2"><a
			href="../dswizard/tree.jsp?serviceName=<%=serviceName%>&flag=new_ui"
			class="icon-link-nofloat"
			style="background-image: url(images/edit2.gif);"> <fmt:message
			key="dataservice.edit.via.new.wizard" /> </a></td>
	</tr>--%>
	<tr>
		<td colspan="2"><a
			href="../ds/raw-xml-edit.jsp?serviceName=<%=serviceName%>"
			class="icon-link-nofloat"
			style="background-image: url(images/edit2.gif);"> <fmt:message
			key="dataservice.edit.raw.xml.edit" /> </a></td>
	</tr>


    <!-- Custom UI to enable/disable API for the Data Service -->
    <tr>
        <td><p class="icon-text"><fmt:message key="api.availability"/></p></td>
        <%
            if (APIAvailability) {
        %>
        <td>
            <span style="margin-left: 3px"> <input id="apiCheckBox" type="checkbox" checked onclick="changeState()"> <fmt:message key="api.available"/></span>
        </td>
        <%
        } else {
        %>
        <td>
            <span style="margin-left: 3px"> <input id="apiCheckBox" type="checkbox"  onclick="changeState()">  <fmt:message key="api.unavailable"/></span>
        </td>
        <%
            }
        %>
    </tr>

    <script type="text/javascript">
        jQuery.noConflict();
        function changeState() {
           // alert("changeState");
            try {
                var active = document.getElementById("apiCheckBox").checked;
                //alert(active);
            } catch(ex) {
                alert(ex);
            }

            var url = '../api-ui/changeState.jsp?serviceName=<%=serviceName%>&isPublishRequest=' + active;
            jQuery.ajax({
                url: url,
                async: false,
                type: "GET",
                cache: false,
                success: function () {
                    alert("API Change request sent.\nPlease reload the page after few seconds.");
                    location.reload(true);
                }
            });


            //alert(url);
            //jQuery("#publishStateDiv").load(url, null, null );
        }
    </script>

    <div id="LoadingImage" style="display: none">
        <img src="images/ajax-loader.gif" />
    </div>

</fmt:bundle>
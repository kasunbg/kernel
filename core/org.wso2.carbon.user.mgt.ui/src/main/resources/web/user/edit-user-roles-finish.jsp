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
<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@page import="org.wso2.carbon.user.mgt.common.IUserAdmin"%>
<%@page import="org.wso2.carbon.user.mgt.ui.UserAdminClient"%>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.util.ResourceBundle" %>

<%
	String BUNDLE = "org.wso2.carbon.userstore.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
	String username = CharacterEncoder.getSafeText(request.getParameter("username"));
	String[] newUserList = request.getParameterValues("selectedRoles");
    boolean logout = false;
    if(request.getParameter("logout") != null){
        logout = Boolean.parseBoolean(request.getParameter("logout"));
    }
    try {
        String cookie = (String)session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        IUserAdmin proxy =
            (IUserAdmin) CarbonUIUtil.
                    getServerProxy(new UserAdminClient(cookie, backendServerURL, configContext),
                                         IUserAdmin.class, session);
        proxy.updateRolesOfUser(username, newUserList);
        if(logout == true){
%>
            <%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
            <script type="text/javascript">
                window.location.href = "../admin/logout_action.jsp";
            </script>
<%
        }else{
%>
            <script type="text/javascript">
                location.href = "user-mgt.jsp?ordinal=1";
            </script>
<%
        }
    } catch (Exception e) {
         String message = MessageFormat.format(resourceBundle.getString("role.list.cannot.update"), username, e.getMessage());
         CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
         %>
                <script type="text/javascript">
                       location.href = "edit-user-roles.jsp?username=<%=username%>";
                </script>
         <%
        
    }
%>

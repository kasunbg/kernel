package org.wso2.carbon.user.mgt.multiplecredentials;/*
 *   Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialUserStoreManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.user.mgt.common.ClaimValue;
import org.wso2.carbon.user.mgt.common.MultipleCredentialsUserAdminException;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MultipleCredentialsUserProxy {

    private UserRealm realm;

    private static final String DOMAIN_PARAMETER  = "multipleCredentialDomain";

    private static final String MULTIPLE_CREDENTIAL_DOMAIN_NAME  = "multipleCredential.com";

    private static Log log = LogFactory.getLog(MultipleCredentialsUserProxy.class);

    private static final Object lock = new Object();

    private static MultipleCredentialUserStoreManager userStoreManager;

    public MultipleCredentialsUserProxy(UserRealm realm) {
        this.realm = realm;
    }

    /**
     * Gets logged in user of the server
     *
     * @return  user name
     */
    private String getLoggedInUser(){

        MessageContext context = MessageContext.getCurrentMessageContext();
        if(context != null){
            HttpServletRequest request = (HttpServletRequest) context.
                    getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            if(request != null){
                HttpSession httpSession = request.getSession(false);
                return (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
            }
        }
        return null;
    }


    public void addUser(Credential credential, String[] roles, ClaimValue[] claims,
                        String profileName) throws MultipleCredentialsUserAdminException {
        try {
            roles = checkRolesPermissions(roles);
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            Map<String, String> claimMap = new HashMap<String, String>();
            if (claims != null) {
                for (ClaimValue claimValue : claims) {
                    claimMap.put(claimValue.getClaimURI(), claimValue.getValue());
                }
            }
            mgr.addUser(credential, roles, claimMap, profileName);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

    private String[] checkRolesPermissions(String[] roles)
            throws UserStoreException, MultipleCredentialsUserAdminException {
        RealmConfiguration realmConfig = realm.getRealmConfiguration();
        if (realmConfig.
                getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_EXTERNAL_IDP) != null) {
            throw new MultipleCredentialsUserAdminException(
                    "Please contact your external Identity Provider to add users");
        }

        if (roles != null) {
            String loggedInUserName = getLoggedInUser();
            Arrays.sort(roles);
            boolean isRoleHasAdminPermission = false;
            for(String role : roles){
                isRoleHasAdminPermission = realm.getAuthorizationManager().
                        isRoleAuthorized(role, "/permission", UserMgtConstants.EXECUTE_ACTION);
                if(!isRoleHasAdminPermission){
                    isRoleHasAdminPermission = realm.getAuthorizationManager().
                            isRoleAuthorized(role, "/permission/admin", UserMgtConstants.EXECUTE_ACTION);
                }

                if(isRoleHasAdminPermission){
                    break;
                }
            }

            if ((Arrays.binarySearch(roles, realmConfig.getAdminRoleName()) > -1 ||
                 isRoleHasAdminPermission) &&
                !realmConfig.getAdminUserName().equals(loggedInUserName)) {
                log.warn("An attempt to assign user to Admin permission role by user : " +
                         loggedInUserName);
                throw new UserStoreException("Can not assign user to Admin permission role");
            }
            boolean isContained = false;
            String[] temp = new String[roles.length + 1];
            for (int i = 0; i < roles.length; i++) {
                temp[i] = roles[i];
                if (roles[i].equals(realmConfig.getEveryOneRoleName())) {
                    isContained = true;
                    break;
                }
            }

            if (!isContained) {
                temp[roles.length] = realmConfig.getEveryOneRoleName();
                roles = temp;
            }
        }
        return roles;
    }

    private MultipleCredentialUserStoreManager getUserStoreManager() throws UserStoreException {

        if(userStoreManager == null){
            synchronized (lock) {
                if(userStoreManager == null){

                    // read parameter from axis2.xml
                    AxisConfiguration axisConfiguration = CarbonConfigurationContextFactory.
                                                            getConfigurationContext().getAxisConfiguration();
                    String multipleCredentialDomain = (String) axisConfiguration.getParameterValue(DOMAIN_PARAMETER);
                    if(multipleCredentialDomain == null){
                        multipleCredentialDomain = MULTIPLE_CREDENTIAL_DOMAIN_NAME;
                    }

                    UserStoreManager storeManager = realm.getUserStoreManager();
                    UserStoreManager second = storeManager.getSecondaryUserStoreManager(multipleCredentialDomain);
                    if(second != null){
                        storeManager = second;
                    }

                    if (!(storeManager instanceof MultipleCredentialUserStoreManager)) {
                        String msg = "User store does not support multiple credentials.";
                        MultipleCredentialsNotSupportedException e = new MultipleCredentialsNotSupportedException(msg);
                        log.fatal(msg, e);
                        throw e;
                    }

                    userStoreManager = (MultipleCredentialUserStoreManager) storeManager;

                }
            }
        }

        return userStoreManager;
    }

    public void addUser(Credential[] credential, String[] roles, ClaimValue[] claims,
                        String profileName) throws MultipleCredentialsUserAdminException {
        try {
            roles = checkRolesPermissions(roles);
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            Map<String, String> claimMap = new HashMap<String, String>();
            if (claims != null) {
                for (ClaimValue claimValue : claims) {
                    claimMap.put(claimValue.getClaimURI(), claimValue.getValue());
                }
            }
            mgr.addUser(credential, roles, claimMap, profileName);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

    public void deleteUser(String userName, String credentialType,  Registry registry)
            throws MultipleCredentialsUserAdminException {
        try {

            String loggedInUserName = getLoggedInUser();
            RealmConfiguration realmConfig = realm.getRealmConfiguration();
            if(userName != null && userName.equals(realmConfig.getAdminUserName()) &&
               !userName.equals(loggedInUserName)){
                log.warn("An attempt to delete Admin user by user : " + loggedInUserName);
                throw new UserStoreException("Can not delete Admin user");
            }

            MultipleCredentialUserStoreManager mgr = getUserStoreManager();

            if(userName != null){
                String[] roles = mgr.getRoleListOfUser(userName, credentialType);
                Arrays.sort(roles);
                if(Arrays.binarySearch(roles, realmConfig.getAdminRoleName()) > -1 &&
                   loggedInUserName != null &&!userName.equals(loggedInUserName) &&
                   !realmConfig.getAdminUserName().equals(loggedInUserName) &&
                   !userName.equals(realmConfig.getAdminUserName())){
                    log.warn("An attempt to delete user in Admin role by user : " +
                             loggedInUserName);
                    throw new UserStoreException("Can not delete user in Admin role");
                }
            }

            mgr.deleteUser(userName, credentialType);
            String path = RegistryConstants.PROFILES_PATH + userName;
            if (registry.resourceExists(path)) {
                registry.delete(path);
            }
        } catch (RegistryException e) {
            String msg = "Error deleting user from registry, " + e.getMessage();
            log.error(msg, e);
            throw new MultipleCredentialsUserAdminException(msg, e);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

    public void addCredential(String anIdentifier, String credentialType, Credential credential)
            throws MultipleCredentialsUserAdminException {
        try {
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            mgr.addCredential(anIdentifier, credentialType,  credential);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

    public void updateCredential(String identifier, String credentialType, Credential credential)
            throws MultipleCredentialsUserAdminException {
        try {
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            mgr.updateCredential(identifier, credentialType, credential);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

//    public void updateCredentialByUser(Credential credential)
//            throws MultipleCredentialsUserAdminException {
//        try {
//
//            HttpServletRequest request = (HttpServletRequest) MessageContext
//                    .getCurrentMessageContext().getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
//            HttpSession httpSession = request.getSession(false);
//            String identifier = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
//
//            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
//            mgr.updateCredential(identifier, credential);
//        } catch (UserStoreException e) {
//            // previously logged so logging not needed
//            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
//        }
//    }

    public void deleteCredential(String identifier, String credentialType) throws MultipleCredentialsUserAdminException {
        try {
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            mgr.deleteCredential(identifier, credentialType);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }

    public Credential[] getCredentials(String anIdentifier, String credentialType)
            throws MultipleCredentialsUserAdminException {
        try {
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            return mgr.getCredentials(anIdentifier, credentialType);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }

    }

    public boolean authenticate(Credential credential)
            throws MultipleCredentialsUserAdminException {
        try {
            MultipleCredentialUserStoreManager mgr = getUserStoreManager();
            return mgr.authenticate(credential);
        } catch (UserStoreException e) {
            // previously logged so logging not needed
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsUserAdminException(e.getMessage(), e);
        }
    }
}

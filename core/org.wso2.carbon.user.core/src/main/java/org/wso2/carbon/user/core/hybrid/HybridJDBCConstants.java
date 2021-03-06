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
package org.wso2.carbon.user.core.hybrid;

public class HybridJDBCConstants {
    
    public static final String ADD_ROLE_SQL = "INSERT INTO UM_HYBRID_ROLE (UM_ROLE_NAME, UM_TENANT_ID) VALUES (?, ?)";
    public static final String DELETE_ROLE_SQL = "DELETE FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME = ? AND UM_TENANT_ID=?";
    public static final String ON_DELETE_ROLE_REMOVE_USER_ROLE_SQL = "DELETE FROM UM_HYBRID_USER_ROLE WHERE " +
            "UM_ROLE_ID=(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME = ? AND " +
            "UM_TENANT_ID=?) AND UM_TENANT_ID=?"; 
    public static final String GET_ROLE_ID = "SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME =? " +
            "AND UM_TENANT_ID=?";
    
    //a single role name - multiple user names
    public static final String ADD_USER_TO_ROLE_SQL = "INSERT INTO UM_HYBRID_USER_ROLE (UM_USER_NAME, UM_ROLE_ID, " +
        "UM_TENANT_ID) VALUES (?,(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?), ?)";
    public static final String ADD_USER_TO_ROLE_SQL_MSSQL = "INSERT INTO UM_HYBRID_USER_ROLE (UM_USER_NAME, UM_ROLE_ID, " +
        "UM_TENANT_ID) SELECT (?),(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?), (?)";

    public static final String REMOVE_USER_FROM_ROLE_SQL = "DELETE FROM UM_HYBRID_USER_ROLE WHERE UM_USER_NAME=? AND " +
            "UM_ROLE_ID=(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?) AND UM_TENANT_ID=?";
    
    //a single user name - multiple role names
    public static final String ADD_ROLE_TO_USER_SQL = "INSERT INTO UM_HYBRID_USER_ROLE (UM_ROLE_ID, UM_USER_NAME, " +
        "UM_TENANT_ID) VALUES ((SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?), ?, ?)";
    public static final String ADD_ROLE_TO_USER_SQL_MSSQL = "INSERT INTO UM_HYBRID_USER_ROLE (UM_ROLE_ID, UM_USER_NAME, " +
        "UM_TENANT_ID) SELECT (SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?), (?), (?)";

    //openedge
    public static final String ADD_USER_TO_ROLE_SQL_OPENEDGE = "INSERT INTO UM_HYBRID_USER_ROLE (UM_USER_NAME, UM_ROLE_ID, UM_TENANT_ID) SELECT ?, UM_ID, ? FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?";
    public static final String ADD_ROLE_TO_USER_SQL_OPENEDGE = "INSERT INTO UM_HYBRID_USER_ROLE (UM_ROLE_ID, UM_USER_NAME, UM_TENANT_ID) SELECT UM_ID, ?, ? FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?";

    public static final String REMOVE_ROLE_FROM_USER_SQL = "DELETE FROM UM_HYBRID_USER_ROLE WHERE UM_ROLE_ID=" +
            "(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?) AND " +
            "UM_USER_NAME=? AND UM_TENANT_ID=?";

    public static final String GET_ROLES = "SELECT UM_ROLE_NAME FROM UM_HYBRID_ROLE WHERE UM_TENANT_ID=?";
    public static final String GET_USER_LIST_OF_ROLE_SQL = "SELECT UM_USER_NAME FROM UM_HYBRID_USER_ROLE WHERE " +
            "UM_ROLE_ID=(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?) AND UM_TENANT_ID=?";
    //public static final String GET_ROLE_LIST_OF_USER_SQL = "SELECT UM_ROLE_NAME FROM UM_HYBRID_USER_ROLE, UM_HYBRID_ROLE WHERE UM_USER_NAME=? AND UM_HYBRID_USER_ROLE.UM_ROLE_ID=UM_HYBRID_ROLE.UM_ID";
    
    public static final String GET_ROLE_LIST_OF_USER_SQL = "SELECT UM_ROLE_NAME FROM UM_HYBRID_USER_ROLE, " +
            "UM_HYBRID_ROLE WHERE UM_USER_NAME=? AND UM_HYBRID_USER_ROLE.UM_ROLE_ID=UM_HYBRID_ROLE.UM_ID AND " +
            "UM_HYBRID_USER_ROLE.UM_TENANT_ID=? AND UM_HYBRID_ROLE.UM_TENANT_ID=?";

    public static final String IS_USER_IN_ROLE_SQL = "SELECT UM_ROLE_ID FROM UM_HYBRID_USER_ROLE WHERE UM_USER_NAME=? " +
            "AND UM_ROLE_ID=(SELECT UM_ID FROM UM_HYBRID_ROLE WHERE UM_ROLE_NAME=? AND UM_TENANT_ID=?) AND UM_TENANT_ID=?";

    public static final String REMOVE_USER_SQL = "DELETE FROM UM_HYBRID_USER_ROLE WHERE UM_USER_NAME=?";

    public static final String UPDATE_ROLE_NAME_SQL="UPDATE UM_HYBRID_ROLE set UM_ROLE_NAME=? WHERE UM_ROLE_NAME = ? AND UM_TENANT_ID=?";
    
    public static final String ADD_REMEMBERME_VALUE_SQL="INSERT INTO UM_HYBRID_REMEMBER_ME (UM_USER_NAME, UM_COOKIE_VALUE, UM_CREATED_TIME, UM_TENANT_ID) VALUES (?,?,?,?)";
    
    public static final String UPDATE_REMEMBERME_VALUE_SQL="UPDATE UM_HYBRID_REMEMBER_ME SET UM_COOKIE_VALUE=?, UM_CREATED_TIME=? WHERE UM_USER_NAME=? AND UM_TENANT_ID=?";
    
    public static final String GET_REMEMBERME_VALUE_SQL="SELECT UM_COOKIE_VALUE, UM_CREATED_TIME FROM UM_HYBRID_REMEMBER_ME WHERE UM_USER_NAME=? AND UM_TENANT_ID=?";
    
}

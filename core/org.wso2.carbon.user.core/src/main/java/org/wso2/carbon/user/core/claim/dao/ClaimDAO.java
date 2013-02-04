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
package org.wso2.carbon.user.core.claim.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.claim.ClaimMapping;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClaimDAO {

    private static Log log = LogFactory.getLog(DatabaseUtil.class);

    private DataSource dataSource = null;

    private int tenantId = MultitenantConstants.INVALID_TENANT_ID;

    public ClaimDAO(DataSource dataSource, int tenantId) {
        this.dataSource = dataSource;
        this.tenantId = tenantId;
    }

    public void addClaimMapping(ClaimMapping claim) throws UserStoreException {
        Connection dbConnection = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            this.addClaimMapping(dbConnection, claim);
            dbConnection.commit();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeConnection(dbConnection);
        }
    }

    public void updateClaim(ClaimMapping claim) throws UserStoreException {
        Connection dbConnection = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            this.updateClaimMapping(dbConnection, claim);
            dbConnection.commit();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeConnection(dbConnection);
        }
    }

    public void deleteClaimMapping(ClaimMapping cm) throws UserStoreException {
        Connection dbConnection = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            deleteClaimMapping(dbConnection, cm.getClaim().getClaimUri(), 
                    cm.getClaim().getDialectURI());
            dbConnection.commit();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeConnection(dbConnection);
        }
    }

    public void deleteDialect(String dialectUri) throws UserStoreException {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            prepStmt = dbConnection
                    .prepareStatement(ClaimDBConstants.GET_CLAIMS_FOR_DIALECTT_SQL);
            prepStmt.setString(1, dialectUri);
            prepStmt.setInt(2, tenantId);
            prepStmt.setInt(3, tenantId);
            ResultSet rs = prepStmt.executeQuery();
            List<String> lst = new ArrayList<String>();
            while (rs.next()) {
                lst.add(rs.getString(1));
            }
            prepStmt.close();
            for (Iterator<String> ite = lst.iterator(); ite.hasNext();) {
                String claimUri = ite.next();
                this.deleteClaimMapping(dbConnection, claimUri, dialectUri);
            }
            
            prepStmt = dbConnection
            .prepareStatement(ClaimDBConstants.DELETE_DIALECT);
            prepStmt.setString(1, dialectUri);
            prepStmt.executeUpdate();
            prepStmt.close();
    
            
            dbConnection.commit();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
    }

    public void addCliamMappings(ClaimMapping[] claims) throws UserStoreException {
        Connection dbConnection = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            for (ClaimMapping claim : claims) {
                this.addClaimMapping(dbConnection, claim);
            }
            dbConnection.commit();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeConnection(dbConnection);
        }

    }

    public int getDialectCount() throws UserStoreException {
        int count = 0;
        PreparedStatement prepStmt = null;
        Connection dbConnection = null;
        ResultSet rs = null;
        try {
            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.COUNT_DIALECTS);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        return count;
    }

    // (um_dialect_id, um_claim_uri, " +
    // "um_display_tag, um_description, um_mapped_attribute, um_reg_ex, " +
    // "um_supported, um_required) v" +
    // "
    protected void addClaimMapping(Connection dbConnection, ClaimMapping claimMapping)
            throws UserStoreException {
        PreparedStatement prepStmt = null;
        try {
            Claim claim = claimMapping.getClaim();
            int dialectId = getDialect(dbConnection, claim.getDialectURI());
            if (dialectId == -1) {
                dialectId = addDialect(dbConnection, claim.getDialectURI());
            }
            short isSupported = 0;
            if (claim.isSupportedByDefault()) {
                isSupported = 1;
            }
            ;
            short isRequired = 0;
            if (claim.isRequired()) {
                isRequired = 1;
            }
            ;
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.ADD_CLAIM_SQL);
            prepStmt.setInt(1, dialectId);
            prepStmt.setString(2, claim.getClaimUri());
            prepStmt.setString(3, claim.getDisplayTag());
            prepStmt.setString(4, claim.getDescription());
            prepStmt.setString(5, claimMapping.getMappedAttribute());
            prepStmt.setString(6, claim.getRegEx());
            prepStmt.setShort(7, isSupported);
            prepStmt.setShort(8, isRequired);
            prepStmt.setInt(9, claim.getDisplayOrder());
            prepStmt.setInt(10, tenantId);
            prepStmt.executeUpdate();
            prepStmt.close();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(null, prepStmt);
        }

    }

    public List<ClaimMapping> loadClaimMappings() throws UserStoreException {
        List<ClaimMapping> lst = new ArrayList<ClaimMapping>();
        PreparedStatement prepStmt = null;
        Connection dbConnection = null;
        ResultSet rs = null;
        try {
            dbConnection = dataSource.getConnection();
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.GET_ALL_CLAIMS_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                ClaimMapping cm = new ClaimMapping();
                Claim claim = new Claim();
                String value = rs.getString(1);
                claim.setDialectURI(value);
                value = rs.getString(2);
                claim.setClaimUri(value);
                value = rs.getString(3);
                claim.setDisplayTag(value);
                value = rs.getString(4);
                claim.setDescription(value);
                value = rs.getString(5);
                cm.setMappedAttribute(value);
                value = rs.getString(6);
                claim.setRegEx(value);
                short is = rs.getShort(7);
                if (is == 1) {
                    claim.setSupportedByDefault(true);
                }
                is = rs.getShort(8);
                if (is == 1) {
                    claim.setRequired(true);
                }
                claim.setDisplayOrder(rs.getInt(9));
                cm.setClaim(claim);
                lst.add(cm);
            }
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        return lst;
    }

    protected void updateClaimMapping(Connection dbConnection, ClaimMapping cm)
            throws UserStoreException {
        PreparedStatement prepStmt = null;
        try {
            Claim claim = cm.getClaim();
            short isSupported = 0;
            if (claim.isSupportedByDefault()) {
                isSupported = 1;
            }

            short isRequired = 0;
            if (claim.isRequired()) {
                isRequired = 1;
            }

            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.UPDATE_CLAIM_SQL);
            prepStmt.setString(1, claim.getDisplayTag());
            prepStmt.setString(2, claim.getDescription());
            prepStmt.setString(3, cm.getMappedAttribute());
            prepStmt.setString(4, claim.getRegEx());
            prepStmt.setShort(5, isSupported);
            prepStmt.setShort(6, isRequired);
            prepStmt.setInt(7, claim.getDisplayOrder());
            prepStmt.setString(8, claim.getClaimUri());
            prepStmt.setString(9, claim.getDialectURI());
            prepStmt.setInt(10, tenantId);
            prepStmt.setInt(11, tenantId);

            prepStmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(null, prepStmt);
        }
    }

    protected void deleteClaimMapping(Connection dbConnection, String claimUri, String dialectUri)
            throws UserStoreException {
            PreparedStatement prepStmt = null;
            try {
                if(dialectUri.equals(UserCoreConstants.DEFAULT_CARBON_DIALECT)){
                    prepStmt = dbConnection.
                    prepareStatement(ClaimDBConstants.GET_CLAIMS_FOR_DIALECTT_SQL);
                    prepStmt.setString(1, dialectUri);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setInt(3, tenantId);
                    ResultSet rs = prepStmt.executeQuery();
                    rs.last();
                    if(rs.getRow() < 2){
                        throw new UserStoreException("Cannot delete all claim mappings");
                    }
                }
            prepStmt = dbConnection
                    .prepareStatement(ClaimDBConstants.ON_CLAIM_DELETE_REMOVE_BEHAVIOR);
            prepStmt.setString(1, claimUri);
            prepStmt.setString(2, dialectUri);
            prepStmt.setInt(3, tenantId);
            prepStmt.setInt(4, tenantId);
            prepStmt.executeUpdate();
            prepStmt.close();
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.DELETE_CLAIM_SQL);
            prepStmt.setString(1, claimUri);
            prepStmt.setString(2, dialectUri);
            prepStmt.setInt(3, tenantId);
            prepStmt.setInt(4, tenantId);
            prepStmt.executeUpdate();
            prepStmt.close();
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(null, prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param uri
     * @return
     */
    protected int getDialect(Connection dbConnection, String uri) throws UserStoreException {
        int dialectId = -1;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.GET_DIALECT_ID_SQL);
            prepStmt.setString(1, uri);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                dialectId = rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(null, rs, prepStmt);
        }
        return dialectId;
    }

    protected int addDialect(Connection dbConnection, String uri) throws UserStoreException {
        int dialectId = -1;
        PreparedStatement prepStmt = null;
        try {
            prepStmt = dbConnection.prepareStatement(ClaimDBConstants.ADD_DIALECT_SQL);
            prepStmt.setString(1, uri);
            prepStmt.setInt(2, tenantId);
            prepStmt.executeUpdate();
            prepStmt.close();
            dialectId = getDialect(dbConnection, uri);
        } catch (SQLException e) {
            log.error("Database Error - " + e.getMessage(), e);
            throw new UserStoreException("Database Error - " + e.getMessage(), e);
        } finally {
            DatabaseUtil.closeAllConnections(null, prepStmt);
        }
        return dialectId;
    }

}

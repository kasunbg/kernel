/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.user.mgt.bulkimport;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.mgt.common.UserAdminException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CSVUserBulkImport {

    private static Log log = LogFactory.getLog(CSVUserBulkImport.class);

    private BufferedReader reader;
    private BulkImportConfig config;

    public CSVUserBulkImport(BulkImportConfig config) {
        this.config = config;
        this.reader = new BufferedReader(new InputStreamReader(config.getInStream()));
    }

    public void addUserList(UserStoreManager userStore) throws UserAdminException {
        try {
            CSVReader csvReader = new CSVReader(reader, ',', '"', 1);
            String password = config.getDefaultPassword();
            String[] line = csvReader.readNext();
            boolean isDuplicate = false;
            while (line != null && line.length > 0) {
                String userName = line[0];
                if (!userStore.isExistingUser(userName)) {
                    userStore.addUser(userName, password, null, null, null, true);
                } else {
                    isDuplicate = true;
                }
                line = csvReader.readNext();
            }
            
            if (isDuplicate == true) {
                throw new UserAdminException(
                        "Detected duplicate usernames. Failed to import duplicate users. Non-duplicate user names were successfually imported.");
            }
        } catch (UserAdminException e) {
            throw e;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new UserAdminException(e.getMessage(), e);
        }
    }

}

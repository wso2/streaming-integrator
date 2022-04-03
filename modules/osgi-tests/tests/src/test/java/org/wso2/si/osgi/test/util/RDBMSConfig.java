/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.si.osgi.test.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.si.osgi.test.DBPersistenceStoreTestcase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RDBMSConfig {

    private static final Log log = LogFactory.getLog(RDBMSConfig.class);
    private static final String YAML_DATASOURCE_CONFIG_JDBC_URL = "          jdbcUrl:";
    private static final String YAML_DATASOURCE_CONFIG_USERNAME = "          username:";
    private static final String YAML_DATASOURCE_CONFIG_PASSWORD = "          password:";
    private static final String YAML_DATASOURCE_CONFIG_JDBC_DRIVER = "          driverClassName:";

    private static final String JDBC_DRIVER_CLASS_NAME_H2 = "org.h2.Driver";
    private static final String JDBC_DRIVER_CLASS_NAME_MYSQL = "com.mysql.jdbc.Driver";
    private static final String JDBC_DRIVER_CLASS_NAME_ORACLE = "oracle.jdbc.driver.OracleDriver";
    private static final String JDBC_DRIVER_CLASS_POSTGRES = "org.postgresql.Driver";
    private static final String JDBC_DRIVER_CLASS_MSSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static String url;
    private static String driverClassName;
    private static String username;
    private static String password;

    public static void createDatasource(String folderName) {
        RDBMSType type = RDBMSType.valueOf(System.getenv("DATABASE_TYPE"));
        username = System.getenv("DATABASE_USER");
        password = System.getenv("DATABASE_PASSWORD");
        String port = System.getenv("PORT");

        switch (type) {
            case MySQL:
                String connectionUrlMysql = "jdbc:mysql://{{container.ip}}:{{container.port}}/WSO2_ANALYTICS_DB" +
                        "?useSSL=false";
                url = connectionUrlMysql.replace("{{container.ip}}", getIpAddressOfContainer()).
                        replace("{{container.port}}", port);
                driverClassName = JDBC_DRIVER_CLASS_NAME_MYSQL;
                break;
            case H2:
                url = "jdbc:h2:./target/WSO2_ANALYTICS_DB";
                driverClassName = JDBC_DRIVER_CLASS_NAME_H2;
                username = "wso2carbon";
                password = "wso2carbon";
                break;
            case POSTGRES:
                String connectionUrlPostgres = "jdbc:postgresql://{{container.ip}}:{{container.port}}" +
                        "/WSO2_ANALYTICS_DB";
                url = connectionUrlPostgres.replace("{{container.ip}}", getIpAddressOfContainer()).
                        replace("{{container.port}}", port);
                driverClassName = JDBC_DRIVER_CLASS_POSTGRES;
                break;
            case ORACLE:
                String connectionUrlOracle = "jdbc:oracle:thin:@{{container.ip}}:{{container.port}}/XE";
                url = connectionUrlOracle.replace("{{container.ip}}", getIpAddressOfContainer()).
                        replace("{{container.port}}", port);
                driverClassName = JDBC_DRIVER_CLASS_NAME_ORACLE;
                break;
            case MSSQL:
                String connectionUrlMsSQL = "jdbc:sqlserver://{{container.ip}}:{{container.port}};" +
                        "databaseName=tempdb";
                url = connectionUrlMsSQL.replace("{{container.ip}}", getIpAddressOfContainer()).
                        replace("{{container.port}}", port);
                driverClassName = JDBC_DRIVER_CLASS_MSSQL;
                break;
        }

        RDBMSConfig.updateDeploymentYaml(folderName);

    }

    private static void updateDeploymentYaml(String folderName) {
        try (BufferedReader br = new BufferedReader(new FileReader("src" + File.separator + "test" + File.separator +
                "resources" + File.separator + "conf" + File.separator + "persistence" + File.separator + folderName
                + File.separator + "deployment-structure.yaml"));
             BufferedWriter bw = new BufferedWriter(new FileWriter("src" + File.separator + "test" + File.separator +
                     "resources" + File.separator + "conf" + File.separator + "persistence" + File.separator
                     + folderName + File.separator + "deployment.yaml"))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(YAML_DATASOURCE_CONFIG_JDBC_URL)) {
                    line = line.replace(YAML_DATASOURCE_CONFIG_JDBC_URL, YAML_DATASOURCE_CONFIG_JDBC_URL +
                            " " + url);
                }
                if (line.contains(YAML_DATASOURCE_CONFIG_USERNAME)) {
                    line = line.replace(YAML_DATASOURCE_CONFIG_USERNAME, YAML_DATASOURCE_CONFIG_USERNAME +
                            " " + username);
                }
                if (line.contains(YAML_DATASOURCE_CONFIG_PASSWORD)) {
                    line = line.replace(YAML_DATASOURCE_CONFIG_PASSWORD, YAML_DATASOURCE_CONFIG_PASSWORD +
                            " " + password);
                }
                if (line.contains(YAML_DATASOURCE_CONFIG_JDBC_DRIVER)) {
                    line = line.replace(YAML_DATASOURCE_CONFIG_JDBC_DRIVER, YAML_DATASOURCE_CONFIG_JDBC_DRIVER +
                            " " + driverClassName);
                }
                bw.write(line + "\n");
            }
        } catch (IOException e) {
            log.error("Error in getting configuration file ready for " + DBPersistenceStoreTestcase.class.getName());
        }
    }

    /**
     * Utility for get Docker running host
     *
     * @return docker host
     * @throws URISyntaxException if docker Host url is malformed this will throw
     */
    private static String getIpAddressOfContainer() {
        String ip = System.getenv("DOCKER_HOST_IP");
        String dockerHost = System.getenv("DOCKER_HOST");
        if (!StringUtils.isEmpty(dockerHost)) {
            try {
                URI uri = new URI(dockerHost);
                ip = uri.getHost();
            } catch (URISyntaxException e) {
                log.error("Error while getting the docker Host url." + e.getMessage(), e);
            }
        }
        return ip;
    }

    private enum RDBMSType {
        MySQL, H2, ORACLE, MSSQL, POSTGRES
    }
}




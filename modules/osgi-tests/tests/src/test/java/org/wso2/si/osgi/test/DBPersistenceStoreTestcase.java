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

package org.wso2.si.osgi.test;

import com.zaxxer.hikari.HikariDataSource;
import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.container.options.CarbonDistributionOption;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;
import org.wso2.carbon.kernel.CarbonServerInfo;
import org.wso2.carbon.streaming.integrator.core.internal.StreamProcessorDataHolder;
import org.wso2.carbon.streaming.integrator.core.persistence.DBPersistenceStore;
import org.wso2.si.osgi.test.util.RDBMSConfig;
import org.wso2.si.osgi.test.util.SiddhiAppUtil;
import org.wso2.si.osgi.test.util.TestConstants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.sql.DataSource;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class DBPersistenceStoreTestcase {

    @Inject
    protected BundleContext bundleContext;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private DataSourceService dataSourceService;

    private static final Logger log = LoggerFactory.getLogger(DBPersistenceStore.class);
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";
    private static final String TABLE_NAME = "PERSISTENCE_TABLE";
    private static final String SIDDHIAPP_NAME = "SiddhiAppPersistence";
    private static final String OJDBC6_OSGI_DEPENDENCY = "ojdbc6_12.1.0.1_atlassian_hosted_1.0.0.jar";

    private final String selectLastQuery = "SELECT siddhiAppName FROM " + TABLE_NAME + " WHERE siddhiAppName = ?";

    /**
     * Replace the existing deployment.yaml file with populated deployment.yaml file.
     */
    private Option copyCarbonYAMLOption() {
        Path carbonYmlFilePath;
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources",
                "conf", "persistence", "db", CARBON_YAML_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", CARBON_YAML_FILENAME));
    }

    /**
     * Copy the OJDBC OSGI dependency to lib folder of distribution
     */
    private Option copyOracleJDBCJar() {
        Path ojdbc6FilePath;
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        ojdbc6FilePath = Paths.get(basedir, "src", "test", "resources", "lib", OJDBC6_OSGI_DEPENDENCY);
        return copyFile(ojdbc6FilePath, Paths.get("lib", OJDBC6_OSGI_DEPENDENCY));
    }

    @Configuration
    public Option[] createConfiguration() {
        log.info("Running - " + this.getClass().getName());
        RDBMSConfig.createDatasource("db");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error in waiting for DataSources configuration file creation");
        }
        return new Option[]{
                copyCarbonYAMLOption(),
                copyOracleJDBCJar(),
                CarbonDistributionOption.copyOSGiLibBundle(maven(
                        "mysql", "mysql-connector-java").versionAsInProject()),
                CarbonDistributionOption.copyOSGiLibBundle(maven(
                        "org.postgresql", "postgresql").versionAsInProject()),
                CarbonDistributionOption.copyOSGiLibBundle(maven(
                        "com.microsoft.sqlserver", "mssql-jdbc").versionAsInProject()),
                carbonDistribution(Paths.get("target", "wso2si-test-" +
                        System.getProperty(TestConstants.STREAMING_INTEGRATION_VERSION)), "server")
        };
    }

    @Test
    public void testDBSystemPersistence() throws InterruptedException, DataSourceException, SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            final DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource("WSO2_ANALYTICS_DB");
            con = dataSource.getConnection();
            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                SiddhiManager siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
                return siddhiManager != null;
            });
            log.info("Running periodic persistence tests with database type " + con.getMetaData().
                    getDatabaseProductName().toLowerCase());
            SiddhiAppRuntime siddhiAppRuntime = SiddhiAppUtil.
                    createSiddhiApp(StreamProcessorDataHolder.getSiddhiManager());

            SiddhiAppUtil.sendDataToStream("WSO2", 500L, siddhiAppRuntime);
            SiddhiAppUtil.sendDataToStream("WSO2", 200L, siddhiAppRuntime);
            SiddhiAppUtil.sendDataToStream("WSO2", 300L, siddhiAppRuntime);
            SiddhiAppUtil.sendDataToStream("WSO2", 250L, siddhiAppRuntime);
            SiddhiAppUtil.sendDataToStream("WSO2", 150L, siddhiAppRuntime);

            log.info("Waiting for first time interval for state persistence");
            Awaitility.await().pollInterval(Duration.FIVE_SECONDS).atMost(2, TimeUnit.MINUTES).until(() -> {
                Connection connection = null;
                PreparedStatement statement = null;
                try {
                    connection = dataSource.getConnection();
                    statement = connection.prepareStatement(selectLastQuery);
                    statement.setString(1, siddhiAppRuntime.getName());
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        log.info(resultSet.getString("siddhiAppName") + " Revisions Found");
                        return true;
                    } else {
                        statement.close();
                        connection.close();
                        return false;
                    }
                } catch (SQLException e) {
                    if (connection != null) {
                        connection.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    return false;
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                }
            });
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                log.error("Error in closing connection to test datasource ", e);
            }
        }
    }

    @Test(dependsOnMethods = {"testDBSystemPersistence"})
    public void testRestoreFromDBSystem() throws InterruptedException, DataSourceException, SQLException {
        DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource("WSO2_ANALYTICS_DB");
        log.info("Waiting for second time interval for state persistence");
        Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
            Connection con = dataSource.getConnection();
            PreparedStatement stmt = con.prepareStatement(selectLastQuery);
            stmt.setString(1, "SiddhiAppPersistence");
            ResultSet resultSet = stmt.executeQuery();
            int count = 0;
            while (resultSet.next()) {
                count++;
                Assert.assertEquals(resultSet.getString("siddhiAppName"), SIDDHIAPP_NAME);
            }
            con.close();
            stmt.close();
            return count == 2;
        });
        SiddhiManager siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME);
        log.info("Restarting " + SIDDHIAPP_NAME + " and restoring last saved state");
        siddhiAppRuntime.shutdown();
        SiddhiAppRuntime newSiddhiAppRuntime = SiddhiAppUtil.
                createSiddhiApp(StreamProcessorDataHolder.getSiddhiManager());
        String revision = null;
        try {
            revision = newSiddhiAppRuntime.restoreLastRevision();
        } catch (CannotRestoreSiddhiAppStateException e) {
            Assert.fail("Restoring of Siddhi App Failed");
        }
        log.info("Siddhi App " + SIDDHIAPP_NAME + " successfully started and restored to " + revision + " revision");

        SiddhiAppUtil.sendDataToStream("WSO2", 280L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 150L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 200L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 270L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 280L, newSiddhiAppRuntime);

        Assert.assertEquals(SiddhiAppUtil.outputElementsArray, Arrays.asList("500", "500", "500", "500", "500",
                "300", "300", "280", "280", "280"));
    }

    @Test(dependsOnMethods = {"testRestoreFromDBSystem"})
    public void testPeriodicDBSystemPersistence() throws InterruptedException {

        Connection con = null;
        PreparedStatement stmt = null;
        SiddhiManager siddhiManager = null;
        try {
            DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource("WSO2_ANALYTICS_DB");
            con = dataSource.getConnection();
            log.info("Waiting for third time interval for state persistence");
            Thread.sleep(60000); //await() cannot be used because number of persisted revisions do not change

            stmt = con.prepareStatement(selectLastQuery);
            siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
            stmt.setString(1, siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME).getName());
            ResultSet resultSet = stmt.executeQuery();
            int count = 0;
            while (resultSet.next()) {
                count++;
                Assert.assertEquals(resultSet.getString("siddhiAppName"), SIDDHIAPP_NAME);
            }
            Assert.assertEquals(count, 2);
        } catch (SQLException e) {
            log.error("Error in processing query ", e);
        } catch (DataSourceException e) {
            log.error("Cannot establish connection to the data source ", e);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                log.error("Error in closing connection to test datasource ", e);
            }
        }
    }

    @Test(dependsOnMethods = {"testPeriodicDBSystemPersistence"})
    public void testPeriodicDBSystemPersistenceClear() {

        Connection con = null;
        PreparedStatement stmt = null;
        SiddhiManager siddhiManager = null;
        try {
            DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource("WSO2_ANALYTICS_DB");
            con = dataSource.getConnection();
            int count = 0;

            siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME);

            stmt = con.prepareStatement(selectLastQuery);
            stmt.setString(1, siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME).getName());
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                count++;
            }
            //check whether the data exists in the table
            if (count == 0) {
                Assert.fail("Already data does not exists in the database table");
            }

            siddhiAppRuntime.clearAllRevisions();

            resultSet = stmt.executeQuery();
            count = 0;
            while (resultSet.next()) {
                count++;
            }
            Assert.assertEquals(count, 0);

        } catch (SQLException e) {
            log.error("Error in processing query ", e);
        } catch (DataSourceException e) {
            log.error("Cannot establish connection to the data source ", e);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                siddhiManager.shutdown();
            } catch (SQLException e) {
                log.error("Error in closing connection to test datasource ", e);
            }
        }
    }
}

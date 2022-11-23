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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Duration;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.kernel.CarbonServerInfo;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.si.osgi.test.util.HTTPResponseMessage;
import org.wso2.si.osgi.test.util.TestConstants;
import org.wso2.si.osgi.test.util.TestUtil;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

/**
 * SiddhiAsAPI OSGI Tests.
 */

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class SiddhiAsAPITestcase {
    private static final Log logger = LogFactory.getLog(SiddhiAsAPITestcase.class);

    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";
    private static final String APP_NAME = "TestInvalidSiddhiApp";
    private static final String SIDDHI_EXTENSION = ".siddhi";

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private SiddhiAppRuntimeService siddhiAppRuntimeService;

    @Inject
    private EventStreamService eventStreamService;

    @Configuration
    public Option[] createConfiguration() {
        logger.info("Running - " + this.getClass().getName());
        return new Option[]{
                copyCarbonYAMLOption(),
                copySiddhiFileOption(),
                carbonDistribution(
                        Paths.get("target", "wso2si-test-" +
                                System.getProperty(TestConstants.STREAMING_INTEGRATION_VERSION)), "server")
                //CarbonDistributionOption.debug(5005)
        };
    }

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
                "conf", "persistence", "file", "default", CARBON_YAML_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", CARBON_YAML_FILENAME));
    }

    /**
     * Copy Siddhi file to deployment directory in runtime.
     */
    private Option copySiddhiFileOption() {
        Path carbonYmlFilePath;
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources", "deployment", "siddhi-files",
                APP_NAME + SIDDHI_EXTENSION);
        return copyFile(carbonYmlFilePath, Paths.get("wso2", "server", "deployment", "siddhi-files",
                APP_NAME + SIDDHI_EXTENSION));
    }

    /**
     * Siddhi App deployment related test cases
     */
    @Test
    public void testValidSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp1')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);
        Assert.assertEquals(httpResponseMessage.getContentType(), "application/json");
        TestUtil.waitForAppDeployment(siddhiAppRuntimeService, eventStreamService, "SiddhiApp1",
                Duration.ONE_MINUTE);

    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeployment"})
    public void testValidDuplicateSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp1')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App whih is already existing in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 409);
        logger.info(httpResponseMessage.getErrorContent());
    }

    @Test(dependsOnMethods = {"testValidDuplicateSiddhiAPPDeployment"})
    public void testInValidSiddhiAPPDeployment() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";

        String invalidBody = "@App:name('SiddhiApp2')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";
        logger.info("Deploying invalid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);

    }

    @Test(dependsOnMethods = {"testInValidSiddhiAPPDeployment"})
    public void testSiddhiAPPDeploymentWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "application/json";
        String method = "POST";

        String invalidBody = "@App:name('SiddhiApp2')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";

        logger.info("Deploying Siddhi App with invalid content type through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 415);

    }

    @Test(dependsOnMethods = {"testSiddhiAPPDeploymentWithInvalidContentType"})
    public void testSiddhiAPPDeploymentWithNoBody() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";

        logger.info("Deploying Siddhi App without request body through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }


    /**
     * Siddhi App update related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPDeploymentWithNoBody"})
    public void testValidNonExistSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";
        String body = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App which does not exists through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);
        TestUtil.waitForAppDeployment(siddhiAppRuntimeService, eventStreamService, "SiddhiApp3", Duration.ONE_MINUTE);

    }

    @Test(dependsOnMethods = {"testValidNonExistSiddhiAPPUpdate"})
    public void testValidAlreadyExistSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";
        String body = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "insert into BarStream;";

        logger.info("Deploying valid Siddhi App whih is already existing in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        TestUtil.waitForAppDeployment(siddhiAppRuntimeService, eventStreamService, "SiddhiApp3", Duration.TEN_SECONDS);

    }

    @Test(dependsOnMethods = {"testValidAlreadyExistSiddhiAPPUpdate"})
    public void testInValidSiddhiAPPUpdate() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "PUT";

        String invalidBody = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";
        logger.info("Deploying invalid Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }

    @Test(dependsOnMethods = {"testInValidSiddhiAPPUpdate"})
    public void testSiddhiAPPUpdateWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "application/json";
        String method = "PUT";

        String invalidBody = "@App:name('SiddhiApp3')\n" +
                "define stream FooStream (symbol string, price float, volume long);\n" +
                "\n" +
                "@source(type='inMemory', topic='symbol', @map(type='json'))Define stream BarStream " +
                "(symbol string, price float, volume long);\n" +
                "\n" +
                "from FooStream\n" +
                "select symbol, price, volume\n" +
                "";

        logger.info("Deploying Siddhi App with invalid content type through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(invalidBody, baseURI, path, contentType,
                method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 415);
    }

    /**
     * Siddhi App retrieval (individual) related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPUpdateWithInvalidContentType"})
    public void testValidSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving active Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrieval"})
    public void testValidSiddhiAPPRetrievalWithDifferntContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving active Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrievalWithDifferntContentType"})
    public void testNonExistSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp33";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving non exist Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRetrieval"})
    public void testInactiveSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving inactive Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /**
     * Siddhi App retrieval (collection) related test cases
     */
    @Test(dependsOnMethods = {"testInactiveSiddhiAPPRetrieval"})
    public void testAllSiddhiAPPRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testAllSiddhiAPPRetrieval"})
    public void testAllSiddhiAPPRetrievalWithContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving all Siddhi App names through REST API (different content type)");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);

    }

    /**
     * Siddhi App status retrieval related test cases
     */

    @Test(dependsOnMethods = {"testAllSiddhiAPPRetrievalWithContentType"})
    public void testNonExistSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp4/status";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving the status of a Siddhi App which not exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPStatusRetrieval"})
    public void testValidSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/status";
        String method = "GET";

        logger.info("Retrieving the status of a Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPStatusRetrieval"})
    public void testInactiveSiddhiAPPStatusRetrieval() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp/status";
        String method = "GET";

        logger.info("Retrieving the status of a Siddhi inactive App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testInactiveSiddhiAPPStatusRetrieval"})
    public void testiddhiAPPStatusRetrievalWithDifferentContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/status";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Retrieving the status of a Siddhi App which exists in server through REST API with different " +
                "content type");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /**
     * Siddhi App state backup related test cases
     */
    @Test(dependsOnMethods = {"testiddhiAPPStatusRetrievalWithDifferentContentType"})
    public void testValidSiddhiAPPBackup() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);

        Thread.sleep(2000);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPBackup"})
    public void testNonExistsSiddhiAPPBackup() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that does not exist in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistsSiddhiAPPBackup"})
    public void testValidSiddhiAPPBackupTake2() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Taking snapshot again for a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);

        Thread.sleep(2000);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPBackupTake2"})
    public void testSiddhiAPPBackupWithInvalidMethod() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API by invoking with " +
                "invalid method");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    /**
     * Siddhi App state restore related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPBackupWithInvalidMethod"})
    public void testValidSiddhiAPPRestoreToLastRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/restore";
        String method = "POST";
        String contentType = "text/plain";

        logger.info("Restoring the snapshot (last revision) of a Siddhi App that exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPRestoreToLastRevision"})
    public void testNonExistSiddhiAPPRestoreToLastRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2/restore";
        String method = "POST";

        logger.info("Restoring the snapshot (last revision) of a Siddhi App that does not exist in " +
                "server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPRestoreToLastRevision"})
    public void testSiddhiAPPRestoreToNonExistRevision() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/restore?revision=445534";
        String method = "POST";

        logger.info("Restoring the snapshot revison that does not exist of a Siddhi App through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 500);

    }

    @Test(dependsOnMethods = {"testSiddhiAPPRestoreToNonExistRevision"})
    public void testSiddhiAPPBackupWithInvalidContentType() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/backup";
        String method = "GET";
        String contentType = "application/json";

        logger.info("Taking snapshot of a Siddhi App that exists in server through REST API by invoking with " +
                "invalid method");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    /**
     * Siddhi App deletion related test cases
     */

    @Test(dependsOnMethods = {"testSiddhiAPPBackupWithInvalidContentType"})
    public void testNonExistSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp2";
        String method = "DELETE";
        String contentType = "text/plain";

        logger.info("Deleting Siddhi App which not exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
    }

    @Test(dependsOnMethods = {"testNonExistSiddhiAPPDeletion"})
    public void testValidSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1";
        String method = "DELETE";

        logger.info("Deleting valid Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        logger.info(httpResponseMessage.getMessage());
        TestUtil.waitForAppUndeployment(siddhiAppRuntimeService, "SiddhiApp1", Duration.TEN_SECONDS);

    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeletion"})
    public void testValidSiddhiAPPDeletionWithoutAppName() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "DELETE";

        logger.info("Deleting Siddhi App which without providing the app name in the url through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, null, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 405);
    }

    @Test(dependsOnMethods = {"testValidSiddhiAPPDeletionWithoutAppName"})
    public void testInactiveSiddhiAPPDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/TestInvalidSiddhiApp";
        String method = "DELETE";
        String contentType = "application/json";

        logger.info("Deleting inactive Siddhi App which exists in server through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        TestUtil.waitForAppUndeployment(siddhiAppRuntimeService, "TestInvalidSiddhiApp", Duration.TEN_SECONDS);
    }

    /**
     * Siddhi App retrieval after deletion related test cases
     */
    @Test(dependsOnMethods = {"testInactiveSiddhiAPPDeletion"})
    public void testSiddhiAPPRetrievalAfterDeletion() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
    }

    /**
     * Siddhi App without authentication
     */
    @Test(enabled = false)
    public void testSiddhiAPPWithoutAuthentication() {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                false, null, null);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 401);
    }

    /**
     * Siddhi App with wrong credentials
     */
    @Test(enabled = false)
    public void testSiddhiAPPWrongCredentials() {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String method = "GET";
        String contentType = "text/plain";

        logger.info("Retrieving all Siddhi App names through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest(null, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, "admin2");
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 401);
    }

    /**
     * Persistence clearing of existing Siddhi App
     */
    @Test (dependsOnMethods = {"testValidSiddhiAPPDeployment"})
    public void testSiddhiAppPersistenceStoreClear() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiApp1/revisions";
        String contentType = "text/plain";
        String method = "DELETE";

        logger.info("Deleting the persistence store through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 200);
        Assert.assertEquals(httpResponseMessage.getContentType(), "application/json");
        Thread.sleep(2000);

    }

    /**
     *
     * Persistence clearing of non-existing Siddhi App
     */
    @Test (dependsOnMethods = {"testValidSiddhiAPPDeployment"})
    public void testInvalidSiddhiAppPersistenceStoreClear() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps/SiddhiAppInvalid/revisions";
        String contentType = "text/plain";
        String method = "DELETE";

        logger.info("Deleting the persistence store through REST API");
        HTTPResponseMessage httpResponseMessage = sendHRequest("", baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 404);
        Assert.assertEquals(httpResponseMessage.getContentType(), "application/json");
        Thread.sleep(2000);

    }

    private HTTPResponseMessage sendHRequest(String body, URI baseURI, String path, String contentType,
                                             String methodType, Boolean auth, String userName, String password) {
        TestUtil testUtil = new TestUtil(baseURI, path, auth, false, methodType,
                contentType, userName, password);
        testUtil.addBodyContent(body);
        return testUtil.getResponse();
    }
}

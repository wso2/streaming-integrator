/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under thhe License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.si.osgi.test;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.kernel.CarbonServerInfo;
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
 * Config Reader Test Case.
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class ConfigReaderTestCase {
    private static final Logger logger = LoggerFactory.getLogger(ConfigReaderTestCase.class);
    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Configuration
    public Option[] createConfiguration() {
        logger.info("Running - " + this.getClass().getName());

        return new Option[]{
                copyCarbonYAMLOption(),
                carbonDistribution(
                        Paths.get("target", "wso2si-test-" +
                                System.getProperty(TestConstants.STREAMING_INTEGRATION_VERSION)), "server")
        };
    }

    /**
     * Replace the existing deployment.yaml file with populated deployment.yaml file.
     */
    private Option copyCarbonYAMLOption() {
        Path carbonYmlFilePath;
        String basedir = System.getProperty("basedir");
        logger.info("Base Directory " + basedir);
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources",
                "conf", "configuration", CARBON_YAML_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", CARBON_YAML_FILENAME));
    }

    @Test
    public void testConfigReaderFunctionalityTest1() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp1')\n" +
                "@Store(ref='store4')" +
                "define table FooTable (symbol string, price float, volume long);";

        logger.info("Store reference support -  Configuring RDBMS extension");
        HTTPResponseMessage httpResponseMessage = sendRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);
    }

    @Test
    public void testConfigReaderFunctionalityTest2() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp3')\n" +
                "@Store(ref='store1')" +
                "define table FooTable (symbol string, price float, volume long);";

        logger.info("Store reference support -  Configuring MongoDB extension");
        HTTPResponseMessage httpResponseMessage = sendRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 201);
    }

    @Test
    public void testConfigReaderFunctionalityTest3() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp4')\n" +
                "@Store(ref='store3')" +
                "define table FooTable (symbol string, price float, volume long);";

        logger.info("Store reference support -  Configuring unknown extension");
        HTTPResponseMessage httpResponseMessage = sendRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }

    @Test
    public void testConfigReaderFunctionalityTest4() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp5')\n" +
                "@Store(ref='store8')" +
                "define table FooTable (symbol string, price float, volume long);";

        logger.info("Store reference support -  Configuring undefined store");
        HTTPResponseMessage httpResponseMessage = sendRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }

    @Test
    public void testConfigReaderFunctionalityTest5() throws Exception {

        URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 9090));
        String path = "/siddhi-apps";
        String contentType = "text/plain";
        String method = "POST";
        String body = "@App:name('SiddhiApp6')\n" +
                "@Store(ref='store2')" +
                "define table FooTable (symbol string, price float, volume long);";

        logger.info("Store reference support -  Configuring with malformed store");
        HTTPResponseMessage httpResponseMessage = sendRequest(body, baseURI, path, contentType, method,
                true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
        Assert.assertEquals(httpResponseMessage.getResponseCode(), 400);
    }

    private HTTPResponseMessage sendRequest(String body, URI baseURI, String path, String contentType,
                                            String methodType, Boolean auth, String userName, String password) {
        TestUtil testUtil = new TestUtil(baseURI, path, auth, false, methodType,
                contentType, userName, password);
        testUtil.addBodyContent(body);
        return testUtil.getResponse();
    }

}

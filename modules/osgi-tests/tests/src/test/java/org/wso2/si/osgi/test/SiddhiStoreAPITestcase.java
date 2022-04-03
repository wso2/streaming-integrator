/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.si.osgi.test;

import com.google.gson.Gson;
import io.siddhi.core.event.Event;
import org.awaitility.Duration;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.kernel.CarbonServerInfo;
import org.wso2.carbon.siddhi.store.api.rest.ApiResponseMessage;
import org.wso2.carbon.siddhi.store.api.rest.model.ModelApiResponse;
import org.wso2.carbon.siddhi.store.api.rest.model.Query;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.msf4j.MicroservicesRegistry;
import org.wso2.si.osgi.test.util.HTTPResponseMessage;
import org.wso2.si.osgi.test.util.TestConstants;
import org.wso2.si.osgi.test.util.TestUtil;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static org.awaitility.Awaitility.await;
import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class SiddhiStoreAPITestcase {
    private static final Logger log = LoggerFactory.getLogger(SiddhiStoreAPITestcase.class);
    private static final String APP_NAME = "StoreApiTest";
    private static final String SIDDHI_EXTENSION = ".siddhi";
    private static final String STORE_API_BUNDLE_NAME = "org.wso2.carbon.siddhi.store.api.rest";
    private static final int HTTP_PORT = 7070;
    private static final String HOSTNAME = TestConstants.HOSTNAME_LOCALHOST;
    private static final String API_CONTEXT_PATH = "/stores/query";
    private static final String CONTENT_TYPE_JSON = TestConstants.CONTENT_TYPE_JSON;
    private static final String HTTP_METHOD_POST = TestConstants.HTTP_METHOD_POST;
    private static final String TABLENAME = "SmartHomeTable";
    private static final String DEFAULT_USER_NAME = TestConstants.DEFAULT_USERNAME;
    private static final String DEFAULT_PASSWORD = TestConstants.DEFAULT_PASSWORD;
    private final Gson gson = new Gson();

    @Inject
    private SiddhiAppRuntimeService siddhiAppRuntimeService;

    @Inject
    private EventStreamService eventStreamService;

    @Inject
    private MicroservicesRegistry microservicesRegistry;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] createConfiguration() {
        log.info("Running - " + this.getClass().getName());
        return new Option[]{
                copySiddhiFileOption(),
                carbonDistribution(
                        Paths.get("target", "wso2si-test-" +
                                System.getProperty("streaming.integration.version")), "server")
        };
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

    private Bundle getBundle(String name) {
        Bundle bundle = null;
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(name)) {
                bundle = b;
                break;
            }
        }
        Assert.assertNotNull(bundle, "Bundle should be available. Name: " + name);
        return bundle;
    }

    @Test
    public void testStoreApiBundle() {
        Bundle coreBundle = getBundle(STORE_API_BUNDLE_NAME);
        Assert.assertEquals(coreBundle.getState(), Bundle.ACTIVE);
    }

    private void testStoreAPI(String app, String strQuery, Event[] events, Response.Status expectedStatus, String
            expectedResponse)
            throws
            InterruptedException {
        Query query = new Query();
        query.setAppName(app);
        query.setQuery(strQuery);
        testQuery(gson.toJson(query), events, expectedStatus.getStatusCode(), expectedResponse);
    }

    private void testQuery(String body, Event[] events, int expectedResponseCode, String expectedResponse) throws
            InterruptedException {
        TestUtil.waitForAppDeployment(siddhiAppRuntimeService, eventStreamService, APP_NAME, Duration.TEN_SECONDS);
        for (Event event : events) {
            eventStreamService.pushEvent(APP_NAME, "SmartHomeData", event);
        }
        testHttpResponse(body, events, expectedResponseCode, expectedResponse, HOSTNAME, HTTP_PORT,
                Duration.TEN_SECONDS);
    }

    private void testHttpResponse(String body, Event[] inputEvents, int expectedResponseCode, String expectedResponse,
                                  String hostname, int port, Duration duration) {
        URI baseURI = URI.create(String.format("http://%s:%d", hostname, port));
        await().atMost(duration).until(() -> {
            HTTPResponseMessage httpResponseMessage =
                    sendHRequest(body, baseURI, API_CONTEXT_PATH, CONTENT_TYPE_JSON, HTTP_METHOD_POST,
                            true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
            if (expectedResponseCode == Response.Status.OK.getStatusCode()) {
                ModelApiResponse response =
                        gson.fromJson(httpResponseMessage.getSuccessContent().toString(), ModelApiResponse.class);
                if (httpResponseMessage.getResponseCode() == expectedResponseCode &&
                        httpResponseMessage.getContentType().equalsIgnoreCase(CONTENT_TYPE_JSON) &&
                        response.getRecords().size() == inputEvents.length) {
                    Assert.assertEquals(response.getRecords().size(), inputEvents.length);
                    return true;
                }
            } else {
                Assert.assertEquals(httpResponseMessage.getResponseCode(), expectedResponseCode);
                ApiResponseMessage response =
                        gson.fromJson(httpResponseMessage.getErrorContent().toString(), ApiResponseMessage.class);
                Assert.assertEquals(response.getMessage(), expectedResponse);
                return true;
            }
            return false;
        });
    }

    @Test(dependsOnMethods = "testStoreApiBundle")
    public void testSelectAllWithSuccessResponse() throws InterruptedException {
        Event[] events = new Event[]{
                new Event(System.currentTimeMillis(), new Object[]{
                        "recordId1", 10.34f, false, 1200, 300, 400, "2017-11-22"}),
                new Event(System.currentTimeMillis(), new Object[]{
                        "recordId2", 11.34f, false, 1300, 600, 100, "2017-11-23"}),
                new Event(System.currentTimeMillis(), new Object[]{
                        "recordId3", 12.34f, false, 1400, 500, 200, "2017-11-26"})};
        testStoreAPI(APP_NAME, "from " + TABLENAME + " select *", events, Response.Status.OK, null);

    }

    @Test(dependsOnMethods = "testSelectAllWithSuccessResponse")
    public void testConditionalSelectWithSuccessResponse() throws InterruptedException {
        Event[] events = new Event[]{
                new Event(System.currentTimeMillis(), new Object[]{
                        "recordId4", 10.34f, false, 1200, 300, 100, "2017-11-22"})
        };
        testStoreAPI(APP_NAME, "from " + TABLENAME + " select * having houseId==100 and recordId=='recordId4'",
                events, Response.Status.OK, null);
    }

    @Test(dependsOnMethods = "testConditionalSelectWithSuccessResponse")
    public void testNonExistentSiddhiApp() throws InterruptedException {
        testStoreAPI("SomeOtherStupidApp", "from " + TABLENAME + " select *", new Event[]{},
                Response.Status.NOT_FOUND, "Cannot find an active SiddhiApp with name: SomeOtherStupidApp");
    }

    @Test(dependsOnMethods = "testNonExistentSiddhiApp")
    public void testNonExistentTable() throws InterruptedException {
        testStoreAPI(APP_NAME, "from SomeOtherTable select *", new Event[]{}, Response.Status.INTERNAL_SERVER_ERROR,
                "Cannot query: SomeOtherTable is neither a table, aggregation or window");
    }

    @Test(dependsOnMethods = "testNonExistentTable")
    public void testEmptyAppName() throws InterruptedException {
        testStoreAPI("", "from " + TABLENAME + " select *", new Event[]{}, Response.Status.BAD_REQUEST,
                "Siddhi app name cannot be empty or null");
    }

    @Test(dependsOnMethods = "testNonExistentTable")
    public void testNullAppName() throws InterruptedException {
        testStoreAPI(null, "from " + TABLENAME + " select *", new Event[]{}, Response.Status.BAD_REQUEST,
                "Siddhi app name cannot be empty or null");
    }

    @Test(dependsOnMethods = "testNonExistentTable")
    public void testEmptyQuery() throws InterruptedException {
        testStoreAPI(APP_NAME, "", new Event[]{}, Response.Status.BAD_REQUEST,
                "Query cannot be empty or null");
    }

    @Test(dependsOnMethods = "testNonExistentTable")
    public void testNullQuery() throws InterruptedException {
        testStoreAPI(APP_NAME, null, new Event[]{}, Response.Status.BAD_REQUEST,
                "Query cannot be empty or null");
    }

    private HTTPResponseMessage sendHRequest(String body, URI baseURI, String path, String contentType,
                                             String methodType, Boolean auth, String userName, String password) {
        TestUtil testUtil = new TestUtil(baseURI, path, auth, false, methodType,
                contentType, userName, password);
        testUtil.addBodyContent(body);
        return testUtil.getResponse();
    }

}

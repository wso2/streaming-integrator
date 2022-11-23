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

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.kernel.CarbonServerInfo;
import org.wso2.carbon.siddhi.extensions.installer.core.config.mapping.ConfigMapper;
import org.wso2.carbon.siddhi.extensions.installer.core.config.mapping.models.ExtensionConfig;
import org.wso2.carbon.siddhi.extensions.installer.core.exceptions.ExtensionsInstallerException;
import org.wso2.carbon.siddhi.extensions.installer.core.models.enums.ExtensionInstallationStatus;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.si.osgi.test.util.HTTPResponseMessage;
import org.wso2.si.osgi.test.util.TestConstants;
import org.wso2.si.osgi.test.util.TestUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

/**
 * Extension Installation OSGI Tests.
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class ExtensionInstallationTestCase {
    private static final Log logger = LogFactory.getLog(ExtensionInstallationTestCase.class);

    private static final String SI_SERVER_HOST = "localhost";
    private static final int SI_SERVER_PORT = 9090;

    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";

    private static final List<String> installableExtensions = new ArrayList<>();
    private static final List<String> installedExtensions = new ArrayList<>();

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private BundleContext bundleContext;

    @Inject
    private SiddhiAppRuntimeService siddhiAppRuntimeService;

    @Inject
    private EventStreamService eventStreamService;

    @Configuration
    public Option[] createConfiguration() {
        logger.info("Running - " + this.getClass().getName());
        return new Option[]{
                copyCarbonYAMLOption(),
                carbonDistribution(
                        Paths.get("target", "wso2si-test-" +
                                System.getProperty(TestConstants.STREAMING_INTEGRATION_VERSION)), "server"),
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

    @Test
    public void testGetExtensionInstallationStatuses() throws ExtensionsInstallerException {
        // Read configurations
        String path = Paths.get("resources", "extensionsInstaller", "extensionDependencies.json")
                .toString();
        Map<String, ExtensionConfig> extensionConfigMap = ConfigMapper.loadAllExtensionConfigs(path);
        //removing redis and elasticsearch extensions from the test due to a maven central issue
        extensionConfigMap.remove("redis");
        extensionConfigMap.remove("elasticsearch");
        //removing gcs, hbase, cassandra and google-cloud-storage extensions from the test due to 3rd party dependency
        // version mismatch
        extensionConfigMap.remove("gcs");
        extensionConfigMap.remove("hbase");
        extensionConfigMap.remove("cassandra");
        extensionConfigMap.remove("google-cloud-storage");


        SoftAssert softAssert = new SoftAssert();
        for (String extensionName : extensionConfigMap.keySet()) {
            HTTPResponseMessage responseMessage = getExtensionInstallationStatus(extensionName);
            softAssert.assertEquals(responseMessage.getResponseCode(), 200,
                    "Failed while getting the extension installation status for extension: " + extensionName);
            if (responseMessage.getSuccessContent() != null) {
                Map<String, Object> responseMap =
                        new Gson().fromJson(responseMessage.getSuccessContent().toString(), Map.class);
                if (responseMap.get("manuallyInstall") == null) {
                    // Auto installable extension
                    String status = responseMap.get("extensionStatus").toString();
                    if (!ExtensionInstallationStatus.INSTALLED.toString().equals(status)) {
                        // Extension has not been installed yet
                        installableExtensions.add(extensionName);
                    }
                }
            }
        }
        softAssert.assertAll();
    }

    @Test(dependsOnMethods = "testGetExtensionInstallationStatuses")
    public void testInstallExtensions() throws IOException, InterruptedException {
        SoftAssert softAssert = new SoftAssert();
        for (String extensionName : installableExtensions) {
            logger.info("Installing extension: " + extensionName);
            HTTPResponseMessage responseMessage = installExtension(extensionName);
            logger.info("Installation call returned for extension: " + extensionName);
            softAssert.assertEquals(responseMessage.getResponseCode(), 200,
                    "Failed while installing the extension: " + extensionName);
            if (responseMessage.getSuccessContent() != null) {
                Map<String, Object> responseMap =
                        new Gson().fromJson(responseMessage.getSuccessContent().toString(), Map.class);
                if (responseMap.get("status") != null) {
                    softAssert.assertEquals(responseMap.get("status"), ExtensionInstallationStatus.INSTALLED.toString(),
                            "Installation was not complete for extension: " + extensionName);
                    if (ExtensionInstallationStatus.INSTALLED.toString().equals(responseMap.get("status"))) {
                        installedExtensions.add(extensionName);
                    }
                } else {
                    softAssert.fail("Erroneous installation status response for extension: " + extensionName);
                }
            }
        }

        copyInstalledExtensions();
        softAssert.assertAll();
    }

    private void copyInstalledExtensions() throws IOException {
        String basedir = System.getProperty("user.dir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }

        String testsDirectory = Paths.get(basedir).getParent().getParent().getParent().getParent().toString();
        String carbonHomePath = Paths.get(basedir).getParent().getParent().toString();

        String bundlesPath = Paths.get(carbonHomePath, ".bundles").toString();
        String jarsPath = Paths.get(carbonHomePath, ".jars").toString();
        String samplesLibPath = Paths.get(carbonHomePath, "samples", "sample-clients", "lib").toString();
        String installedExtensionsPath = Paths.get(testsDirectory, "target",
                "extension-installer", "installed-extensions").toString();
        String installedJarsPath = Paths.get(testsDirectory, "target",
                "extension-installer", "installed-jars").toString();
        String installedBundlesPath = Paths.get(testsDirectory, "target",
                "extension-installer", "installed-bundles").toString();
        String installedSamplesLibPath = Paths.get(testsDirectory, "target",
                "extension-installer", "installed-samples-lib").toString();

        // Copy installed extension dependencies to temporary directories
        new File(installedJarsPath).mkdirs();
        new File(installedBundlesPath).mkdirs();
        new File(installedSamplesLibPath).mkdirs();
        FileUtils.copyDirectory(new File(jarsPath), new File(installedJarsPath));
        FileUtils.copyDirectory(new File(bundlesPath), new File(installedBundlesPath));
        FileUtils.copyDirectory(new File(samplesLibPath), new File(installedSamplesLibPath));

        // Write names of installed extensions
        new File(installedExtensionsPath).mkdirs();
        FileWriter fileWriter =
                new FileWriter(Paths.get(installedExtensionsPath, "installed-extensions.txt").toString());
        for (String installedExtension : installedExtensions) {
            fileWriter.write(installedExtension);
            fileWriter.write("\n");
        }
        fileWriter.close();
    }

    private HTTPResponseMessage getExtensionInstallationStatus(String extensionName) {
        URI baseURI = URI.create(String.format("http://%s:%d", SI_SERVER_HOST, SI_SERVER_PORT));
        String path = String.format("/siddhi-extensions/%s/status", extensionName);
        String contentType = "text/plain";
        String method = "GET";

        return sendHRequest("", baseURI, path, contentType, method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
    }

    private HTTPResponseMessage installExtension(String extensionName) {
        URI baseURI = URI.create(String.format("http://%s:%d", SI_SERVER_HOST, SI_SERVER_PORT));
        String path = "/siddhi-extensions/" + extensionName;
        String contentType = "text/plain";
        String method = "POST";

        return sendHRequest("", baseURI, path, contentType, method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
    }

    private HTTPResponseMessage sendHRequest(String body, URI baseURI, String path, String contentType,
                                             String methodType, Boolean auth, String userName, String password) {
        TestUtil testUtil = new TestUtil(baseURI, path, auth, false, methodType,
                contentType, userName, password);
        testUtil.addBodyContent(body);
        return testUtil.getResponse();
    }
}

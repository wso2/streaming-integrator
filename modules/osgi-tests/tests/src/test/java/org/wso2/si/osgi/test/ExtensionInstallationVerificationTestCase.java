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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.kernel.CarbonServerInfo;
import org.wso2.carbon.siddhi.extensions.installer.core.models.enums.ExtensionInstallationStatus;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.si.osgi.test.util.HTTPResponseMessage;
import org.wso2.si.osgi.test.util.TestConstants;
import org.wso2.si.osgi.test.util.TestUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

/**
 * Extension Installation verification OSGI Tests.
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class ExtensionInstallationVerificationTestCase {

    private static final Log logger = LogFactory.getLog(ExtensionInstallationVerificationTestCase.class);

    private static final String SI_SERVER_HOST = "localhost";
    private static final int SI_SERVER_PORT = 9090;

    private static final String DEFAULT_USER_NAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";
    private static final String BASH_STARTUP_SCRIPT_FILENAME = "carbon.sh";
    private static final String BATCH_STARTUP_SCRIPT_FILENAME = "carbon.bat";
    private static final String LOCALHOST = "localhost";

    private static final List<String> installedExtensions = new ArrayList<>();

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private SiddhiAppRuntimeService siddhiAppRuntimeService;

    @Inject
    private EventStreamService eventStreamService;

    @Configuration
    public Option[] createConfiguration() {
        logger.info("Running - " + this.getClass().getName());
        List<Option> options = new ArrayList<>();
        options.addAll(copyBundlesAndJars());
        options.addAll(copyStartupScriptsOption());
        options.add(copyCarbonYAMLOption());
        options.add(carbonDistribution(
                Paths.get("target", "wso2si-test-" +
                        System.getProperty("streaming.integration.version")), "server"));

        return options.toArray(new Option[0]);
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

    private List<Option> copyStartupScriptsOption() {
        Path bashScriptPath;
        Path batchScriptPath;
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        bashScriptPath = Paths.get(basedir, "src", "test", "resources",
                "extension-installer", "startup-scripts", BASH_STARTUP_SCRIPT_FILENAME);
        batchScriptPath = Paths.get(basedir, "src", "test", "resources",
                "extension-installer", "startup-scripts", BATCH_STARTUP_SCRIPT_FILENAME);
        return new ArrayList<>(Arrays.asList(
                copyFile(bashScriptPath, Paths.get("wso2", "server", "bin", BASH_STARTUP_SCRIPT_FILENAME)),
                copyFile(batchScriptPath, Paths.get("wso2", "server", "bin", BATCH_STARTUP_SCRIPT_FILENAME))
        ));
    }

    private List<Option> copyBundlesAndJars() {
        List<Option> options = new ArrayList<>();

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }

        File installedBundlesDirectory = new File(Paths.get(basedir,
                 "target", "extension-installer", "installed-bundles").toString());
        File[] bundles = installedBundlesDirectory.listFiles();
        for (File bundle : bundles) {
            if (bundle.isFile()) {
                options.add(copyFile(bundle.toPath(), Paths.get(".bundles", bundle.getName())));
            }
        }

        File installedJarsDirectory = new File(Paths.get(basedir,
                "target", "extension-installer", "installed-jars").toString());
        File[] jars = installedJarsDirectory.listFiles();
        for (File jar : jars) {
            if (jar.isFile()) {
                options.add(copyFile(jar.toPath(), Paths.get(".jars", jar.getName())));
            }
        }

        File installedSamplesLibDirectory = new File(Paths.get(basedir,
                 "target", "extension-installer", "installed-samples-lib").toString());
        File[] sampleLibs = installedSamplesLibDirectory.listFiles();
        for (File jar : sampleLibs) {
            if (jar.isFile()) {
                options.add(copyFile(jar.toPath(), Paths.get("samples", "sample-clients", "lib", jar.getName())));
            }
        }

        return options;
    }

    private void readAndPopulateInstalledExtensions() throws IOException {
        String basedir = System.getProperty("user.dir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        String testsDirectory = Paths.get(basedir).getParent().getParent().getParent().getParent().toString();
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(Paths.get(testsDirectory,
                 "target", "extension-installer", "installed-extensions",
                "installed-extensions.txt").toString()));
        String line = reader.readLine();
        while (line != null) {
            installedExtensions.add(line);
            line = reader.readLine();
        }
        reader.close();
    }

    @Test
    public void testOccupancyOfCommonPortsAfterExtensionInstall() {
        SoftAssert softAssert = new SoftAssert();
        for (int port : TestConstants.COMMON_SI_PORTS) {
            softAssert.assertTrue(TestUtil.isPortOccupied(LOCALHOST, port),
                    "Common port: " + port + " of host: " + LOCALHOST + " is not occupied.");
        }
        softAssert.assertAll();
    }

    @Test(dependsOnMethods = {"testOccupancyOfCommonPortsAfterExtensionInstall"})
    public void testOccupancyOfServerRuntimePortsAfterExtensionInstall() {
        SoftAssert softAssert = new SoftAssert();
        for (int port : TestConstants.SERVER_RUNTIME_PORTS) {
            softAssert.assertTrue(TestUtil.isPortOccupied(LOCALHOST, port),
                    "Server runtime port: " + port + " of host: " + LOCALHOST + " is not occupied.");
        }
        softAssert.assertAll();
    }

    @Test(dependsOnMethods = {"testOccupancyOfServerRuntimePortsAfterExtensionInstall"})
    public void testGetExtensionInstallationStatusesAfterExtensionInstall() throws IOException {
        readAndPopulateInstalledExtensions();

        SoftAssert softAssert = new SoftAssert();
        for (String extensionName : installedExtensions) {
            HTTPResponseMessage responseMessage = getExtensionInstallationStatus(extensionName);
            softAssert.assertEquals(responseMessage.getResponseCode(), 200,
                    "Failed while getting the extension installation status for extension: " + extensionName);
            if (responseMessage.getSuccessContent() != null) {
                String responseString = responseMessage.getSuccessContent().toString();
                Map<String, Object> responseMap = new Gson().fromJson(responseString, Map.class);
                String status = responseMap.get("extensionStatus").toString();
                softAssert.assertEquals(status, ExtensionInstallationStatus.INSTALLED.toString(),
                        "Extension: " + extensionName + " was not installed.");
            } else {
                softAssert.fail("Erroneous installation status response for extension: " + extensionName);
            }
        }

        softAssert.assertAll();
        cleanupDirectories();
    }

    private HTTPResponseMessage getExtensionInstallationStatus(String extensionName) {
        URI baseURI = URI.create(String.format("http://%s:%d", SI_SERVER_HOST, SI_SERVER_PORT));
        String path = String.format("/siddhi-extensions/%s/status", extensionName);
        String contentType = "text/plain";
        String method = "GET";

        return sendHRequest("", baseURI, path, contentType, method, true, DEFAULT_USER_NAME, DEFAULT_PASSWORD);
    }

    private HTTPResponseMessage sendHRequest(String body, URI baseURI, String path, String contentType,
                                             String methodType, Boolean auth, String userName, String password) {
        TestUtil testUtil = new TestUtil(baseURI, path, auth, false, methodType,
                contentType, userName, password);
        testUtil.addBodyContent(body);
        return testUtil.getResponse();
    }

    private void cleanupDirectories() throws IOException {
        String basedir = System.getProperty("user.dir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        String testsDirectory = Paths.get(basedir).getParent().getParent().getParent().getParent().toString();
        String installedExtensionsPath = Paths.get(testsDirectory,
                "target", "extension-installer", "installed-extensions").toString();
        String installedJarsPath = Paths.get(testsDirectory,
                "target", "extension-installer", "installed-jars").toString();
        String installedBundlesPath = Paths.get(testsDirectory,
                "target", "extension-installer", "installed-bundles").toString();
        String installedSamplesLibPath = Paths.get(testsDirectory,
                "target", "extension-installer", "installed-samples-lib").toString();

        File file = new File(installedExtensionsPath);
        FileUtils.deleteDirectory(file);
        file = new File(installedJarsPath);
        FileUtils.deleteDirectory(file);
        file = new File(installedBundlesPath);
        FileUtils.deleteDirectory(file);
        file = new File(installedSamplesLibPath);
        FileUtils.deleteDirectory(file);
    }
}

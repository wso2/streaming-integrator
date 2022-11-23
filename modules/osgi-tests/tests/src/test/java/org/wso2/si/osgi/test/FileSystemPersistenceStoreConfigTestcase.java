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

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.streaming.integrator.core.internal.StreamProcessorDataHolder;
import org.wso2.si.osgi.test.util.SiddhiAppUtil;
import org.wso2.si.osgi.test.util.TestConstants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class FileSystemPersistenceStoreConfigTestcase {

    private static final Log log = LogFactory.getLog(FileSystemPersistenceStoreConfigTestcase.class);
    private static final String DEPLOYMENT_FILENAME = "deployment.yaml";
    private static final String PERSISTENCE_FOLDER = "siddhi-app-persistence";
    private static final String SIDDHIAPP_NAME = "SiddhiAppPersistence";

    @Inject
    protected BundleContext bundleContext;

    /**
     * Replace the existing deployment.yaml file with populated deployment.yaml file.
     */
    private Option copyCarbonYAMLOption() {
        Path carbonYmlFilePath;
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources", "conf", "persistence", "file",
                "configTest", DEPLOYMENT_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", DEPLOYMENT_FILENAME));
    }


    @Configuration
    public Option[] createConfiguration() {
        log.info("Running - " + this.getClass().getName());
        return new Option[]{
                copyCarbonYAMLOption(),
                carbonDistribution(Paths.get("target", "wso2si-test-" +
                        System.getProperty(TestConstants.STREAMING_INTEGRATION_VERSION)), "server")
        };
    }

    @Test
    public void testFileSystemPersistenceWithDefaultValues() throws InterruptedException {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            SiddhiManager siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
            return siddhiManager != null;
        });
        SiddhiAppRuntime siddhiAppRuntime = SiddhiAppUtil.createSiddhiApp(StreamProcessorDataHolder.getSiddhiManager());

        SiddhiAppUtil.sendDataToStream("WSO2", 500L, siddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 200L, siddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 300L, siddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 250L, siddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 150L, siddhiAppRuntime);

        final File[] file = {new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME)};
        Assert.assertEquals(file[0].exists() && file[0].isDirectory(), false, "No Folder Created");

        log.info("Waiting for first time interval for state persistence");
        Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
            file[0] = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);
            return file[0].exists() && file[0].isDirectory() && file[0].list().length == 1;
        });
        log.info("Waiting for second time interval for state persistence");
        Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
            file[0] = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);
            return file[0].exists() && file[0].isDirectory() && file[0].list().length == 2;
        });
        log.info("Waiting for third time interval for state persistence");
        Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
            file[0] = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);
            return file[0].exists() && file[0].isDirectory() && file[0].list().length == 3;
        });

        Assert.assertEquals(file[0].exists() && file[0].isDirectory(), true);
        Assert.assertEquals(file[0].list().length, 3, "There should be three revisions persisted");
        siddhiAppRuntime.shutdown();

    }
}

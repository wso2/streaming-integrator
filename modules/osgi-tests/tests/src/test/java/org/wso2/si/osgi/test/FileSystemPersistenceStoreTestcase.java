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
import io.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class FileSystemPersistenceStoreTestcase {

    private static final Log log = LogFactory.getLog(FileSystemPersistenceStoreTestcase.class);
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
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources",
                "conf", "persistence", "file", "default", DEPLOYMENT_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", DEPLOYMENT_FILENAME));
    }


    @Configuration
    public Option[] createConfiguration() {
        log.info("Running - " + this.getClass().getName());
        return new Option[]{
                copyCarbonYAMLOption(),
                carbonDistribution(
                        Paths.get("target", "wso2si-test-" +
                                System.getProperty("project.version")), "server")
        };
    }

    @Test
    public void testFileSystemPersistence() throws InterruptedException {
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

        Assert.assertEquals(file[0].exists() && file[0].isDirectory(), true);
        Assert.assertEquals(file[0].list().length, 1, "There should be one revision persisted");

    }

    @Test(dependsOnMethods = {"testFileSystemPersistence"})
    public void testRestoreFromFileSystem() throws InterruptedException {
        log.info("Waiting for second time interval for state persistence");
        Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
            File file = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);
            return file.exists() && file.isDirectory() && file.list().length == 2;
        });

        SiddhiManager siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME);
        log.info("Shutting Down SiddhiApp");
        siddhiAppRuntime.shutdown();
        log.info("Creating New SiddhiApp with Same Name");
        SiddhiAppRuntime newSiddhiAppRuntime = SiddhiAppUtil.
                createSiddhiApp(StreamProcessorDataHolder.getSiddhiManager());
        try {
            newSiddhiAppRuntime.restoreLastRevision();
        } catch (CannotRestoreSiddhiAppStateException e) {
            Assert.fail("Restoring of Siddhi App Failed");
        }

        SiddhiAppUtil.sendDataToStream("WSO2", 280L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 150L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 200L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 270L, newSiddhiAppRuntime);
        SiddhiAppUtil.sendDataToStream("WSO2", 280L, newSiddhiAppRuntime);

        Assert.assertEquals(SiddhiAppUtil.outputElementsArray, Arrays.asList("500", "500", "500", "500", "500",
                "300", "300", "280", "280", "280"));
    }

    @Test(dependsOnMethods = {"testRestoreFromFileSystem"})
    public void testFileSystemPeriodicPersistence() throws InterruptedException {
        File file = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);
        log.info("Waiting for third time interval for state persistence");
        Thread.sleep(60000); //await() cannot be used because number of persisted revisions do not change

        Assert.assertEquals(file.exists() && file.isDirectory(), true);
        Assert.assertEquals(file.list().length, 2, "There should be two revisions persisted");
    }

    @Test(dependsOnMethods = {"testFileSystemPeriodicPersistence"})
    public void testFileSystemClearPeriodicPersistence() {
        File file = new File(PERSISTENCE_FOLDER + File.separator + SIDDHIAPP_NAME);

        SiddhiManager siddhiManager = StreamProcessorDataHolder.getSiddhiManager();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(SIDDHIAPP_NAME);

        if (file.list().length == 0) {
            Assert.fail("File revisions are already deleted");
        }

        log.info("Deleting all the revisions of the persistence store of Siddhi App : " + SIDDHIAPP_NAME);
        siddhiAppRuntime.clearAllRevisions();
        Assert.assertEquals(file.list().length, 0, "All the revisions should be deleted");
        siddhiAppRuntime.shutdown();
    }

}

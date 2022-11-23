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
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.si.osgi.test.util.TestConstants;
import org.wso2.si.osgi.test.util.TestUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

/**
 * Streaming Integrator opened port OSGI Tests.
 */

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class StreamingIntegratorOpenedPortTestcase {
    private static final Log logger = LogFactory.getLog(StreamingIntegratorOpenedPortTestcase.class);

    private static final String CARBON_YAML_FILENAME = "deployment.yaml";
    private static final String LOCALHOST = "localhost";

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
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources",
                "conf", "persistence", "file", "default", CARBON_YAML_FILENAME);
        return copyFile(carbonYmlFilePath, Paths.get("conf", "server", CARBON_YAML_FILENAME));
    }

    @Test
    public void testOccupancyOfCommonPorts() {
        SoftAssert softAssert = new SoftAssert();
        for (int port : TestConstants.COMMON_SI_PORTS) {
            softAssert.assertTrue(TestUtil.isPortOccupied(LOCALHOST, port),
                    "Common port: " + port + " of host: " + LOCALHOST + " is not occupied.");
        }
        softAssert.assertAll();
    }

    @Test
    public void testOccupancyOfServerRuntimePorts() {
        SoftAssert softAssert = new SoftAssert();
        for (int port : TestConstants.SERVER_RUNTIME_PORTS) {
            softAssert.assertTrue(TestUtil.isPortOccupied(LOCALHOST, port),
                    "Server runtime port: " + port + " of host: " + LOCALHOST + " is not occupied.");
        }
        softAssert.assertAll();
    }
}

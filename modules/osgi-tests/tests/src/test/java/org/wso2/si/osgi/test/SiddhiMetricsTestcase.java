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
import io.siddhi.core.event.Event;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import io.siddhi.core.util.EventPrinter;
import io.siddhi.core.util.SiddhiTestHelper;
import io.siddhi.core.util.statistics.metrics.Level;
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
import org.wso2.carbon.metrics.core.MetricManagementService;
import org.wso2.carbon.metrics.core.MetricService;
import org.wso2.carbon.metrics.core.jmx.MetricsMXBean;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.si.osgi.test.util.TestConstants;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.wso2.carbon.container.options.CarbonDistributionOption.carbonDistribution;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

/**
 * SiddhiAsAPI Metrics Tests.
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class SiddhiMetricsTestcase {
    private static final Logger log = LoggerFactory.getLogger(SiddhiMetricsTestcase.class);

    private static final String LOAD_AVG_MBEAN_NAME = "org.wso2.carbon.metrics:name=jvm.os.system.load.average";
    private static final String SYSTEM_CPU_MBEAN_NAME = "org.wso2.carbon.metrics:name=jvm.os.cpu.load.system";
    private static final String PROCESS_CPU_MBEAN_NAME = "org.wso2.carbon.metrics:name=jvm.os.cpu.load.process";
    private static final String MEMORY_USAGE_MBEAN_NAME = "org.wso2.carbon.metrics:name=jvm.memory.heap.usage";
    private static final String CARBON_YAML_FILENAME = "deployment.yaml";
    private static final String APP_NAME = "MetricsTestApp2";
    private static final String SIDDHI_EXTENSION = ".siddhi";

    private AtomicInteger eventCount;
    private boolean eventArrived;

    @Inject
    private BundleContext bundleContext;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private MetricService metricService;

    @Inject
    private SiddhiAppRuntimeService siddhiAppRuntimeService;

    @Inject
    private MetricManagementService metricManagementService;

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

    @Configuration
    public Option[] createConfiguration() {
        log.info("Running - " + this.getClass().getName());
        return new Option[]{
                copyCarbonYAMLOption(),
                copySiddhiFileOption(),
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
                "conf", "metrics", CARBON_YAML_FILENAME);
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

    @Test
    public void testMetricsCoreBundle() {
        Bundle coreBundle = getBundle("org.wso2.carbon.metrics.core");
        Assert.assertEquals(coreBundle.getState(), Bundle.ACTIVE);
    }

    private void testMBean(String mBeanName) throws Exception {

        SiddhiAppRuntime siddhiAppRuntime = siddhiAppRuntimeService.getActiveSiddhiAppRuntimes().get("MetricsTestApp2");
        siddhiAppRuntime.setStatisticsLevel(Level.DETAIL);

        siddhiAppRuntime.addCallback("outputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                eventArrived = true;
                for (Event event : events) {
                    eventCount.incrementAndGet();
                    Assert.assertTrue("IBM".equals(event.getData(0)) || "WSO2".equals(event.getData(0)));
                }
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");

        MetricsMXBean metricsMXBean = null;
        try {
            ObjectName n = new ObjectName(mBeanName);

            metricsMXBean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), n, MetricsMXBean.class);
        } catch (MalformedObjectNameException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(metricsMXBean);

        inputHandler.send(new Object[]{"WSO2", 55.6f, 100});
        inputHandler.send(new Object[]{"IBM", 75.6f, 100});
        SiddhiTestHelper.waitForEvents(1000, 3, eventCount, 20000);

        Assert.assertTrue(eventArrived);
        Assert.assertEquals(eventCount.get(), 3);
    }


    @Test(dependsOnMethods = "testMetricsCoreBundle")
    public void testMetricsWithMemoryUsageMbeanName() throws Exception {
        eventArrived = false;
        eventCount = new AtomicInteger(0);
        testMBean(MEMORY_USAGE_MBEAN_NAME);
    }

    @Test(dependsOnMethods = "testMetricsWithMemoryUsageMbeanName")
    public void testMetricsProcessCpuMbeanName() throws Exception {
        eventArrived = false;
        eventCount = new AtomicInteger(0);
        testMBean(PROCESS_CPU_MBEAN_NAME);
    }

    @Test(dependsOnMethods = "testMetricsProcessCpuMbeanName")
    public void testMetricsSystemCpuMbeanName() throws Exception {
        eventArrived = false;
        eventCount = new AtomicInteger(0);
        testMBean(SYSTEM_CPU_MBEAN_NAME);
    }

    @Test(dependsOnMethods = "testMetricsSystemCpuMbeanName")
    public void testMetricsLoadAvgMbeanName() throws Exception {
        eventArrived = false;
        eventCount = new AtomicInteger(0);
        testMBean(LOAD_AVG_MBEAN_NAME);
    }
}

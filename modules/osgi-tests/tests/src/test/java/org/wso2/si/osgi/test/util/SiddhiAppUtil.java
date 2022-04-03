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

package org.wso2.si.osgi.test.util;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import io.siddhi.core.util.EventPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class SiddhiAppUtil {

    private static final Log log = LogFactory.getLog(SiddhiAppUtil.class);

    private static final String SIDDHIAPP_STREAM = "@App:name('SiddhiAppPersistence')" +
            "define stream FooStream (symbol string, volume long); ";

    private static final String SIDDHIAPP_SOURCE = "@source(type='inMemory', topic='symbol'," +
            " @map(type='json'))" +
            "Define stream BarStream (symbol string, max long);";

    private static final String SIDDHIAPP_QUERY = "" +
            "@info(name = 'query1')  " +
            "from FooStream#window.length(5) " +
            "select symbol, max(volume) as max " +
            "group by symbol " +
            "insert into BarStream ;";

    public static List<String> outputElementsArray = new ArrayList<String>(10);

    public static SiddhiAppRuntime createSiddhiApp(SiddhiManager siddhiManager) throws InterruptedException {
        log.info(SIDDHIAPP_STREAM + SIDDHIAPP_SOURCE + SIDDHIAPP_QUERY);
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.
                createSiddhiAppRuntime(SIDDHIAPP_STREAM + SIDDHIAPP_SOURCE + SIDDHIAPP_QUERY);
        siddhiAppRuntime.start();
        siddhiAppRuntime.addCallback("BarStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                SiddhiAppUtil.outputElementsArray.add(events[0].getData(1).toString());
            }
        });

        return siddhiAppRuntime;
    }

    public static void sendDataToStream(String name, long value, SiddhiAppRuntime siddhiAppRuntime)
            throws InterruptedException {
        InputHandler fooStream = siddhiAppRuntime.getInputHandler("FooStream");
        fooStream.send(new Object[]{name, value});
        Thread.sleep(500);
    }

}

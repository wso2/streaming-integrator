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

import io.netty.handler.codec.http.HttpMethod;
import io.siddhi.core.SiddhiAppRuntime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Duration;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.streaming.integrator.common.SiddhiAppRuntimeService;
import org.wso2.msf4j.MicroservicesRegistry;

import static org.awaitility.Awaitility.await;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.net.URLConnection.guessContentTypeFromName;


/**
 * Util class for test cases.
 */
public class TestUtil {
    private static final String LINE_FEED = "\r\n";
    private static final String CHARSET = "UTF-8";
    private static final Log logger = LogFactory.getLog(TestUtil.class);
    private HttpURLConnection connection = null;
    private OutputStream outputStream = null;
    private PrintWriter writer = null;
    private String boundary = null;

    public TestUtil(URI baseURI, String path, Boolean auth, Boolean keepAlive, String methodType,
                    String contentType, String userName, String password) {
        try {
            URL url = baseURI.resolve(path).toURL();
            boundary = "---------------------------" + currentTimeMillis();

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Charset", CHARSET);
            connection.setRequestMethod(methodType);
            setHeader("HTTP_METHOD", methodType);
            if (keepAlive) {
                connection.setRequestProperty("Connection", "Keep-Alive");
            }
            if (contentType != null) {
                if (contentType.equals("multipart/form-data")) {
                    setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
                } else {
                    setHeader("Content-Type", contentType);
                }
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            if (auth) {
                connection.setRequestProperty("Authorization",
                        "Basic " + java.util.Base64.getEncoder().
                                encodeToString((userName + ":" + password).getBytes()));
            }
            if (methodType.equals(HttpMethod.POST.name()) || methodType.equals(HttpMethod.PUT.name())
                    || methodType.equals(HttpMethod.DELETE.name())) {
                connection.setDoOutput(true);
                outputStream = connection.getOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET),
                        true);
            }
        } catch (IOException e) {
            handleException("IOException occurred while running the HttpsSourceTestCaseForSSL", e);
        }
    }

    public static void waitForAppDeployment(SiddhiAppRuntimeService runtimeService,
                                            EventStreamService streamService, String appName, Duration atMost) {
        await().atMost(atMost).until(() -> {
            SiddhiAppRuntime app = runtimeService.getActiveSiddhiAppRuntimes().get(appName);
            if (app != null) {
                List<String> streams = streamService.getStreamNames(appName);
                if (!streams.isEmpty()) {
                    return true;
                }
            }
            return false;
        });
    }

    public static void waitForAppUndeployment(SiddhiAppRuntimeService runtimeService, String appName,
                                              Duration atMost) {
        await().atMost(atMost).until(() -> {
            SiddhiAppRuntime app = runtimeService.getActiveSiddhiAppRuntimes().get(appName);
            return app == null;
        });
    }

    public static void waitForMicroServiceDeployment(MicroservicesRegistry microservicesRegistry, String basePath,
                                                     Duration duration) {
        await().atMost(duration).until(() -> {
            Optional<Map.Entry<String, Object>> entry = microservicesRegistry.getServiceWithBasePath(basePath);
            return entry.isPresent();
        });
    }

    public HttpURLConnection getConnection() {
        return this.connection;
    }

    public void addBodyContent(String body) {
        if (body != null && !body.isEmpty()) {
            writer.write(body);
            writer.close();
        }
    }

    public void addFormField(final String name, final String value) {
        writer.append("--").append(boundary).append(LINE_FEED)
                .append("Content-Disposition: form-data; name=\"").append(name)
                .append("\"").append(LINE_FEED)
                .append("Content-Type: text/plain; charset=").append(CHARSET)
                .append(LINE_FEED).append(LINE_FEED).append(value).append(LINE_FEED);
    }

    public void addFilePart(final String fieldName, final File uploadFile)
            throws IOException {
        final String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED)
                .append("Content-Disposition: form-data; name=\"")
                .append(fieldName).append("\"; filename=\"").append(fileName)
                .append("\"").append(LINE_FEED).append("Content-Type: ")
                .append(guessContentTypeFromName(fileName)).append(LINE_FEED)
                .append("Content-Transfer-Encoding: binary").append(LINE_FEED)
                .append(LINE_FEED);

        writer.flush();
        outputStream.flush();
        try (final FileInputStream inputStream = new FileInputStream(uploadFile)) {
            final byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        writer.append(LINE_FEED);
        writer.flush();
    }

    public HTTPResponseMessage getResponse() {
        assert connection != null;
        String successContent = null;
        String errorContent = null;
        if (writer != null) {
            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();
        }
        try {
            if (connection.getResponseCode() >= 400) {
                errorContent = readErrorContent();
            } else {
                successContent = readSuccessContent();
            }
            return new HTTPResponseMessage(connection.getResponseCode(),
                    connection.getContentType(), connection.getResponseMessage(), successContent, errorContent);
        } catch (IOException e) {
            handleException("IOException occurred while running the HttpsSourceTestCaseForSSL", e);
        } finally {
            connection.disconnect();
        }
        return new HTTPResponseMessage();
    }

    private String readSuccessContent() throws IOException {
        StringBuilder sb = new StringBuilder("");
        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()))) {
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
        }
        return sb.toString();
    }

    private String readErrorContent() throws IOException {
        StringBuilder sb = new StringBuilder("");
        String line;
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(errorStream))) {
                while ((line = in.readLine()) != null) {
                    sb.append(line + "\n");
                }
            }
        }
        return sb.toString();
    }

    private void setHeader(String key, String value) {
        if (key != null && value != null) {
            connection.setRequestProperty(key, value);
        }
    }

    public static boolean isPortOccupied(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            // Port is occupied
            return true;
        } catch (IOException e) {
            // Port is not occupied
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    private void handleException(String msg, Exception ex) {
        logger.error(msg, ex);
    }

}

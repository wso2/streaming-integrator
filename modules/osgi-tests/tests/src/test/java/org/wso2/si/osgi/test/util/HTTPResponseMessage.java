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


public class HTTPResponseMessage {

    private int responseCode;
    private String contentType;
    private String message;
    private Object successContent;
    private Object errorContent;

    public HTTPResponseMessage(int responseCode, String contentType, String message) {
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.message = message;
    }

    public HTTPResponseMessage(int responseCode, String contentType, String message, Object successContent, Object
            errorContent) {
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.message = message;
        this.successContent = successContent;
        this.errorContent = errorContent;
    }

    public HTTPResponseMessage() {
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object getSuccessContent() {
        return successContent;
    }

    public void setSuccessContent(Object content) {
        this.successContent = content;
    }

    public Object getErrorContent() {
        return errorContent;
    }

    public void setErrorContent(Object errorContent) {
        this.errorContent = errorContent;
    }
}

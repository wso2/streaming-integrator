<!--
  ~  Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
  ~
  ~  WSO2 Inc. licenses this file to you under the Apache License,
  ~  Version 2.0 (the "License"); you may not use this file except
  ~  in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing,
  ~  software distributed under the License is distributed on an
  ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~  KIND, either express or implied.  See the License for the
  ~  specific language governing permissions and limitations
  ~  under the License.
  -->
  
# Streaming Integrator

[![Jenkins Build Status](https://wso2.org/jenkins/view/wso2-dependencies/job/products/job/streaming-integrator/badge/icon)](https://wso2.org/jenkins/view/wso2-dependencies/job/products/job/streaming-integrator/)
  [![GitHub Release](https://img.shields.io/github/release/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/releases/)
  [![GitHub Release Date](https://img.shields.io/github/release-date/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/releases)
  [![GitHub Open Issues](https://img.shields.io/github/issues-raw/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/commits/master)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/commits/master)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Introduction

WSO2 Streaming Integrator is an open-source streaming data processing engine which lets users integrate streaming data and/or take action based on streaming data.

## What does Streaming Integrator do?

WSO2 Streaming Integration has the capability of consuming streaming data, apply stream processing techniques to process them and Integrate the process data with one or more destinations and/or trigger integration

![Streaming Integrator/ Workflow](docs/images/streaming-integrator.png)

## Download

The Streaming Integrator is currently on development stage so please follow the [How to build]() section to build the streaming integrator from the source.
<!-- Please download the latest WSO2 Streaming Integrator release from [here]()  -->

## Building from the Source

Please follow the steps mentioned below to build the WSO2 Streaming Integrator from source.

1. Clone or download the source code from this repository.
2. Run the `mvn clean install` from the root directory of the repository
3. The generated Streaming Integrator distribution can be found at `streaming-integrator/modules/distribution/target/-streaming-integrator-<version>.zip`

## Getting Started

Get started with Streaming integrator in a few minutes by following [Streaming Integrator Quick Start Guide](https://docs.wso2.com/display/SP4xx/Quick+Start+Guide)

## Deploy in Docker

WSO2 Streaming Integrator has a docker distribution so that it can be deployed in any container-orchestration system.
The docker image can be build from the source or can be download directly from the docker_hub.

### Build the docker image

To build the docker image from the source the host machine should have docker installed and run the `mvn clean install -Ddocker.skip=false` from the root directory.

### Get from docker hub

Please use following command to get the docker image from the docker hub.

```bash
docker pull wso2/streaming-integrator
```

## Deploy in kubernetes

WSO2 Streaming Integrator can be deployed in a Kubernetes cluster using Siddhi Operator. For more details please refer to [Installing the Streaming Integrator Using Kubernetes](https://docs.wso2.com/display/INSTALL/Installing+Enterprise+Integrator+Using+Kubernetes)

## Support

We are committed to ensuring that your enterprise middleware deployment is completely supported from evaluation to production. Our unique approach ensures that all support leverages our open development methodology and is provided by the very same engineers who build the technology.

For more details and to take advantage of this unique opportunity please visit our [support site](http://wso2.com/support).


## Reporting Issues

We encourage you to report issues, documentation faults and feature requests regarding WSO2 Streaming integrator through the [WSO2 SI Issue Tracker](https://github.com/wso2/streaming-integrator/issues).

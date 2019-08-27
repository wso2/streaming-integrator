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
  [![GitHub Release](https://img.shields.io/github/release-pre/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/releases/)
  [![GitHub Release Date](https://img.shields.io/github/release-date-pre/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/releases)
  [![GitHub Open Issues](https://img.shields.io/github/issues-raw/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/commits/master)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/wso2/streaming-integrator.svg)](https://github.com/wso2/streaming-integrator/commits/master)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

WSO2 Streaming Integrator(SI) is a streaming data processing server which lets users integrate streaming data and take action based on streaming data. 

WSO2 SI can be effectively used for, 
- Realtime ETL with files, DBs, SaaS apps, HTTP endpoints, etc.
- Work with streaming messaging systems such as Kafka and NATS
- Streaming data Integration 
- Execute complex integrations based on streaming data with WSO2 Micro Integrator

WSO2 SI is powered by [Siddhi.io](https://siddhi.io/), a well-known cloud native open source stream processing engine. Siddhi lets users write complex stream processing logics using a SQL-like language referred to as [SiddhiQL](https://siddhi.io/en/v5.0/docs/). Users can aggregate, transform, enrich, analyze, cleanse and correlate streams of data on the fly using Siddhi queries and constructs. 

WSO2 SI lets users connect to any data source with any destination regardless of the different protocols, data formats that different endpoints use. The SI store API provides the capability to fetch stored and aggregated data kept in-memory and in DBs via a REST API on demand using ad-hoc quarries.

The [SI tooling](https://github.com/wso2/streaming-integrator-tooling) provides a web based IDE which allows users to build Siddhi apps with a drag-and-drop graphical editor or a streaming SQL editor. It facilitates users to test their siddhi apps with capability to simulate data streams and to debug Siddhi queries. The deployed siddhi apps can be directly deployed in VM via the IDE or export as a docker image, or K8s artifacts that can be used with Siddhi K8s operator.

SI has native support for Kubernetes with a [K8s Operator] (https://siddhi.io/en/v5.1/docs/siddhi-as-a-kubernetes-microservice/) designed to provide a convenient way of deploying SI on a K8s. SI has a very simple deployment architecture, the users can achieve high availability with zero data loss with two nodes of SI.

Integration flows deployed in [WSO2 Micro Integration(MI)] (https://github.com/wso2/micro-integrator) can be invoked directly by SI in a seamless manner using low latency RPC, this allows users to build robust data processing and integration pipelines by combining powerful streaming and integration capabilities. 

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

Please refer the below guides to get started with the Streaming Integrator

* [Quick Start Guide](): Step by step guide to get your first app running in less than 5 mins

* [Streaming Integrator 101](): 30 mins guide to explore the end to end development lifecycle of Streaming Integrator

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

WSO2 Streaming Integrator can be deployed in a Kubernetes cluster using Siddhi Operator. 
* [Siddhi operator](https://github.com/siddhi-io/siddhi-operator) enables the deployment of Siddhi apps directly in your kubernetes cluster using a kubernetes custom resource.
For more details please refer to [Installing the Streaming Integrator Using Kubernetes](https://docs.wso2.com/display/INSTALL/Installing+Enterprise+Integrator+Using+Kubernetes)

## Support

We are committed to ensuring that your enterprise middleware deployment is completely supported from evaluation to production. Our unique approach ensures that all support leverages our open development methodology and is provided by the very same engineers who build the technology.

For more details and to take advantage of this unique opportunity please visit our [support site](http://wso2.com/support).


## Reporting Issues

We encourage you to report issues, documentation faults and feature requests regarding WSO2 Streaming integrator through the [WSO2 SI Issue Tracker](https://github.com/wso2/streaming-integrator/issues).

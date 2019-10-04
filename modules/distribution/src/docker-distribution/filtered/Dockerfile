# ---------------------------------------------------------------------------
#  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
#  WSO2 Inc. licenses this file to you under the Apache License,
#  Version 2.0 (the "License"); you may not use this file except
#  in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied. See the License for the
#  specific language governing permissions and limitations
#  under the License.
# ---------------------------------------------------------------------------

# set base Docker image to AdoptOpenJDK Alpine Docker image
FROM adoptopenjdk/openjdk8:jre8u212-b03-alpine
LABEL maintainer="WSO2 Docker Maintainers <dev@wso2.org>"

# docker image build arguments for user/group configurations
ARG USER=wso2carbon
ARG USER_ID=802
ARG USER_GROUP=wso2
ARG USER_GROUP_ID=802
ARG USER_HOME=/home/${USER}
# docker image build arguments for wso2 product installation
ARG STREAMING_INTEGRATOR_NAME=wso2si
ARG STREAMING_INTEGRATOR_HOME=${USER_HOME}/${STREAMING_INTEGRATOR_NAME}

# create the user and group
RUN \
    addgroup --system -g ${USER_GROUP_ID} ${USER_GROUP} \
    && adduser --system --home ${USER_HOME} -g ${USER_GROUP_ID} -u ${USER_ID} ${USER}

# copy the wso2 streaming intergator product distribution to user's home directory
COPY --chown=wso2carbon:wso2 ${STREAMING_INTEGRATOR_NAME}/ ${STREAMING_INTEGRATOR_HOME}

# set the user and work directory
USER ${USER}
WORKDIR ${USER_HOME}

# expose streaming-integrator ports
EXPOSE 7443 7070 9717 9612 7711 7611

# initiate container and execute the streaming integrator product startup script
ENTRYPOINT ["/home/wso2carbon/wso2si/bin/server.sh"]

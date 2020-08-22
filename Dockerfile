# Linux for Health Connect Image
# Supports the LFH
#
# Environment variables:
# - APP_ROOT: The root application directory. Set in base image.
# - JAVA_HOME: The Java installation directory. Set in base image.
# - JAVA_OPTIONS: Java command line options used to configure the JVM. Set in base image.

# builder image
FROM docker.io/linuxforhealth/openjdk:1.8 AS builder

RUN mkdir -p /tmp/lfh/{config,libs}
WORKDIR /tmp/lfh
ADD build/libs/linux-for-health-connect*dependencies.tar libs/
COPY build/libs/linux-for-health-connect*.jar .

# target image
FROM  docker.io/linuxforhealth/openjdk:1.8

LABEL maintainer="Linux for Health"
LABEL com.linuxforhealth.component="connect"
LABEL name="connect"
LABEL com.linuxforhealth.license_terms="https://www.apache.org/licenses/LICENSE-2.0"
LABEL summary="Linux For Health Connectors for Inbound Data Processing"
LABEL description="Provides Route Based Processing for Inbound Data Flows"

RUN mkdir -p /opt/lfh/libs
COPY --from=builder /tmp/lfh /opt/lfh/

# expose MLLP, REST
EXPOSE 2575 8080 9090

WORKDIR ${APP_ROOT}
USER lfh

CMD ["sh", "-c", "java -XX:+UseContainerSupport ${JAVA_OPTS} -jar linux-for-health-connect.jar"]

# builder image
FROM registry.redhat.io/ubi8/ubi-minimal:8.2 AS builder

RUN mkdir -p /tmp/lfh/{config,libs}
WORKDIR /tmp/lfh
ADD build/libs/linux-for-health-connect*dependencies.tar libs/
COPY build/libs/linux-for-health-connect*.jar .

# target image
FROM registry.redhat.io/ubi8/ubi-minimal:8.2

LABEL maintainer="Linux for Health"
LABEL com.linuxforhealth.component="connect"
LABEL name="connect"
LABEL com.linuxforhealth.license_terms="https://www.apache.org/licenses/LICENSE-2.0"
LABEL summary="Linux For Health Connectors for Inbound Data Processing"
LABEL description="Provides Route Based Processing for Inbound Data Flows"

RUN microdnf update -y && rm -rf /var/cache/yum
RUN microdnf install -y shadow-utils \
                        ca-certificates \
                        java-1.8.0-openjdk && \
                        microdnf clean all

RUN mkdir -p /opt/lfh/libs
COPY --from=builder /tmp/lfh /opt/lfh/

ENV HOME=/home/lfh
ENV APP_ROOT=/opt/lfh
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
ENV JAVA_OPTIONS=""

RUN useradd -u 1001 -r -g 0 -d ${HOME} \
            -s /sbin/nologin \
            -c "Default Application User" default && \
            chown -R 1001:0 ${APP_ROOT}

# MLLP
EXPOSE 2575
# REST
EXPOSE 8080

WORKDIR ${APP_ROOT}
USER 1001

CMD ["sh", "-c", "java -XX:+UseContainerSupport ${JAVA_OPTS} -jar linux-for-health-connect.jar"]

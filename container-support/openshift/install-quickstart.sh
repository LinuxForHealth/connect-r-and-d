#!/bin/sh
# install-quickstart.sh
# Provisions the LFH quick start stack into an existing OpenShift 4.x cluster.
# install-quickstart.sh creates:
# - OpenShift top level LFH project
# - OpenShift applications for LFH and its supporting services
# - OpenShift routes for external client use

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack
source ../compose/.env

oc new-project lfh \
    --description='Linux For Health Open Shift Project' \
    --display-name='Linux for Health'

# create LFH applications
# Orthanc - Image Processing
oc new-app "${LFH_ORTHANC_IMAGE}" \
  --name="${LFH_ORTHANC_SERVICE_NAME}" \
  --labels='app='"${LFH_ORTHANC_SERVICE_NAME}" \
  --show-all=true

# NATS - Messaging
oc new-app "${LFH_NATS_IMAGE}" \
  --name="${LFH_NATS_SERVICE_NAME}" \
  --labels='app='"${LFH_NATS_SERVICE_NAME}" \
  --show-all=true

oc rollout status deployment/"${LFH_NATS_SERVICE_NAME}" -w

# Zookeeper - Kafka Metadata
oc new-app "${LFH_ZOOKEEPER_IMAGE}" \
    --name="${LFH_ZOOKEEPER_SERVICE_NAME}" \
    --labels='app='"${LFH_ZOOKEEPER_SERVICE_NAME}" \
    --show-all=true

oc rollout status deployment/"${LFH_ZOOKEEPER_SERVICE_NAME}" -w

# Kafka - Used for storage
oc new-app "${LFH_KAFKA_IMAGE}" \
    --name="${LFH_KAFKA_SERVICE_NAME}" \
    --labels='app='"${LFH_KAFKA_SERVICE_NAME}" \
    --env KAFKA_ZOOKEEPER_CONNECT="${LFH_KAFKA_ZOOKEEPER_CONNECT}" \
    --env KAFKA_LISTENERS="INTERNAL://:9092" \
    --env KAFKA_ADVERTISED_LISTENERS="${LFH_KAFKA_ADVERTISED_LISTENERS}" \
    --env KAFKA_INTER_BROKER_LISTENER_NAME="${LFH_KAFKA_INTER_BROKER_LISTENER_NAME}" \
    --env KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="${LFH_KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}" \
    --show-all=true

oc rollout status deployment/"${LFH_KAFKA_SERVICE_NAME}" -w

# Kafdrop
# overriding variables for OpenShift
LFH_KAFDROP_JVM_OPTS="-Xms16M -Xmx48M -Xss180K -XX:-TieredCompilation -XX:+UseStringDeduplication -noverify"
oc new-app "${LFH_KAFDROP_IMAGE}" \
    --name "${LFH_KAFDROP_SERVICE_NAME}" \
    --labels='app='"${LFH_KAFDROP_SERVICE_NAME}" \
    --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
    --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
    --show-all=true

oc rollout status deployment/"${LFH_KAFDROP_SERVICE_NAME}" -w

oc expose service "${LFH_KAFDROP_SERVICE_NAME}"

# LFH Connect
oc new-app "${LFH_CONNECT_IMAGE}" \
  --name "${LFH_CONNECT_SERVICE_NAME}" \
  --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
  --env LFH_CONNECT_DATASTORE_URI="{{lfh.connect.datastore.host}}:<topicName>?brokers=kafka:9092" \
  --env LFH_CONNECT_MESSAGING_URI="nats:lfh-events?servers=nats-server:4222" \
  --env LFH_CONNECT_MESSAGING_SUBSCRIBE_HOSTS="nats-server:4222" \
  --env LFH_CONNECT_ORTHANC_SERVER_URI="http://orthanc:{{lfh.connect.orthanc_server.port}}/instances" \
  --show-all=true

oc rollout status deployment/"${LFH_CONNECT_SERVICE_NAME}" -w

oc expose service "${LFH_CONNECT_SERVICE_NAME}" \
  --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
  --port="${LFH_CONNECT_REST_PORT}" \
  --hostname="lfh-server.apps-crc.testing" \
  --name="lfh-server"

oc expose service "${LFH_CONNECT_SERVICE_NAME}" \
  --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
  --port="${LFH_CONNECT_HTTP_PORT}" \
  --hostname="lfh-imaging.apps-crc.testing" \
  --name="lfh-imaging"

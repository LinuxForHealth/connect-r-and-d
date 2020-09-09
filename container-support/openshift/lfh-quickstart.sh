#!/bin/bash
# lfh-quickstart.sh
# Provisions the LFH quick start stack into an existing OpenShift 4.x cluster.
#
# Usage:
# ./lfh-quickstart.sh [install | remove]
#
# lfh-quickstart.sh creates:
# - OpenShift top level LFH project
# - OpenShift applications for LFH and its supporting services
# - OpenShift routes for external client use

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack
source ../compose/.env

SETUP_MODE=$1

function install() {
  # installs LFH assets into an existing OCP cluster
  oc new-project lfh \
      --description='Linux For Health OpenShift Project' \
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

  oc expose service "${LFH_NATS_SERVICE_NAME}" \
    --labels='app='"${LFH_NATS_SERVICE_NAME}" \
    --port="${LFH_NATS_CLIENT_PORT}" \
    --name="lfh-nats-server" \
    --generator='route/v1'

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

  oc expose service "${LFH_KAFDROP_SERVICE_NAME}" \
    --name='lfh-kafdrop-server' \
    --labels='app='"${LFH_KAFDROP_SERVICE_NAME}" \
    --generator='route/v1'

  # LFH Connect
  oc new-app "${LFH_CONNECT_IMAGE}" \
    --name "${LFH_CONNECT_SERVICE_NAME}" \
    --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
    --env LFH_CONNECT_DATASTORE_URI="{{lfh.connect.datastore.host}}:<topicName>?brokers=kafka:9092" \
    --env LFH_CONNECT_MESSAGING_URI="nats:lfh-events?servers=nats-server:4222" \
    --env LFH_CONNECT_MESSAGING_SUBSCRIBE_HOSTS="nats-server:4222" \
    --env LFH_CONNECT_ORTHANC_SERVER_URI="http://orthanc:{{lfh.connect.orthanc_server.port}}/instances" \
    --env LFH_CONNECT_DATASTORE_BROKERS="kafka:9092" \
    --show-all=true

  oc rollout status deployment/"${LFH_CONNECT_SERVICE_NAME}" -w

  oc expose service "${LFH_CONNECT_SERVICE_NAME}" \
    --name="lfh-rest-server" \
    --port="${LFH_CONNECT_REST_PORT}" \
    --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
    --generator='route/v1'

  oc expose service "${LFH_CONNECT_SERVICE_NAME}" \
    --name="lfh-imaging-server" \
    --port="${LFH_CONNECT_HTTP_PORT}" \
    --labels='app='"${LFH_CONNECT_SERVICE_NAME}" \
    --generator='route/v1'
}

function remove() {
  # removes LFH resources from the OCP cluster
  oc delete all --selector app="${LFH_CONNECT_SERVICE_NAME}" --wait
  oc delete all --selector app="${LFH_NATS_SERVICE_NAME}" --wait
  oc delete all --selector app="${LFH_KAFDROP_SERVICE_NAME}" --wait
  oc delete all --selector app="${LFH_KAFKA_SERVICE_NAME}" --wait
  oc delete all --selector app="${LFH_ZOOKEEPER_SERVICE_NAME}" --wait
  oc delete all --selector app="${LFH_ORTHANC_SERVICE_NAME}" --wait
  oc delete project --force lfh --wait
}

case "${SETUP_MODE}" in
  "install")
    install
    ;;
  "remove")
    remove
    ;;
  *)
    echo "Invalid setup mode. Expecting one of: install, remove"
    exit 1
esac

#!/bin/sh

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack
source ../compose/.env

oc new-project lfh \
    --description='Linux For Health Open Shift Project' \
    --display-name='Linux for Health'

# create LFH applications
oc new-app "${LFH_NATS_IMAGE}" \
  --name="${LFH_NATS_SERVICE_NAME}" \
  --labels='app='"${LFH_NATS_SERVICE_NAME}" \
  --show-all=true
oc rollout status deployment/"${LFH_NATS_SERVICE_NAME}" -w

oc new-app "${LFH_ZOOKEEPER_IMAGE}" \
    --name="${LFH_ZOOKEEPER_SERVICE_NAME}" \
    --labels='app='"${LFH_ZOOKEEPER_SERVICE_NAME}" \
    --show-all=true
oc rollout status deployment/"${LFH_ZOOKEEPER_SERVICE_NAME}" -w

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

# overriding variables for OpenShift
LFH_KAFDROP_JVM_OPTS="-Xms16M -Xmx48M -Xss180K -XX:-TieredCompilation -XX:+UseStringDeduplication -noverify"
oc new-app "${LFH_KAFDROP_IMAGE}" \
    --name "${LFH_KAFDROP_SERVICE_NAME}" \
    --labels='app='"${LFH_KAFDROP_SERVICE_NAME}" \
    --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
    --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
    --show-all=true
oc rollout status deployment/"${LFH_KAFDROP_SERVICE_NAME}" -w
oc expose svc/"${LFH_KAFDROP_SERVICE_NAME}"

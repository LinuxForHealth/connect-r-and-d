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
    --show-all=true
oc rollout status deployment/"${LFH_KAFKA_SERVICE_NAME}" -w

# overriding variables for OpenShift
LFH_KAFDROP_JVM_OPTS="-Xms16M -Xmx48M -Xss180K -XX:-TieredCompilation -XX:+UseStringDeduplication -noverify"
LFH_KAFDROP_BROKER_CONNECT="kafka:9092"

oc new-app "${LFH_KAFDROP_IMAGE}" \
    --name "${LFH_KAFDROP_SERVICE_NAME}" \
    --labels='app='"${LFH_KAFDROP_SERVICE_NAME}" \
    --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
    --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
    --show-all=true
oc rollout status deployment/"${LFH_KAFDROP_SERVICE_NAME}" -w
oc expose svc/"${LFH_KAFDROP_SERVICE_NAME}"

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

LFH_KAFDROP_JVM_OPTS="-Xms16M -Xmx48M -Xss180K -XX:-TieredCompilation -XX:+UseStringDeduplication -noverify"
oc new-app "${LFH_KAFDROP_IMAGE}" \
    --name "${LFH_KAFDROP_SERVICE_NAME}" \
    --labels='app='"${LFH_KAFDROP_SERVICE_NAME}" \
    --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
    --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
    --show-all=true

oc rollout status deployment/"${LFH_KAFDROP_SERVICE_NAME}" -w

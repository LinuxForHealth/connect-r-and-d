#!/bin/sh
# remove-quickstart.sh
# removes all LFH objects, including the project from an OpenShift 4.x cluster.

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack
source ../compose/.env

oc delete all --selector app="${LFH_CONNECT_SERVICE_NAME}" --wait
oc delete all --selector app="${LFH_NATS_SERVICE_NAME}" --wait
oc delete all --selector app="${LFH_KAFDROP_SERVICE_NAME}" --wait
oc delete all --selector app="${LFH_KAFKA_SERVICE_NAME}" --wait
oc delete all --selector app="${LFH_ZOOKEEPER_SERVICE_NAME}" --wait
oc delete all --selector app="${LFH_ORTHANC_SERVICE_NAME}" --wait
oc delete project --force lfh --wait

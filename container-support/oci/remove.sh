#!/usr/bin/env bash
# remove.sh
# removes the LFH network and containers

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack and the oci tools
source ../compose/.env
source .env

${OCI_COMMAND} rm -f ${LFH_CONNECT_SERVICE_NAME}
${OCI_COMMAND} rm -f ${LFH_KAFDROP_SERVICE_NAME}
${OCI_COMMAND} rm -f ${LFH_KAFKA_SERVICE_NAME}
${OCI_COMMAND} rm -f ${LFH_ZOOKEEPER_SERVICE_NAME}
${OCI_COMMAND} rm -f ${LFH_NATS_SERVICE_NAME}
${OCI_COMMAND} rm -f ${LFH_ORTHANC_SERVICE_NAME}

${OCI_COMMAND} network rm ${LFH_NETWORK_NAME}

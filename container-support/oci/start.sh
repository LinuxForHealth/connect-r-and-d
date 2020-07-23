#!/usr/bin/env bash
# start.sh
# starts Linux for Health OCI containers

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack and the oci tools
source ../compose/.env
source .env

function is_ready {
  # returns 0 if the service is "ready" or 1 if the service is not ready after "${LFH_RETRY_ATTEMPTS}" are exhausted
  # parameters:
  # - host: the service host
  # - port: the service port
  local host=$1
  local port=$2

  local retry_count=0
  local return_code=0

  while [ "${retry_count}" -lt "${LFH_RETRY_ATTEMPTS}" ]
  do
    nc -z "${host}" "${port}"
    return_code=$?
    if [ "${return_code}" -eq 0 ]; then
      echo "${host} ${port} is ready"
      return 0
    else
      echo "retrying ${host}:${port}"
      ((retry_count=$retry_count+1))
      sleep "${LFH_RETRY_INTERVAL}"
    fi
  done
  if [ "${LFH_RETRY_EXIT_ON_FAILURE}" == true ]; then
    echo "${host} ${port} is not ready. Exiting."
    exit
  fi
  return 1
}

# create network
echo "create lfh network"
${OCI_COMMAND} network create "${LFH_NETWORK_NAME}"

# launch containers
echo "launch nats container"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_NATS_SERVICE_NAME}" \
              -p "${LFH_NATS_CLIENT_HOST_PORT}":"${LFH_NATS_CLIENT_CONTAINER_PORT}" \
              -p "${LFH_NATS_MANAGEMENT_HOST_PORT}":"${LFH_NATS_MANAGEMENT_CONTAINER_PORT}" \
              -p "${LFH_NATS_CLUSTER_HOST_PORT}":"${LFH_NATS_CLUSTER_CONTAINER_PORT}" \
              "${LFH_NATS_IMAGE}"

is_ready localhost "${LFH_NATS_CLIENT_HOST_PORT}"
is_ready localhost "${LFH_NATS_MANAGEMENT_HOST_PORT}"
is_ready localhost "${LFH_NATS_CLUSTER_HOST_PORT}"

echo "launch zookeeper container"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_ZOOKEEPER_SERVICE_NAME}" \
              -p "${LFH_ZOOKEEPER_HOST_PORT}":"${LFH_ZOOKEEPER_CONTAINER_PORT}" \
              "${LFH_ZOOKEEPER_IMAGE}"

is_ready localhost "${LFH_ZOOKEEPER_HOST_PORT}"

echo "launch kafka container"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_KAFKA_SERVICE_NAME}" \
              -p "${LFH_KAFKA_INTERNAL_HOST_PORT}":"${LFH_KAFKA_INTERNAL_CONTAINER_PORT}" \
              -p "${LFH_KAFKA_EXTERNAL_HOST_PORT}":"${LFH_KAFKA_EXTERNAL_CONTAINER_PORT}" \
              --env KAFKA_ZOOKEEPER_CONNECT="${LFH_KAFKA_ZOOKEEPER_CONNECT}" \
              --env KAFKA_LISTENERS="${LFH_KAFKA_LISTENERS}" \
              --env KAFKA_ADVERTISED_LISTENERS="${LFH_KAFKA_ADVERTISED_LISTENERS}" \
              --env KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="${LFH_KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}" \
              --env KAFKA_INTER_BROKER_LISTENER_NAME="${LFH_KAFKA_INTER_BROKER_LISTENER_NAME}" \
              "${LFH_KAFKA_IMAGE}"

is_ready localhost "${LFH_KAFKA_INTERNAL_HOST_PORT}"
is_ready localhost "${LFH_KAFKA_EXTERNAL_HOST_PORT}"

echo "launch kafdrop"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_KAFDROP_SERVICE_NAME}" \
              -p "${LFH_KAFDROP_HOST_PORT}":"${LFH_KAFDROP_CONTAINER_PORT}" \
              --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
              --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
              "${LFH_KAFDROP_IMAGE}"

is_ready localhost "${LFH_KAFDROP_HOST_PORT}"

echo "launch lfh connect"

HOST_VOLUME_DIR=$(dirname "${PWD}")/compose
echo "mounting application.properties from ${HOST_VOLUME_DIR}"

${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_CONNECT_SERVICE_NAME}" \
              -p "${LFH_CONNECT_MLLP_HOST_PORT}":"${LFH_CONNECT_MLLP_CONTAINER_PORT}" \
              -p "${LFH_CONNECT_REST_HOST_PORT}":"${LFH_CONNECT_REST_CONTAINER_PORT}" \
              -v "${HOST_VOLUME_DIR}"/application.properties:/opt/lfh/config/application.properties \
              "${LFH_CONNECT_IMAGE}"

is_ready localhost "${LFH_CONNECT_MLLP_HOST_PORT}"
is_ready localhost "${LFH_CONNECT_REST_HOST_PORT}"

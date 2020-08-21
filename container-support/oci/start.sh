#!/usr/bin/env bash
# start-stack.sh
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
${OCI_COMMAND} pull "${LFH_ORTHANC_IMAGE}"
echo "launch orthanc container"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_ORTHANC_SERVICE_NAME}" \
              "${LFH_ORTHANC_IMAGE}"

${OCI_COMMAND} pull "${LFH_NATS_IMAGE}"
echo "launch nats container"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_NATS_SERVICE_NAME}" \
              "${LFH_NATS_IMAGE}"

echo "launch zookeeper container"
${OCI_COMMAND} pull "${LFH_ZOOKEEPER_IMAGE}"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_ZOOKEEPER_SERVICE_NAME}" \
              "${LFH_ZOOKEEPER_IMAGE}"

echo "launch kafka container"
${OCI_COMMAND} pull "${LFH_KAFKA_IMAGE}"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_KAFKA_SERVICE_NAME}" \
              --env KAFKA_ZOOKEEPER_CONNECT="${LFH_KAFKA_ZOOKEEPER_CONNECT}" \
              --env KAFKA_LISTENERS="${LFH_KAFKA_LISTENERS}" \
              --env KAFKA_ADVERTISED_LISTENERS="${LFH_KAFKA_ADVERTISED_LISTENERS}" \
              --env KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="${LFH_KAFKA_LISTENER_SECURITY_PROTOCOL_MAP}" \
              --env KAFKA_INTER_BROKER_LISTENER_NAME="${LFH_KAFKA_INTER_BROKER_LISTENER_NAME}" \
              "${LFH_KAFKA_IMAGE}"

echo "launch kafdrop"
${OCI_COMMAND} pull "${LFH_KAFDROP_IMAGE}"
${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_KAFDROP_SERVICE_NAME}" \
              -p "${LFH_KAFDROP_PORT}":"${LFH_KAFDROP_PORT}" \
              --env KAFKA_BROKERCONNECT="${LFH_KAFDROP_BROKER_CONNECT}" \
              --env JVM_OPTS="${LFH_KAFDROP_JVM_OPTS}" \
              "${LFH_KAFDROP_IMAGE}"

is_ready localhost "${LFH_KAFDROP_PORT}"

echo "launch lfh connect"
${OCI_COMMAND} pull "${LFH_CONNECT_IMAGE}"

${OCI_COMMAND} run -d \
              --network "${LFH_NETWORK_NAME}" \
              --name "${LFH_CONNECT_SERVICE_NAME}" \
              -p "${LFH_CONNECT_MLLP_PORT}":"${LFH_CONNECT_MLLP_PORT}" \
              -p "${LFH_CONNECT_REST_PORT}":"${LFH_CONNECT_REST_PORT}" \
              -p "${LFH_CONNECT_HTTP_PORT}":"${LFH_CONNECT_HTTP_PORT}" \
              --env LFH_CONNECT_DATASTORE_URI="{{lfh.connect.datastore.host}}:<topicName>?brokers=kafka:9092" \
              --env LFH_CONNECT_MESSAGING_URI="nats:lfh-events?servers=nats-server:4222" \
              --env LFH_CONNECT_MESSAGING_SUBSCRIBE_HOSTS="nats-server:4222" \
              --env LFH_CONNECT_ORTHANC_SERVER_URI="http://orthanc:{{lfh.connect.orthanc_server.port}}/instances" \
              "${LFH_CONNECT_IMAGE}"

is_ready localhost "${LFH_CONNECT_MLLP_PORT}"
is_ready localhost "${LFH_CONNECT_REST_PORT}"
is_ready localhost "${LFH_CONNECT_HTTP_PORT}"

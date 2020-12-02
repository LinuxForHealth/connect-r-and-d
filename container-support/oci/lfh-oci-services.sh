#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# lfh-oci-services.sh
#
# Usage:
# ./lfh-oci-services.sh [start | remove] [oci_command]
#
# The oci_command defaults to "docker"

set -o errexit
set -o nounset
set -o pipefail

# load environment variables from compose stack and the oci tools
source ../compose/.env
source .env

SERVICE_OPERATION=$1
# the container command used - docker, podman, etc
OCI_COMMAND=${2:-"docker"}

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

function wait_for_log_msg {
    local service_name=$1
    local message=$2
    local retry_count=0

    while [ "$retry_count" -lt "${LFH_RETRY_ATTEMPTS}" ]
    do
      ${OCI_COMMAND} logs "$service_name" 2> >(grep -i "$message")
      status=$?
        if [ $status -eq 0 ]; then
            echo "$service_name log message found"
            return 0
        else
            echo "waiting for $service_name log message: $message"
            ((retry_count=$retry_count+1))
            sleep "$LFH_SLEEP_INTERVAL"
        fi
    done

    if [ "${LFH_RETRY_EXIT_ON_FAILURE}" == true ]; then
      echo "${service_name} is not ready. Exiting."
      exit
    fi
    return 1
}

function wait_for_cmd {
    local retry_count=0
    local failed=0
    local cmd="${@}"

    while [ "$retry_count" -lt "${LFH_RETRY_ATTEMPTS}" ]
    do
      { ${cmd}; } || { failed=1; }
      if [ $failed -eq 1 ]; then
          failed=0
          echo "waiting until service is ready"
          ((retry_count=$retry_count+1))
          sleep "$LFH_SLEEP_INTERVAL"
      else
          echo "service is ready"
          return 0
      fi
    done

    if [ "${LFH_RETRY_EXIT_ON_FAILURE}" == true ]; then
      echo "service is not ready. Exiting."
      exit
    fi
    return 1
}

function start() {
  # creates and starts LFH container services
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
                -p "${LFH_NATS_CLIENT_PORT}":"${LFH_NATS_CLIENT_PORT}" \
                -v "$PWD/../certs:/certs" \
                "${LFH_NATS_IMAGE}" \
                server --tls \
                --tlscert=/certs/nats-server.crt \
                --tlskey=/certs/nats-server.key \
                --tlscacert=/certs/rootCA.crt
  is_ready localhost "${LFH_NATS_CLIENT_PORT}"
  echo "create NATS JetStream stream"
  wait_for_cmd docker exec -it "${LFH_NATS_SERVICE_NAME}" \
                nats --server="${LFH_NATS_SERVICE_NAME}":"${LFH_NATS_CLIENT_PORT}" \
                --tlscert=../certs/server.crt \
                --tlskey=../certs/server.key \
                --tlsca=../certs/rootCA.crt \
                str add EVENTS \
                --subjects EVENTS.* \
                --ack \
                --max-msgs=-1 \
                --max-bytes=-1 \
                --max-age=1y \
                --storage file \
                --retention limits \
                --max-msg-size=-1 \
                --discard old \
                --dupe-window=10s > /dev/null
  echo "create NATS JetStream consumer"
  docker exec -it "${LFH_NATS_SERVICE_NAME}" \
                nats --server="${LFH_NATS_SERVICE_NAME}":"${LFH_NATS_CLIENT_PORT}" \
                --tlscert=../certs/server.crt \
                --tlskey=../certs/server.key \
                --tlsca=../certs/rootCA.crt \
                con add EVENTS SUBSCRIBER \
                --ack none \
                --target lfh-events \
                --deliver last \
                --replay instant \
                --filter '' > /dev/null

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
                --env LFH_CONNECT_DATASTORE_URI="kafka:<topicName>?brokers=kafka:9092" \
                --env LFH_CONNECT_MESSAGING_RESPONSE_URI="nats:EVENTS.responses?servers=nats-server:4222&secure=true&sslContextParameters=#sslContextParameters" \
                --env LFH_CONNECT_MESSAGING_ERROR_URI="nats:EVENTS.errors?servers=nats-server:4222&secure=true&sslContextParameters=#sslContextParameters" \
                --env LFH_CONNECT_MESSAGING_SUBSCRIBE_HOSTS="nats-server:4222" \
                --env LFH_CONNECT_ORTHANC_SERVER_URI="http://orthanc:8042/instances" \
                --env LFH_CONNECT_DATASTORE_BROKERS="kafka:9092" \
                "${LFH_CONNECT_IMAGE}"

  echo "launch postgres container"
  ${OCI_COMMAND} pull "${LFH_PG_IMAGE}"
  ${OCI_COMMAND} run -d \
                --network "${LFH_NETWORK_NAME}" \
                --name "${LFH_PG_SERVICE_NAME}" \
                --env PGDATA="${LFH_PG_DATA}" \
                --env POSTGRES_USER="${LFH_PG_USER}" \
                --env POSTGRES_PASSWORD="${LFH_PG_PASSWORD}" \
                --env POSTGRES_DB="${LFH_KONG_DATABASE}" \
                --mount source=pg_data,target=${LFH_PG_DATA} \
                --health-cmd='pg_isready -U postgres' \
                --health-interval=5s \
                --health-retries=10 \
                --health-timeout=5s \
                "${LFH_PG_IMAGE}"
  wait_for_cmd docker exec -it "${LFH_PG_SERVICE_NAME}" psql --username "${LFH_PG_USER}" -c '\q'

  echo "launch kong-migration"
  ${OCI_COMMAND} pull "${LFH_KONG_IMAGE}"
  ${OCI_COMMAND} run -d \
                --network "${LFH_NETWORK_NAME}" \
                --name "${LFH_KONG_MIGRATION_SERVICE_NAME}" \
                --env KONG_DATABASE="${LFH_KONG_DATABASE_TYPE}" \
                --env KONG_PG_HOST="postgres" \
                --env KONG_PG_USER="${LFH_PG_USER}" \
                --env KONG_PG_PASSWORD="${LFH_PG_PASSWORD}" \
                "${LFH_KONG_IMAGE}" \
                kong migrations bootstrap -v
  { wait_for_log_msg ${LFH_KONG_MIGRATION_SERVICE_NAME} "Database is up-to-date"; } || \
  { wait_for_log_msg ${LFH_KONG_MIGRATION_SERVICE_NAME} "Database already bootstrapped"; }

  echo "launch kong"
  ${OCI_COMMAND} pull "${LFH_KONG_IMAGE}"
  ${OCI_COMMAND} run -d \
                --network "${LFH_NETWORK_NAME}" \
                --name "${LFH_KONG_SERVICE_NAME}" \
                -p "${LFH_KONG_SSL_PORT}":"${LFH_KONG_SSL_PORT}" \
                -p "${LFH_KONG_ADMIN_SSL_PORT}":"${LFH_KONG_ADMIN_SSL_PORT}" \
                -p "${LFH_KONG_MLLP_PORT}":"${LFH_KONG_MLLP_PORT}" \
                --env KONG_DATABASE="${LFH_KONG_DATABASE_TYPE}" \
                --env KONG_PG_HOST="postgres" \
                --env KONG_PG_USER="${LFH_PG_USER}" \
                --env KONG_PG_PASSWORD="${LFH_PG_PASSWORD}" \
                --env KONG_ADMIN_LISTEN="${LFH_KONG_ADMIN_LISTEN}" \
                --env KONG_STREAM_LISTEN="${LFH_KONG_STREAM_LISTEN}" \
                --env KONG_LOG_LEVEL="${LFH_KONG_LOG_LEVEL}" \
                --env KONG_PLUGINS="${LFH_KONG_PLUGINS}" \
                "${LFH_KONG_IMAGE}"
  is_ready localhost "${LFH_KONG_SSL_PORT}"
  is_ready localhost "${LFH_KONG_ADMIN_SSL_PORT}"
  is_ready localhost "${LFH_KONG_MLLP_PORT}"
  wait_for_cmd curl --silent --insecure https://localhost:8444/services
  . ./configure-kong.sh
}

function remove() {
  # removes LFH container services including the network
  ${OCI_COMMAND} rm -f ${LFH_CONNECT_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_KAFDROP_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_KAFKA_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_ZOOKEEPER_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_NATS_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_ORTHANC_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_PG_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_KONG_SERVICE_NAME}
  ${OCI_COMMAND} rm -f ${LFH_KONG_MIGRATION_SERVICE_NAME}

  ${OCI_COMMAND} volume rm pg_data

  ${OCI_COMMAND} network rm ${LFH_NETWORK_NAME}
}

case "${SERVICE_OPERATION}" in
  "start")
    start
    ;;
  "remove")
    remove
    ;;
    *)
      echo "Invalid service operation. Expected one of [start|remove]"
      exit 1
esac

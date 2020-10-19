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

function wait_for_kong {
    local retry_count=0
    local failed=0

    while [ "$retry_count" -lt "${LFH_RETRY_ATTEMPTS}" ]
    do
      { curl --silent http://localhost:8001/services; } || { failed=1; }
      if [ $failed -eq 1 ]; then
          failed=0
          echo "waiting until kong is ready"
          ((retry_count=$retry_count+1))
          sleep "$LFH_SLEEP_INTERVAL"
      else
          echo ""
          echo "kong is ready"
          return 0
      fi
    done

    if [ "${LFH_RETRY_EXIT_ON_FAILURE}" == true ]; then
      echo "$kong is not ready. Exiting."
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
                "${LFH_NATS_IMAGE}"
  is_ready localhost "${LFH_NATS_CLIENT_PORT}"

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
                -p "${LFH_CONNECT_HTTP_PORT}":"${LFH_CONNECT_HTTP_PORT}" \
                --env LFH_CONNECT_DATASTORE_URI="kafka:<topicName>?brokers=kafka:9092" \
                --env LFH_CONNECT_MESSAGING_URI="nats:lfh-events?servers=nats-server:4222" \
                --env LFH_CONNECT_MESSAGING_SUBSCRIBE_HOSTS="nats-server:4222" \
                --env LFH_CONNECT_ORTHANC_SERVER_URI="http://orthanc:8042/instances" \
                --env LFH_CONNECT_DATASTORE_BROKERS="kafka:9092" \
                "${LFH_CONNECT_IMAGE}"

  is_ready localhost "${LFH_CONNECT_MLLP_PORT}"
  is_ready localhost "${LFH_CONNECT_HTTP_PORT}"

  echo "launch postgres container"
  ${OCI_COMMAND} pull "${LFH_PG_IMAGE}"
  ${OCI_COMMAND} run -d \
                --network "${LFH_NETWORK_NAME}" \
                --name "${LFH_PG_SERVICE_NAME}" \
                -p "${LFH_PG_SERVER_PORT}":"${LFH_PG_SERVER_PORT}" \
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
  is_ready localhost "${LFH_PG_SERVER_PORT}"
  wait_for_log_msg ${LFH_PG_SERVICE_NAME} "database system is ready to accept connections"

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
  wait_for_log_msg ${LFH_KONG_MIGRATION_SERVICE_NAME} "Database is up-to-date"

  echo "launch kong"
  ${OCI_COMMAND} pull "${LFH_KONG_IMAGE}"
  ${OCI_COMMAND} run -d \
                --network "${LFH_NETWORK_NAME}" \
                --name "${LFH_KONG_SERVICE_NAME}" \
                -p "${LFH_KONG_PORT}":"${LFH_KONG_PORT}" \
                -p "${LFH_KONG_SSL_PORT}":"${LFH_KONG_SSL_PORT}" \
                -p "${LFH_KONG_ADMIN_PORT}":"${LFH_KONG_ADMIN_PORT}" \
                -p "${LFH_KONG_ADMIN_SSL_PORT}":"${LFH_KONG_ADMIN_SSL_PORT}" \
                -p "${LFH_KONG_MLLP_PORT}":"${LFH_KONG_MLLP_PORT}" \
                --env KONG_DATABASE="${LFH_KONG_DATABASE_TYPE}" \
                --env KONG_PG_HOST="postgres" \
                --env KONG_PG_USER="${LFH_PG_USER}" \
                --env KONG_PG_PASSWORD="${LFH_PG_PASSWORD}" \
                --env KONG_ADMIN_LISTEN="${LFH_KONG_ADMIN_LISTEN}" \
                --env KONG_STREAM_LISTEN="${LFH_KONG_STREAM_LISTEN}" \
                "${LFH_KONG_IMAGE}"
  is_ready localhost "${LFH_KONG_PORT}"
  is_ready localhost "${LFH_KONG_SSL_PORT}"
  is_ready localhost "${LFH_KONG_ADMIN_PORT}"
  is_ready localhost "${LFH_KONG_ADMIN_SSL_PORT}"
  is_ready localhost "${LFH_KONG_MLLP_PORT}"
  wait_for_kong
  configure_kong
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

  ${OCI_COMMAND} network rm ${LFH_NETWORK_NAME}
}

function configure_kong {
  host=${LFH_CONNECT_SERVICE_NAME}
  hostip=${HOST_IP}
  lfhhttp=${LFH_CONNECT_HTTP_PORT}
  lfhmllp=${LFH_CONNECT_MLLP_PORT}
  kongmllp=${LFH_KONG_MLLP_PORT}

  echo "Adding a kong service for all LinuxForHealth http routes"
  curl http://localhost:8001/services \
    -H 'Content-Type: application/json' \
    -d '{"name": "lfh-http-service", "url": "http://'"${host}"':'"${lfhhttp}"'"}'
  echo ""

  echo "Adding a kong route that matches incoming requests and sends them to the lfh-http-service url"
  curl http://localhost:8001/services/lfh-http-service/routes \
    -H 'Content-Type: application/json' \
    -d '{"name": "lfh-http-route", "hosts": ["'"${hostip}"'","127.0.0.1","localhost"]}'
  echo ""

  echo "Adding a kong service for the linux for health hl7v2 mllp route"
  curl http://localhost:8001/services \
    -H 'Content-Type: application/json' \
    -d '{"name": "lfh-hl7v2-service", "url": "tcp://'"${host}"':'"${lfhmllp}"'"}'
  echo ""

  echo "Adding a kong route that matches incoming requests and sends them to the lfh-hl7v2-service url"
  curl http://localhost:8001/services/lfh-hl7v2-service/routes \
    -H 'Content-Type: application/json' \
    -d '{"name": "lfh-hl7v2-route", "protocols": ["tcp", "tls"], "destinations": [{"port":'"${kongmllp}"'}]}'
  echo ""
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

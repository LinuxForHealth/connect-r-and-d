#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# start-stack.sh
# Starts the LFH Docker Compose Stack for a specified profile. The profile determines which configurations are included
# in the startup process.
#
# Usage: ./start-stack.sh [profile name] where profile is one of dev, server or pi.
# [profile name] "defaults" to dev
#
# It is recommended to run this script in the current shell session, so that the docker-compose
# CLI will evaluate it's commands using the COMPOSE_FILE variable set in this script.
# Example:
# . ./start-stack.sh server
# OR
# source start-stack.sh server
set -o errexit
set -o nounset
set -o pipefail

LFH_COMPOSE_PROFILE=${1:-dev}
echo "${LFH_COMPOSE_PROFILE}"

echo "==============================================="
echo "LFH Compose Startup"
echo "LFH compose profile is set to ${LFH_COMPOSE_PROFILE}"

# configures the LFH Kong API Gateway for the dev, integration, and server profiles
case "${LFH_COMPOSE_PROFILE}" in
  dev)
    export LFH_KONG_CONNECT_HOST="localhost"
    [ "$(uname -s)" == "Darwin" ] && export LFH_KONG_CONNECT_HOST="host.docker.internal"
    if [[ "$(uname -r)" == *"microsoft"* ]]; then
      export LFH_KONG_CONNECT_HOST="host.docker.internal"
      export LFH_KONG_ADMIN_LISTEN="'${LFH_KONG_ADMIN_LISTEN}'"
    fi
    export LFH_KONG_ORTHANC_HOST=${LFH_KONG_CONNECT_HOST}
    export COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml:docker-compose.kong-migration.yml
    source ./configure-kong.sh
    ;;
  integration|pi|server)
    export LFH_KONG_CONNECT_HOST="compose_lfh_1"
    export LFH_KONG_ORTHANC_HOST="compose_orthanc_1"
    export COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml:docker-compose.kong-migration.yml
    source ./configure-kong.sh
    ;;
esac

# set the compose override file
OVERRIDE_FILE=docker-compose."${LFH_COMPOSE_PROFILE}".yml

if [ ! -f "${OVERRIDE_FILE}" ]; then
  echo "Invalid LFH Compose Profile ${LFH_COMPOSE_PROFILE}."
  echo "Expecting one of: dev, integration, server, or pi"
  return
fi

export COMPOSE_FILE=docker-compose.yml:"${OVERRIDE_FILE}"

# integration includes external systems used as route producers
if [[ "${LFH_COMPOSE_PROFILE}" == "integration" ]]; then
  export COMPOSE_FILE="${COMPOSE_FILE}":docker-compose.server.yml
fi

# start and configure NATS JetStream
source ./configure-nats.sh

echo "starting LFH compose ${LFH_COMPOSE_PROFILE}"
docker-compose up -d
docker-compose ps
echo "==============================================="

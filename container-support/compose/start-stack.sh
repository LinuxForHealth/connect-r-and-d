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

export LFH_KONG_LFHHOST="compose_lfh_1"

LFH_COMPOSE_PROFILE=${1:-dev}
echo "${LFH_COMPOSE_PROFILE}"

echo "==============================================="
echo "LFH Compose Startup"
echo "LFH compose profile is set to ${LFH_COMPOSE_PROFILE}"

function set_lfh_host() {
  if [ "$(uname -s)" == "Darwin" ]; then
    # Set Docker container to localhost connection workaround on MacOS
    export LFH_KONG_LFHHOST="host.docker.internal"
  else
    export LFH_KONG_LFHHOST="localhost"
  fi
}

case "${LFH_COMPOSE_PROFILE}" in
  dev)
  echo "starting LFH compose development profile"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml:docker-compose.kong-migration.yml
  set_lfh_host
  . ./configure-kong.sh
  export COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml
  ;;
  server)
  echo "starting LFH compose server profile"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml:docker-compose.kong-migration.yml
  . ./configure-kong.sh
  export COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml
  ;;
  pi)
  echo "starting LFH compose pi profile"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.pi.yml:docker-compose.kong-migration.yml
  . ./configure-kong.sh
  export COMPOSE_FILE=docker-compose.yml:docker-compose.pi.yml
  ;;
  *)
  echo "invalid LFH Compose Profile. Expecting one of:dev, server, pi"
  export COMPOSE_FILE=""
  ;;
esac

if [ -n "${COMPOSE_FILE}" ]; then
  echo "Parsing compose files for ${LFH_COMPOSE_PROFILE} profile."
  echo "COMPOSE_FILE=${COMPOSE_FILE}"
  docker-compose up -d --remove-orphans
  docker-compose ps
fi
echo "==============================================="

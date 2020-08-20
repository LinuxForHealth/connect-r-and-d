#!/bin/sh
# start-stack.sh
# Starts a LFH Docker Compose Stack
# Usage: ./start-stack.sh [compose stack] where compose stack is one of dev, server or pi.
# [compose stack] "defaults" to dev
#
# It is recommended to run this script in the current shell session, so that the docker-compose
# CLI will evaluate it's commands using the COMPOSE_FILE variable set in this script.
# Example:
# . ./start-stack.sh server
set -o errexit
set -o nounset
set -o pipefail

LFH_COMPOSE_STACK=${1:-dev}

echo "==============================================="
echo "LFH Compose Startup"
echo "LFH compose stack is set to ${LFH_COMPOSE_STACK}"

case "${LFH_COMPOSE_STACK}" in
  dev)
  echo "starting LFH compose development stack"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml
  ;;
  server)
  echo "starting LFH compose server stack"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml
  ;;
  pi)
  echo "starting LFH compose pi stack"
  export COMPOSE_FILE=docker-compose.yml:docker-compose.server.yml:docker-compose.pi.yml
  ;;
  *)
  echo "invalid LFH Compose Stack. Expecting one of:dev, server, pi"
  ;;
esac

if [ -n "${COMPOSE_FILE}" ]; then
  echo "Parsing compose files for ${LFH_COMPOSE_STACK} stack. COMPOSE_FILE=${COMPOSE_FILE}"
  docker-compose up -d
  docker-compose ps
fi
echo "==============================================="

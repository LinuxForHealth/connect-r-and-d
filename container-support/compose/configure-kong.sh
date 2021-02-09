#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# configure-kong.sh
# Configures the postgres database for Kong.
# Must be run at least once in the database lifecycle, but subsequent runs are not harmful.

# wait parameters used to determine when the services within a container are available
SLEEP_INTERVAL=2
MAX_CHECKS=5

source .env

DB_SERVICE="postgres"
DB_SERVICE_MESSAGE="database system is ready to accept connections"
DB_CONFIG_SERVICE="kong-migration"
DB_CONFIG_SERVICE_MESSAGE="Database is up-to-date"
DB_CONFIG_CONFIGURED_MESSAGE="Database already bootstrapped"
KONG_SERVICE="kong"
KONG_SERVICE_MESSAGE="finished preloading 'plugins' into the core_cache"

CURL_FLAGS="--insecure --silent --output /dev/null"

function is_ready() {
    local service_name=$1
    local success_message=$2
    local retry_count=0
    local log_statement=$(docker-compose logs "$service_name" | grep -i "$success_message")

    while [ "$retry_count" -lt "$MAX_CHECKS" ]
    do
        if docker-compose logs "$service_name" | grep -i "$success_message"; then
            echo "$service_name is ready"
            return 0
        else
            echo "$service_name is not ready"
            ((retry_count=$retry_count+1))
            sleep "$SLEEP_INTERVAL"
        fi
    done

    return 1
}

function wait_for_cmd() {
    local retry_count=0
    local failed=0
    local cmd="${@}"

    while [ "$retry_count" -lt "$MAX_CHECKS" ]
    do
      { ${cmd}; } || { failed=1; }
      if [ $failed -eq 1 ]; then
          failed=0
          echo "waiting until service is ready"
          ((retry_count=$retry_count+1))
          sleep "$SLEEP_INTERVAL"
      else
          echo "service is ready"
          return 0
      fi
    done

    return 1
}

# wait for postgres
docker-compose up -d "$DB_SERVICE"
is_ready "$DB_SERVICE" "$DB_SERVICE_MESSAGE"
wait_for_cmd docker exec -it compose_"$DB_SERVICE"_1 psql --username "${LFH_PG_USER}" -c '\q'

# start kong migration and wait for ephemeral container to complete
docker-compose up -d "$DB_CONFIG_SERVICE"
{ is_ready "$DB_CONFIG_SERVICE" "$DB_CONFIG_SERVICE_MESSAGE"; } || \
{ is_ready "$DB_CONFIG_SERVICE" "$DB_CONFIG_CONFIGURED_MESSAGE"; }

# start kong and wait for kong to come up
docker-compose up -d "$KONG_SERVICE"
is_ready "$KONG_SERVICE" "$KONG_SERVICE_MESSAGE"
wait_for_cmd curl --silent --insecure https://localhost:8444/services

. ../oci/configure-kong.sh

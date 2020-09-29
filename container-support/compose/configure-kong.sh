#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# configure-kong.sh
# Configures the postgres database for Kong.
# Must be run at least once in the database lifecycle, but subsequent runs are not harmful.

# wait parameters used to determine when the services within a container are available
SLEEP_INTERVAL=5
MAX_CHECKS=10

DB_SERVICE="postgres"
DB_CONFIG_SERVICE="kong-migration"
DB_CONFIG_SERVICE_MESSAGE="Database is up-to-date"
KONG_SERVICE="kong"
KONG_SERVICE_MESSAGE="finished preloading 'plugins' into the core_cache"

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

function wait_for_postgres() {
    local retry_count=0

    while [ "$retry_count" -lt "$MAX_CHECKS" ]
    do
        if docker-compose exec postgres sh -c "psql -c '\q'"; then
            echo "postgres is ready"
            return 0
        else
            echo "postgres is not ready"
            ((retry_count=$retry_count+1))
            sleep "$SLEEP_INTERVAL"
        fi
    done

    return 1
}

function create_kong_database() {
    if docker-compose exec -u postgres postgres sh -c "createdb kong"; then
        echo "created kong database: $?"
        return 0
    else
        echo "error creating kong database: $?"
    fi

    return 1
}

function wait_for_kong_database() {
    local retry_count=0

    while [ "$retry_count" -lt "$MAX_CHECKS" ]
    do
        if docker-compose exec postgres sh -c "psql kong postgres -c '\q'"; then
            echo "postgres is ready"
            return 0
        else
            echo "postgres is not ready"
            ((retry_count=$retry_count+1))
            sleep "$SLEEP_INTERVAL"
        fi
    done

    return 1
}

# wait for postgres
docker-compose up -d "$DB_SERVICE"
wait_for_postgres

# once postgres is ready, create the kong database and wait for it
create_kong_database
wait_for_kong_database

# start kong migration and wait for ephemeral container to complete
docker-compose up -d "$DB_CONFIG_SERVICE"
is_ready "$DB_CONFIG_SERVICE" "$DB_CONFIG_SERVICE_MESSAGE"

# start kong and wait for kong to come up
docker-compose up -d "$KONG_SERVICE"
is_ready "$KONG_SERVICE" "$KONG_SERVICE_MESSAGE"

# host is how kong needs to reference LFH, depending on where LFH is running
host=${LFH_KONG_LFHHOST}
lfhhttp=${LFH_CONNECT_REST_PORT}
lfhmllp=${LFH_CONNECT_MLLP_PORT}
kongmllp=${LFH_KONG_MLLP_PORT}

# Add a kong service for all linux for health http routes
curl http://localhost:8001/services \
  -H 'Content-Type: application/json' \
  -d '{"name": "lfh-http-service", "url": "http://'"${host}"':'"${lfhhttp}"'"}'
echo ""

# Add a kong route that matches incoming requests and sends them to the url above
curl http://localhost:8001/services/lfh-http-service/routes \
  -H 'Content-Type: application/json' \
  -d '{"hosts": ["'"${host}"'","127.0.0.1","localhost"]}'
echo ""

# Add a kong service for all linux for health hl7v2 mllp route
curl http://localhost:8001/services \
  -H 'Content-Type: application/json' \
  -d '{"name": "lfh-hl7v2-service", "url": "tcp://'"${host}"':'"${lfhmllp}"'"}'
echo ""

# Add a kong route that matches incoming requests and sends them to the url above
curl http://localhost:8001/services/lfh-hl7v2-service/routes \
  -H 'Content-Type: application/json' \
  -d '{"protocols": ["tcp", "tls"], "destinations": [{"port":'"${kongmllp}"'}]}'
echo ""

echo "Kong configuration complete"

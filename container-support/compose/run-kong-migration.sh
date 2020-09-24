#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# run-kong-migration.sh
# Configures the postgres database for Kong.
# Must be run at least once in the database lifecycle, but subsequent runs are not harmful.

# wait parameters used to determine when the services within a container are available
SLEEP_INTERVAL=5
MAX_CHECKS=10

DB_SERVICE="postgres"
DB_CONFIG_SERVICE="kong-migration"
DB_CONFIG_SERVICE_MESSAGE="Database is up-to-date"

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
docker-compose up "$DB_CONFIG_SERVICE"
is_ready "$DB_CONFIG_SERVICE" "$DB_CONFIG_SERVICE_MESSAGE"

echo "Database configuration complete"

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
MAX_CHECKS=10

source .env

DB_SERVICE="postgres"
DB_SERVICE_MESSAGE="database system is ready to accept connections"
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

function add_http_route() {
  local name=$1
  local method=$2
  local url=$3
  local service=$4
  local admin_url="https://localhost:8444/services/${service}/routes"

  curl --insecure "${admin_url}" \
  -H 'Content-Type: application/json' \
  -d '{"paths": ["'"${url}"'"], "methods": ["'"${method}"'"], "name": "'"${name}"'", "protocols": ["https"], "strip_path": false}'
  echo ""
}

# wait for postgres
docker-compose up -d "$DB_SERVICE"
is_ready "$DB_SERVICE" "$DB_SERVICE_MESSAGE"
wait_for_cmd docker exec -it compose_"$DB_SERVICE"_1 psql --username "${LFH_PG_USER}" -c '\q'

# start kong migration and wait for ephemeral container to complete
docker-compose up -d "$DB_CONFIG_SERVICE"
is_ready "$DB_CONFIG_SERVICE" "$DB_CONFIG_SERVICE_MESSAGE"

# start kong and wait for kong to come up
docker-compose up -d "$KONG_SERVICE"
is_ready "$KONG_SERVICE" "$KONG_SERVICE_MESSAGE"
wait_for_cmd curl --silent --insecure https://localhost:8444/services

# host is how kong needs to reference LFH, depending on where LFH is running
host=${LFH_KONG_LFHHOST}
lfhhttp=${LFH_CONNECT_HTTP_PORT}
lfhmllp=${LFH_CONNECT_MLLP_PORT}
kongmllp=${LFH_KONG_MLLP_PORT}

echo "Adding a kong service for all LinuxForHealth http routes"
curl --insecure https://localhost:8444/services \
  -H 'Content-Type: application/json' \
  -d '{"name": "lfh-http-service", "url": "http://'"${host}"':'"${lfhhttp}"'"}'
echo ""

echo "Adding kong http routes that match incoming requests and send them to the lfh-http-service url"
add_http_route "hello-world-route" "GET" "/hello-world" "lfh-http-service"
add_http_route "fhir-r4-patient-post-route" "POST" "/fhir/r4/Patient" "lfh-http-service"
add_http_route "orthanc-image-post-route" "POST" "/orthanc/instances" "lfh-http-service"
add_http_route "kafka-get-message-route" "GET" "/datastore/message" "lfh-http-service"
add_http_route "x12-post-route" "POST" "/x12" "lfh-http-service"
add_http_route "ccd-post-route" "POST" "/ccd" "lfh-http-service"

echo "Adding a kong service for the LinuxForHealth hl7v2 mllp route"
curl --insecure https://localhost:8444/services \
  -H 'Content-Type: application/json' \
  -d '{"name": "lfh-hl7v2-service", "url": "tcp://'"${host}"':'"${lfhmllp}"'"}'
echo ""

echo "Adding a kong route that matches incoming requests and sends them to the lfh-hl7v2-service url"
curl --insecure https://localhost:8444/services/lfh-hl7v2-service/routes \
  -H 'Content-Type: application/json' \
  -d '{"protocols": ["tcp", "tls"], "destinations": [{"port":'"${kongmllp}"'}]}'
echo ""

echo "Adding a kong service for the LinuxForHealth Blue Button 2.0 routes"
curl --insecure https://localhost:8444/services \
  -H 'Content-Type: application/json' \
  -d '{"name": "lfh-bluebutton-service", "url": "http://'"${host}"':'"${lfhhttp}"'"}'
echo ""

echo "Adding Kong http routes that match incoming requests and send them to the lfh-bluebutton-service url"
add_http_route "bb-authorize-route" "GET" "/bluebutton/authorize" "lfh-bluebutton-service"
add_http_route "bb-patient-route" "GET" "/bluebutton/v1/Patient" "lfh-bluebutton-service"
add_http_route "bb-eob-route" "GET" "/bluebutton/v1/ExplanationOfBenefit" "lfh-bluebutton-service"
add_http_route "bb-coverage-route" "GET" "/bluebutton/v1/Coverage" "lfh-bluebutton-service"

echo "Kong configuration complete"

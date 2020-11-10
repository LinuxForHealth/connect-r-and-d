#!/bin/bash
#
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0
#
# configure-nats.sh
# Configures the NATS JetStream server.

# wait parameters used to determine when the services within a container are available
SLEEP_INTERVAL=2
MAX_CHECKS=10

source .env

function wait_for_cmd() {
    local retry_count=0
    local failed=0
    local cmd="${@}"
    echo ${cmd}

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

# start NATS JetStream
echo "Starting NATS JetStream"
service=$LFH_NATS_SERVICE_NAME
docker-compose up -d --remove-orphans $service

# create JetStream stream
wait_for_cmd docker exec -it compose_"$service"_1 \
              nats --server=compose_"$service"_1:4222 \
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

# create JetStream consumer
docker exec -it compose_"$service"_1 \
              nats --server=compose_"$service"_1:4222 \
              con add EVENTS SUBSCRIBER \
              --ack none \
              --target lfh-events \
              --deliver last \
              --replay instant \
              --filter '' > /dev/null

echo "NATS JetStream configuration complete"

#!/usr/bin/env bash

# load environment variables from compose stack and the oci tools
source ../compose/.env
source .env

docker rm -f ${LFH_CONNECT_SERVICE_NAME}
docker rm -f ${LFH_KAFDROP_SERVICE_NAME}
docker rm -f ${LFH_KAFKA_SERVICE_NAME}
docker rm -f ${LFH_ZOOKEEPER_SERVICE_NAME}
docker rm -f ${LFH_NATS_SERVICE_NAME}

docker network rm ${LFH_NETWORK_NAME}
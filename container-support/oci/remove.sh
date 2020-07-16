#!/usr/bin/env bash
# remove.sh
# removes the LFH network and containers

# load environment variables from compose stack and the oci tools
source ../compose/.env
source .env

sudo podman rm -f ${LFH_CONNECT_SERVICE_NAME}
sudo podman rm -f ${LFH_KAFDROP_SERVICE_NAME}
sudo podman rm -f ${LFH_KAFKA_SERVICE_NAME}
sudo podman rm -f ${LFH_ZOOKEEPER_SERVICE_NAME}
sudo podman rm -f ${LFH_NATS_SERVICE_NAME}

sudo podman network rm ${LFH_NETWORK_NAME}

#!/bin/sh

set -o errexit
set -o nounset
set -o pipefail

oc delete all --selector app=nats-server --wait
oc delete all --selector app=kafdrop --wait
oc delete all --selector app=kafka --wait
oc delete all --selector app=zookeeper --wait
oc delete project --force lfh --wait

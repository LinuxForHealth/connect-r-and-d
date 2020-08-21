#!/bin/sh
# remove-quickstart.sh
# removes all LFH objects, including the project from an OpenShift 4.x cluster.

set -o errexit
set -o nounset
set -o pipefail

oc delete all --selector app=nats-server --wait
oc delete all --selector app=kafdrop --wait
oc delete all --selector app=kafka --wait
oc delete all --selector app=zookeeper --wait
oc delete project --force lfh --wait

#!/bin/sh

set -o errexit
set -o nounset
set -o pipefail

oc delete service,deploymentconfig,imagestream,pod --selector='app=kafdrop' --wait
oc delete service,deploymentconfig,imagestream,pod --selector='app=kafka' --wait
oc delete service,deploymentconfig,imagestream,pod --selector='app=zookeeper' --wait
oc delete project --force lfh --wait

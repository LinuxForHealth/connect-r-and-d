#!/bin/bash
# aro-quickstart.sh
# Install or removes Azure Redhat OpenShift resources within an existing Azure Subscription.
#
# Usage:
# ./aro-quickstart.sh [subscription name] [resource group] [region] [install|remove]
# Requirements:
# - The Azure CLI tool
# - An existing Azure account with the following permissions:
# - - Create app registrations/service principals
# - - Membership in the User Access Administrator role at the "Subscription Level"
#
# Script Parameters (mapped to variables):
# - SUBSCRIPTION_ID: The Azure Subscription ID
# - RESOURCE_GROUP_NAME: The resource group used to support the Azure RedHat OpenShift installation.
#                         The resource group is considered "disposable" as it is removed by the "remove" setup option.
# - REGION_NAME: The Azure Region Name where resources are created.
# - SETUP_OPTION: "install" or "remove"
#
# The aro-quickstart.sh script supports two setup modes, install or remove.
# "install" mode creates a resource group named RESOURCE_GROUP_NAME if the resource group does not exist.
# A service principal, ${SERVICE_PRINCIPAL_NAME}, is created to provision the RedHat OpenShift resources.
#
# "remove" mode deletes the service principal, ${SERVICE_PRINCIPAL_NAME}, and the resource group.

set -o errexit
set -o nounset
set -o pipefail

# script arguments
SUBSCRIPTION_NAME=${1:-""}
RESOURCE_GROUP_NAME=${2:-""}
REGION_NAME=${3:-""}
SETUP_OPTION=${4:-""}

# azure resource identifiers
SUBSCRIPTION_ID=""
RESOURCE_GROUP_ID=""
REGION_ID=""
CLIENT_ID=""
CLIENT_SECRET=""

# variables used in resource creation
SERVICE_PRINCIPAL_NAME="http://lfh-aro-sp"
LFH_ARO_VNET_NAME="lfh-aro-vnet"
LFH_ARO_VNET_PREFIX="10.0.0.0/22"
LFH_ARO_MASTER_NODE_SUBNET_NAME="lfh-aro-master-subnet"
LFH_ARO_MASTER_SUBNET_PREFIX="10.0.0.0/23"
LFH_ARO_WORKER_SUBNET_NAME="lfh-aro-worker-subnet"
LFH_ARO_WORKER_SUBNET_PREFIX="10.0.2.0/23"
LFH_ARO_CLUSTER_NAME="lfh-aro-cluster"
LFH_ARO_MASTER_VM_SIZE="Standard_D8s_v3"
LFH_ARO_WORKER_VM_SIZE="Standard_D4s_v3"
LFH_ARO_CLUSTER_WORKER_COUNT=3

function validateAzureResourceId() {
  # validates that an id field is set
  local ID_FIELD=$1
  local RESOURCE_NAME=$2
  local RESOURCE_TYPE=$3

  if [[ -z "${ID_FIELD}" ]]; then
    echo "Azure ${RESOURCE_TYPE}:${RESOURCE_NAME} was not found"
    exit 1
  fi
}

function init() {
  # validates script parameters
  # sets the "context" for the script including Azure Subscription and Resource Group

  if [[ "${SETUP_OPTION}" != "install" && "${SETUP_OPTION}" != "remove" ]]; then
    echo "Invalid setup option ${SETUP_OPTION:- "blank"}. Expecting either install or remove"
    exit 1
  fi

  SUBSCRIPTION_ID=$(az account list --query "[?name == '"${SUBSCRIPTION_NAME}"'].id" --output tsv)
  validateAzureResourceId "${SUBSCRIPTION_ID}" "${SUBSCRIPTION_NAME}" "Subscription"

  az account set --subscription "${SUBSCRIPTION_ID}"

  REGION_ID=$(az account list-locations --query "[?name == '"${REGION_NAME}"'].id" --output tsv)
  validateAzureResourceId "${REGION_ID}" "${REGION_NAME}" "Region"

  RESOURCE_GROUP_ID=$(az group list --query "[?name == '"${RESOURCE_GROUP_NAME}"'].id" --output tsv)
  if [[ -z "${RESOURCE_GROUP_ID}" && "${SETUP_OPTION}" == "install" ]]; then
    echo "Resource Group ${RESOURCE_GROUP_NAME} does not exist, and will be created"
    az group create --location ${REGION_NAME} --name ${RESOURCE_GROUP_NAME}
    RESOURCE_GROUP_ID=$(az group list --query "[?name == '"${RESOURCE_GROUP_NAME}"'].id" --output tsv)
  fi

  az provider register -n Microsoft.RedHatOpenShift --wait

  echo "================================================================================================="
  echo "Initializing Azure CLI Subscription: ${SUBSCRIPTION_NAME}, Resource Group: ${RESOURCE_GROUP_NAME}"
  echo "Subscription ID: ${SUBSCRIPTION_ID}"
  echo "Resource Group ID: ${RESOURCE_GROUP_ID}"
  echo "Region: ${REGION_NAME}"
  echo "Setup Option: ${SETUP_OPTION}"
}

function install() {
  # installs the quickstart service principal and Azure RedHat OpenShift resources
  echo "================================================================================================="
  echo "Provisioning Azure RedHat OpenShift"

  echo "================================================================================================="
  echo "Creating Azure VNET"

  az network vnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --name "${LFH_ARO_VNET_NAME}" \
    --address-prefixes "${LFH_ARO_VNET_PREFIX}"

  az network vnet subnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --vnet-name "${LFH_ARO_VNET_NAME}" \
    --name "${LFH_ARO_MASTER_NODE_SUBNET_NAME}" \
    --address-prefixes "${LFH_ARO_MASTER_SUBNET_PREFIX}" \
    --service-endpoints Microsoft.ContainerRegistry

  az network vnet subnet update \
    --name "${LFH_ARO_MASTER_NODE_SUBNET_NAME}" \
    --resource-group "${RESOURCE_GROUP_NAME}"  \
    --vnet-name "${LFH_ARO_VNET_NAME}" \
    --disable-private-link-service-network-policies true

  az network vnet subnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --vnet-name "${LFH_ARO_VNET_NAME}" \
    --name "${LFH_ARO_WORKER_SUBNET_NAME}" \
    --address-prefixes "${LFH_ARO_WORKER_SUBNET_PREFIX}" \
    --service-endpoints Microsoft.ContainerRegistry

  echo "================================================================================================="
  echo "Creating Azure Service Principal"

  TEMP_CREDENTIALS=$(az ad sp create-for-rbac --role Contributor \
    --name "${SERVICE_PRINCIPAL_NAME}" \
    --scopes "${RESOURCE_GROUP_ID}" \
    --query "[appId,password]" \
    --output tsv)

  TEMP_CREDENTIALS=$(echo "${TEMP_CREDENTIALS}"| tr '\n' ' ' | xargs)

  CLIENT_ID=$(echo ${TEMP_CREDENTIALS} | cut -d ' ' -f1)
  CLIENT_SECRET=$(echo ${TEMP_CREDENTIALS} | cut -d ' ' -f2)

  echo "Parsed client credentials"
  echo "client id ${CLIENT_ID}"
  echo "client secret ${CLIENT_SECRET}"

  echo "================================================================================================="
  echo "Creating Azure RedHat OpenShift Cluster"

  az aro create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --name "${LFH_ARO_CLUSTER_NAME}" \
    --vnet "${LFH_ARO_VNET_NAME}" \
    --master-subnet "${LFH_ARO_MASTER_NODE_SUBNET_NAME}" \
    --worker-subnet "${LFH_ARO_WORKER_SUBNET_NAME}" \
    --client-id "${CLIENT_ID}" \
    --client-secret "${CLIENT_SECRET}" \
    --location "${REGION_NAME}" \
    --master-vm-size "${LFH_ARO_MASTER_VM_SIZE}" \
    --worker-vm-size "${LFH_ARO_WORKER_VM_SIZE}" \
    --worker-count "${LFH_ARO_CLUSTER_WORKER_COUNT}" \
    --no-wait
}

function remove() {
  # removes the quickstart service principal and resource group
  echo "================================================================================================="
  echo "Removing Azure RedHat OpenShift Resources"
  echo "================================================================================================="

  az ad sp delete --id "${SERVICE_PRINCIPAL_NAME}"
  az group delete --yes --name "${RESOURCE_GROUP_NAME}"
}

init

if [[ ${SETUP_OPTION} == "install" ]]; then
  install
else
  remove
fi

#!/bin/bash
# aro-quickstart.sh
# Install or removes Azure Redhat OpenShift resources (OCP) within an existing Azure Subscription.
#
# Usage:
# ./aro-quickstart.sh [subscription name] [resource group name] [region name] [install|remove|connection-info]
#
# Requirements:
# - Azure CLI tool
# - An existing Azure account with the following permissions:
# - - Create app registrations/service principals
# - - Membership in the User Access Administrator role at the "Subscription Level"
# - Subscription Limits/Quotes:
# - - Minimum of 40 vCPUs/Cores are required for the OpenShift Cluster (DsV3 family)
#
# Script Parameters (mapped to variables):
# - SUBSCRIPTION_ID: The Azure Subscription ID
# - RESOURCE_GROUP_NAME: The resource group used to support the Azure OCP installation.
#                         The resource group is considered "disposable" as it is removed by the "remove" setup option.
# - REGION_NAME: The Azure Region Name where resources are created.
# - SETUP_MODE: One of "install", "remove", or "connection-info"
#
# The aro-quickstart.sh script supports three setup modes, install, remove, and connection-info.
#
# "install" mode creates a resource group named RESOURCE_GROUP_NAME if the resource group does not exist.
# A service principal, ${SERVICE_PRINCIPAL_NAME}, is created to provision the RedHat OpenShift resources.
#
# "remove" mode deletes all the entire OCP cluster and related Azure resources (service principal, resource groups, etc)
#
# "connection-info" is intended for use after the cluster is provisioned. "connection-info" returns cluster credentials,
# and URLs for the OpenShift console and api.
#
# Note: Access to Red Hat repositories within the cluster is not supported at this time.

set -o errexit
set -o nounset
set -o pipefail

# script arguments
SUBSCRIPTION_NAME=${1:-""}
RESOURCE_GROUP_NAME=${2:-""}
REGION_NAME=${3:-""}
SETUP_MODE=${4:-""}

# azure resource identifiers
SUBSCRIPTION_ID=""
RESOURCE_GROUP_ID=""
REGION_ID=""
CLIENT_ID=""
CLIENT_SECRET=""

DISPLAY_BREAK="================================================================================================="

# variables used in resource creation
SERVICE_PRINCIPAL_NAME="http://lfh-aro-sp"
ARO_VNET_NAME="lfh-aro-vnet"
ARO_VNET_PREFIX="10.0.0.0/22"
ARO_MASTER_NODE_SUBNET_NAME="lfh-aro-master-subnet"
ARO_MASTER_SUBNET_PREFIX="10.0.0.0/23"
ARO_WORKER_SUBNET_NAME="lfh-aro-worker-subnet"
ARO_WORKER_SUBNET_PREFIX="10.0.2.0/23"
ARO_CLUSTER_NAME="lfh-aro-cluster"
ARO_CLUSTER_RESOURCE_GROUP_NAME="lfh-aro-cluster-rg"
ARO_MASTER_VM_SIZE="Standard_D8s_v3"
ARO_WORKER_VM_SIZE="Standard_D4s_v3"
ARO_CLUSTER_WORKER_COUNT=3

function validateAzureResourceId() {
  # validates that an id field is set
  local ID_FIELD="$1"
  local RESOURCE_NAME="$2"
  local RESOURCE_TYPE="$3"

  if [ -z "${ID_FIELD}" ]; then
    echo "Azure ${RESOURCE_TYPE}:${RESOURCE_NAME} was not found"
    exit 1
  fi
}

function init() {
  # validates script parameters
  # sets the "context" for the script including Azure Subscription and Resource Group
  if [ "${SETUP_MODE}" != "install" ] && [ "${SETUP_MODE}" != "remove" ] && [ "${SETUP_MODE}" != "connection-info" ]; then
    echo "Invalid setup mode ${SETUP_MODE:- "blank"}. Expecting one of: install, remove, or connection-info"
    exit 1
  fi

  SUBSCRIPTION_ID=$(az account list --query "[?name == '"${SUBSCRIPTION_NAME}"'].id" --output tsv)
  validateAzureResourceId "${SUBSCRIPTION_ID}" "${SUBSCRIPTION_NAME}" "Subscription"
  az account set --subscription "${SUBSCRIPTION_ID}"

  REGION_ID=$(az account list-locations --query "[?name == '"${REGION_NAME}"'].id" --output tsv)
  validateAzureResourceId "${REGION_ID}" "${REGION_NAME}" "Region"

  RESOURCE_GROUP_ID=$(az group list --query "[?name == '"${RESOURCE_GROUP_NAME}"'].id" --output tsv)
  if [ "${SETUP_MODE}" = "remove" ]; then
    validateAzureResourceId "${RESOURCE_GROUP_ID}" "${RESOURCE_GROUP_NAME}" "Resource Group"
  elif [ "${SETUP_MODE}" = "install" ] && [ -z "${RESOURCE_GROUP_ID}" ]; then
    echo "Resource Group ${RESOURCE_GROUP_NAME} does not exist, and will be created"
    az group create --location ${REGION_NAME} --name ${RESOURCE_GROUP_NAME}
    RESOURCE_GROUP_ID=$(az group list --query "[?name == '"${RESOURCE_GROUP_NAME}"'].id" --output tsv)
  fi

  echo "${DISPLAY_BREAK}"
  echo "Initializing Azure CLI Subscription: ${SUBSCRIPTION_NAME}, Resource Group: ${RESOURCE_GROUP_NAME}"
  echo "Subscription ID: ${SUBSCRIPTION_ID}"
  echo "Resource Group ID: ${RESOURCE_GROUP_ID}"
  echo "Region: ${REGION_NAME}"
  echo "Setup Mode: ${SETUP_MODE}"
}

function install() {
  # installs the quickstart service principal and Azure RedHat OpenShift resources
  echo "${DISPLAY_BREAK}"
  echo "Provisioning Azure RedHat OpenShift"

  echo "${DISPLAY_BREAK}"
  echo "Creating Azure VNET"

  az network vnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --name "${ARO_VNET_NAME}" \
    --address-prefixes "${ARO_VNET_PREFIX}"

  az network vnet subnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --vnet-name "${ARO_VNET_NAME}" \
    --name "${ARO_MASTER_NODE_SUBNET_NAME}" \
    --address-prefixes "${ARO_MASTER_SUBNET_PREFIX}" \
    --service-endpoints Microsoft.ContainerRegistry

  az network vnet subnet update \
    --name "${ARO_MASTER_NODE_SUBNET_NAME}" \
    --resource-group "${RESOURCE_GROUP_NAME}"  \
    --vnet-name "${ARO_VNET_NAME}" \
    --disable-private-link-service-network-policies true

  az network vnet subnet create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --vnet-name "${ARO_VNET_NAME}" \
    --name "${ARO_WORKER_SUBNET_NAME}" \
    --address-prefixes "${ARO_WORKER_SUBNET_PREFIX}" \
    --service-endpoints Microsoft.ContainerRegistry

  echo "${DISPLAY_BREAK}"
  echo "Creating Azure Service Principal"

  CLIENT_SECRET=$(az ad sp create-for-rbac --role Contributor \
    --name "${SERVICE_PRINCIPAL_NAME}" \
    --scopes "${RESOURCE_GROUP_ID}" \
    --query "[password]" \
    --output tsv)

  CLIENT_ID=$(az ad sp list --query "[?contains(servicePrincipalNames, '"${SERVICE_PRINCIPAL_NAME}"')].appId" --output tsv)

  echo "Parsed client credentials"
  echo "client id ${CLIENT_ID}"
  echo "client secret ${CLIENT_SECRET}"

  echo "${DISPLAY_BREAK}"
  echo "Creating Azure RedHat OpenShift Cluster"

  az provider register -n Microsoft.RedHatOpenShift --wait

  az aro create \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --cluster-resource-group "${ARO_CLUSTER_RESOURCE_GROUP_NAME}" \
    --name "${ARO_CLUSTER_NAME}" \
    --vnet "${ARO_VNET_NAME}" \
    --master-subnet "${ARO_MASTER_NODE_SUBNET_NAME}" \
    --worker-subnet "${ARO_WORKER_SUBNET_NAME}" \
    --client-id "${CLIENT_ID}" \
    --client-secret "${CLIENT_SECRET}" \
    --location "${REGION_NAME}" \
    --master-vm-size "${ARO_MASTER_VM_SIZE}" \
    --worker-vm-size "${ARO_WORKER_VM_SIZE}" \
    --worker-count "${ARO_CLUSTER_WORKER_COUNT}" \
    --no-wait
}

function remove() {
  # removes the quickstart service principal and resource group
  echo "${DISPLAY_BREAK}"
  echo "Removing Azure RedHat OpenShift Resources"
  echo "${DISPLAY_BREAK}"

  az aro delete --yes --resource-group "${RESOURCE_GROUP_NAME}" --name "${ARO_CLUSTER_NAME}" --no-wait
  az ad sp delete --id "${SERVICE_PRINCIPAL_NAME}"
  az group delete --yes --name "${RESOURCE_GROUP_NAME}"
}

function connection_info() {
  # prints OpenShift Cluster credentials and URLs used for access
  echo "${DISPLAY_BREAK}"
  echo "Cluster Credentials:"
  az aro list-credentials --name "${ARO_CLUSTER_NAME}" \
    --resource-group "${RESOURCE_GROUP_NAME}"
  echo "${DISPLAY_BREAK}"

  echo "Console URL:"
  az aro show --name "${ARO_CLUSTER_NAME}" \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --query "consoleProfile.url" -o tsv
  echo "${DISPLAY_BREAK}"

  echo "Server/API URL:"
  az aro show --name "${ARO_CLUSTER_NAME}" \
    --resource-group "${RESOURCE_GROUP_NAME}" \
    --query "apiserverProfile.url" -o tsv
  echo "${DISPLAY_BREAK}"
}

init

case "${SETUP_MODE}" in
  "install")
    install
    ;;
  "remove")
    remove
    ;;
  "connection-info")
    connection_info
    ;;
esac

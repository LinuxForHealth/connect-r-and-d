# Linux for Health OpenShift Support

LFH provides "QuickStart" scripts to provision an [Open Shift Container Platform (OCP) cluster](https://www.openshift.com/), and install LFH resources within the OCP cluster.
Supported cloud platforms include Azure, with additional platforms coming soon!


## Pre-requisites

- [Register](https://developers.redhat.com/register) for a free Red Hat Developer Account.
- Download the OC CLI toolkit from the RedHat developer site
- Optional: Download the Azure CLI toolkit if working with OCP on Azure.

## Provision OCP cluster on Azure

The `aro-quickstart.sh` script provisions a working Azure RedHat Open Shift (ARO).  Setting up ARO requires the following:

- Azure "users" are able to create application registrations and service principals.
- An Azure account that is a member of the "User Access Administrator" role within the Subscription.
- A raised Compute/CPU quota of at least 40 for the DsV3 family of VMs within the desired region.

Login to Azure
```shell script
# the CLI will open the default browser and authenticate using the Azure portal
az login
```

To create the OCP cluster
```shell script
# the script will return once the provisioning requests are sent to the Azure Resource Manager
# the process takes approximately 30 - 40 minutes to complete
./aro-quickstart [Subscription Name] [Resource Group Name] [Region Name] install
```

When provisioning is complete, view connection information
```shell script
# returns credentials, and URLs for the console and api server
./aro-quickstart [Subscription Name] [Resource Group Name] [Region Name] connection-info
```

To remove/delete the OCP quickstart
```shell script
./aro-quickstart [Subscription Name] [Resource Group Name] [Region Name] remove
# the script will return once the deletion requests are sent to the Azure Resource Manager, and provide commands
# to monitor deletion progress and remove remaining resources.
```

## Provision LFH Within an Existing OCP Cluster

The `lfh-quickstart.sh` script provisions LFH within an existing OCP cluster. The script supports any OCP cluster environment including Azure, RedHat, AWS, or lcocally with Code Ready Containers (CRC).

Install the LFH quickstart
```shell script
# login (if not currently authenticated)
oc login -u [username] -p [password] [openshift api endpoint]
./lfh-quickstart.sh install
```

Remove the LFH quickstart
```shell script
# login (if not currently authenticated)
oc login -u [username] -p [password] [openshift api endpoint]
./lfh-quickstart.sh remove
```

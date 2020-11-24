# Linux for Health OpenShift Support

Linux for Health provides separate scripts to provision an [OpenShift Container Platform (OCP) cluster](https://www.openshift.com/), and create a LFH OpenShift project.
OCP provisioning support is available for Azure using [Azure's Red Hat OpenShift (ARO) service](https://azure.microsoft.com/en-us/services/openshift/), with additional platforms coming soon!
The provisioning and project creation scripts are independent of one another. In other words the LFH project creation script does not directly depend on a LFH OCP provisioning script, but it does require an existing OCP cluster running on a supported platform (Red Hat, AWS, Azure, Code Ready Containers, private, etc).  

## Pre-requisites

- [Register](https://developers.redhat.com/register) for a free Red Hat Developer Account.
- Download the OC CLI toolkit from the Red Hat developer site

## Create LFH Project Within an Existing OCP Cluster

The `lfh-quickstart.sh` script provisions LFH within an existing OCP cluster. The script supports any supported OCP cluster environment, and locally with Code Ready Containers (CRC).

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

## Provision OCP cluster on Azure

The `aro-quickstart.sh` script provisions a working Azure Red Hat OpenShift (ARO) installation. ARO setup requirements include:

- An Azure account that is a member of the "User Access Administrator" role within the Subscription.
- The Azure account is able to create application registrations and service principals.
- A raised Compute/CPU quota of at least 40 for the DsV3 family of VMs within the desired region.
- An Azure CLI installation on the machine running the `aro-quickstart.sh` script.

To create the cluster, complete the following steps:

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

Create the LFH OpenShift project as [outlined above](#create-lfh-project-within-an-existing-ocp-cluster)

To remove/delete the OCP quickstart
```shell script
./aro-quickstart [Subscription Name] [Resource Group Name] [Region Name] remove
# the script will return once the deletion requests are sent to the Azure Resource Manager, and provide commands
# to monitor deletion progress and remove remaining resources.
```


# Linux for Health OpenShift Setup

LFH provides command line scripts to support provisioning LFH applications and services on [Open Shift](https://www.openshift.com/).

LFH supports two installation options. The first is a "quick-start" script which leverages the [oc](https://docs.openshift.com/container-platform/4.3/cli_reference/openshift_cli/getting-started-cli.html) cli and its "reasonable" defaults.
The second option is configuration-based, and supports customizations. This second option is in active development and will be delivered in a future release.

## Pre-requisites

- [Register](https://developers.redhat.com/register) for a free Red Hat Developer Account.
- Download the OC CLI toolkit from the RedHat developer site

## Quick Start

Install the LFH quickstart on the OpenShift Container Platform (OCP)
```shell script
# login
oc login -u [username] -p [password] [openshift api endpoint]
./install-quickstart.sh
```

Remove the LFH quickstart
```shell script
# login
oc login -u [username] -p [password] [openshift api endpoint]
./remove-quickstart.sh
```
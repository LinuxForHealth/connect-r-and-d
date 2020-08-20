# Linux for Health OCI (Open Container Initiative) Scripts

The Linux for Health OCI scripts support LFH containers within OCI compliant tools such as [podman](http://docs.podman.io/en/latest/Introduction.html)
and [docker](https://www.docker.com/)

## Script Overview

The OCI scripts source configuration data, typically image coordinates and ports from the [docker compose env](../compose/.env)
and the [OCI env](.env). Consult these scripts to update configuration settings as needed.

## Review the OCI_COMMAND
The OCI_COMMAND is set to execute podman as root. Please adjust this command prior to use, based on your system's
installation.

```shell script
OCI_COMMAND="sudo podman"
```  

Linux for Health provides OCI scripts for starting and removing the container environment. Please consult the appropriate
implementation specific documentation for the commands used to administer and interact with the container environment.

## Starting Containers

```shell script
./start.sh
```

## Removing Containers

```shell script
./remove.sh
```
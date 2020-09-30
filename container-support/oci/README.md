# Linux for Health OCI (Open Container Initiative) Scripts

The Linux for Health OCI script supports LFH containers within OCI compliant tools such as [podman](http://docs.podman.io/en/latest/Introduction.html)
and [docker](https://www.docker.com/)

## Script Configuration

The OCI script sources configuration data, typically image coordinates and ports from the [docker compose env](../compose/.env)
and the [OCI env](.env). Review these files as needed to update configuration settings.

## Script Usage

The OCI script accepts two arguments, the SERVICE_OPERATION and OCI_COMMAND.

```shell script
./lfh-oci-services.sh [SERVICE_OPERATION] [OCI_COMMAND]
```

| Argument Name | Argument Description |
| :--- | :--- |
| SERVICE_OPERATION | Valid values include: start and remove. |
| OCI_COMMAND | The command, or executable used to execute container commands. Defaults to "docker". |

## Starting Containers with Docker

```shell script
./lfh-oci-services.sh start
```

## Removing Containers with Docker

```shell script
./lfh-oci-services.sh remove
```

## Starting Containers with "rooted" Podman

```shell script
./lfh-oci-services.sh start "sudo podman"
```

## Removing Containers with "rooted" Podman

```shell script
./lfh-oci-services.sh remove "sudo podman"
```

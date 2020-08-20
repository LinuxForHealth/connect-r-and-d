# Linux for Health Docker Compose Stacks

Docker Compose supports various application stacks through the use Docker Compose's [COMPOSE_FILE](https://docs.docker.com/compose/reference/envvars/#compose_file) environment variable.
The `start-stack.sh` script launches Linux for Health Docker Compose stacks.

start-stack.sh usage
```shell script
# it is recommend to execute the script within the current shell to ensure that the docker-compose CLI behaves
# as expected following service startup
. ./start-stack.sh [stack name]
```

## Supported Stacks
| Stack Name | Stack Description |
| :--- | :--- |
| dev | For local development use. Runs LFH supporting services with port mappings. |
| server | For deployment environments or integrated testing. Includes the LFH connect application and supporting services. |
| pi | Similar to the server stack. Optimized for arm64/Raspberry Pi usage. |

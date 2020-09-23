# Linux for Health Docker Compose

LFH supports multiple "profiles" through the use of Docker Compose's [COMPOSE_FILE](https://docs.docker.com/compose/reference/envvars/#compose_file) environment variable.
The `start-stack.sh` script launches the Linux for Health Docker Compose stack for the specified profile.

start-stack.sh usage
```shell script
# execute the script within the current environment to ensure the docker-compose CLI starts all requested services
. ./start-stack.sh [profile name]
# OR
source start-stack.sh [profile name]
```

Use standard docker-compose commands to stop and remove the stack.
Execute these commands in the same session used to start the stack.

Stop the docker-compose stack, leaving containers intact.
```shell script
docker-compose stop
```

Stop the docker-compose stack and remove containers and volumes
```shell script
docker-compose down -v
```

## Supported Profiles
| Profile Name | Profile Description |
| :--- | :--- |
| dev | For local development use. Runs LFH supporting services with port mappings. |
| server | For deployment environments or integrated testing. Includes the LFH connect application and supporting services. |
| pi | Similar to the server stack. Optimized for arm64/Raspberry Pi usage. |

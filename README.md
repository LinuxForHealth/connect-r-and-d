# iDAAS-Connect
iDAAS Connectors for Inboud Data Processing

Powered by [Apache Camel](https://camel.apache.org/)

## Overview
iDAAS Connect provides an opinionated implementation of [Camel Routing](https://camel.apache.org/manual/latest/routes.html), focused
on standard data and messaging formats. iDAAS Connect data processing routes are dynamically built from [application properties](src/main/resources/application.properties)
configurations.

An iDAAS Connect data processing route consists of:
- A consumer endpoint which receives inbound data
- Optional "components" used to transform, filter, or route data
- Error handling and auditing 
- A Kafka based producer endpoint, used to store data messages
- Additional optional producer endpoints to support "multi-cast" routes when needed

For additional information on iDAAS Connect route configuration, please refer to the [Route Configuration README](ROUTE-CONFIGURATION.md)

## Pre-requisites
- Java 1.8
- [Gradle](https://gradle.org/) 6.x

## Getting Started 
Clone the repo and build the project (including tests)
```sh
# clone the repo and confirm the build
git clone https://github.com/idaas-connect/idaas-connect
cd idaas-connect
./gradlew build
```
The Test Summary is available within the project's [build directory](./build/reports/tests/test/index.html)

## The Development Environment
The development environment provides additional systems and integration targets via a [Docker Compose configuration](docker-compose.yml).

Systems include:
- [Kafdrop](https://github.com/obsidiandynamics/kafdrop) - A Kafka Cluster Viewer
- Kafka - For real time streaming and messaging

Additional systems will be added to support the MVP as needed.

To start the docker compose stack
```sh
docker-compose up -d
```

To tail current processing logs
```sh
docker-compose logs -f 
```

For additional Docker Compose commands, please refer to the [Official Documentation](https://docs.docker.com/compose/reference/overview/)

To access Kafdrop, the Kafka Cluster View, browse to http://localhost:9000

To start the iDAAS Connect application
```
./gradlew run
```

To list all available Gradle tasks
```
./gradlew tasks
```
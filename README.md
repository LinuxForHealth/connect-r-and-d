# iDAAS-Connect
iDAAS Connectors for Inboud Data Processing

Powered by [Apache Camel](https://camel.apache.org/)

## Pre-requisites
- Java 1.8
- [Gradle](https://gradle.org/) 6.x

## MVP Features
- Consolidate inbound processing routes aka "connectors" defined in [iDAAS Connect Clinical Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-ThirdParty), [iDAAS Connect Clinical Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-IndustryStandards), [iDAAS Connect Finanacial Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-IndustryStandards), and [iDAAS Connect Financial Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-ThirdParty).
- Configuration based features to support route declaration and deployment.
- Minimal dependency build using build properties.
- Container build support targeting [Red Hat Universal Base Image](https://developers.redhat.com/products/rhel/ubi/)
- Build plugin support for K8s, OpenShift, Azure, and AWS deployments.

MVP features are tracked using a `mvp` branch which is updated as feature branches are merged. Please refer to the `mvp` branch's README for updated information on available features and testing.

## Starting the Development Environment
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

To access Kafdrop, the Kafka Cluster View, browser to http://localhost:9000

To start the iDAAS Connect application
```
./gradlew clean run
```

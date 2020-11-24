#!/bin/bash

docker cp create-topics.sh idaas-connect_kafka_1:/opt/kafka_2.12-2.3.0/bin
docker cp hl7-msgs.txt idaas-connect_kafka_1:/opt/kafka_2.12-2.3.0/bin
docker cp fhir-msgs.txt idaas-connect_kafka_1:/opt/kafka_2.12-2.3.0/bin

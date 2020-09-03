#!/bin/bash

while read p; do
  topic=HL7v2_$p
  echo $topic
  ./kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic $topic
done < ./hl7-msgs.txt

while read p; do
  topic=FHIR_R4_$(echo $p | tr '[:lower:]' '[:upper:]')
  echo $topic
  ./kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic $topic
done < ./fhir-msgs.txt

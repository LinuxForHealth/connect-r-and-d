## Create Kafka Topics

1. Start Linux for Health connect:  

```
./gradlew start
```

2. In the connect/utils directory, run:  

```
./cp-docker.sh
```
This will copy the 3 files needed to create the topics into the Kafka container.  

3. Exec into the Kafka container:  

```
docker exec -it idaas-connect_kafka_1 /bin/sh
```
4. cd to the Kafka tools dir:  

```
cd /opt/kafka_2.12-2.3.0/bin
```
5. run the script to create the topics:  

```
./create-topics.sh
```
This will take a few minutes.
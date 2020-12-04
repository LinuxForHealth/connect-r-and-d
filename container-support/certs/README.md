# LinuxForHealth certificate and store generation

The LinuxForHealth container-support/certs directory contains the scripts required to generate self-signed certs needed for LinuxForHealth, along with a Java truststore and keystore. Follow the instructions below to re-generate and install the LinuxForHealth certs and stores.

## Generate the certs, truststore and keystore

Run the following commands to generate and install the LinuxForHealth certs, truststore and keystore.

```shell script
cd container-support/certs
./clean.sh
./mk-certs.sh
cp *.jks ../../src/main/resources
```

Note: When asked for information for input, just hit return as the defaults have already been provided.

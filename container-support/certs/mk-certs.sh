#
# (C) Copyright IBM Corp. 2020
#
# SPDX-License-Identifier: Apache-2.0
#
# make-certs.sh
# Creates the LinuxForHealth certificates required to enable TLS.
#
PASSWORD=change-password

OPENSSL=`which openssl`
if [ -z "$OPENSSL" ]; then
    echo "Please install openssl."
    exit 1
fi

KEYTOOL=`which keytool`
if [ -z "$KEYTOOL" ]; then
    echo "Please ensure keytool from the Java JDK is available."
    exit 1
fi

echo "Creating the LinuxForHealth rootCA certificate"
openssl req -nodes -x509 -newkey rsa:4096 -sha256 -days 3650 -keyout rootCA.key \
    -out rootCA.crt -passout pass:$PASSWORD -config ./ca.cnf

echo "Creating a signing request for the LinuxForHealth server certificate"
openssl req -nodes -newkey rsa:2048 -sha256 -out servercert.csr \
    -keyout server.key -subj "/C=US/ST=Texas/L=Austin/O=LinuxForHealth/CN=linuxforhealth.org" \
    -config ./server.cnf

echo "Signing the LinuxForHealth server certificate"
openssl ca -batch -config ca.cnf -policy signing_policy -extensions signing_req -out server.crt \
    -infiles servercert.csr

echo "Creating a signing request for the LinuxForHealth NATS server certificate"
openssl req -nodes -newkey rsa:2048 -sha256 -out natsservercert.csr \
    -keyout nats-server.key -subj "/C=US/ST=Texas/L=Austin/O=LinuxForHealth/CN=linuxforhealth.org" \
    -config ./nats-server.cnf

echo "Signing the LinuxForHealth NATS server certificate"
openssl ca -batch -config ca.cnf -policy signing_policy -extensions signing_req -out nats-server.crt \
    -infiles natsservercert.csr

echo "Creating the java trust store"
keytool -keystore lfhtruststore.jks -alias CARoot -import -file ./rootCA.crt -noprompt \
    -storetype pkcs12 -storepass $PASSWORD

echo "Importing the Blue Button sandbox cert into the truststore"
keytool -keystore lfhtruststore.jks -alias BlueButtonSandbox -import -file ./test.cms.gov.cer \
    -noprompt -storetype pkcs12 -storepass $PASSWORD

echo "Creating the java key store and importing the LFH server cert"
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -CAfile rootCA.crt \
    -name server -caname CARoot -passout pass:$PASSWORD
keytool -importkeystore -srckeystore server.p12 -srcstoretype pkcs12 -deststoretype pkcs12 \
    -alias server -destkeystore lfhkeystore.jks -srcstorepass $PASSWORD -deststorepass $PASSWORD

echo "Importing the NATS server cert into the keystore"
openssl pkcs12 -export -in nats-server.crt -inkey nats-server.key -out nats-server.p12 \
    -name nats-server -CAfile rootCA.crt -caname CARoot -passout pass:$PASSWORD
keytool -importkeystore -srckeystore nats-server.p12 -srcstoretype pkcs12 -alias nats-server \
    -destkeystore lfhkeystore.jks -srcstorepass $PASSWORD -deststorepass $PASSWORD

echo "Importing the CA cert into the keystore"
keytool -keystore lfhkeystore.jks -alias CARoot -import -file ./rootCA.crt \
    -noprompt -storepass $PASSWORD

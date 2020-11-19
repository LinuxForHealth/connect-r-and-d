/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/*
 * Modified from NATS documentation @ https://docs.nats.io/developing-with-nats/security/tls
 *
 * This implementation requires certificates to be in the java keystore format (.jks).
 * openssl is used to generate a pkcs12 file (.p12) from client-cert.pem and client-key.pem.
 * The resulting file is then imported into the java keystore using keytool.
 * keytool is also used to import the CA certificate into the truststore.
 */
class SSLUtils {

    static KeyStore loadKeystore(String path, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");

        try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));) {
          store.load(in, password.toCharArray());
        }
        return store;
    }

    static KeyManager[] createTestKeyManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty("keystore"), tlsProps.getProperty("keystorePwd"));
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, tlsProps.getProperty("keystorePwd").toCharArray());
        return factory.getKeyManagers();
    }

    static TrustManager[] createTestTrustManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty("truststore"), tlsProps.getProperty("truststorePwd"));
        TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    static SSLContext createSSLContext(Properties tlsProps) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(createTestKeyManagers(tlsProps), createTestTrustManagers(tlsProps), new SecureRandom());
        return ctx;
    }
}

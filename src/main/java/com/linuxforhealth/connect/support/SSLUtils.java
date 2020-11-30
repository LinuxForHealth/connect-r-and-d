/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.support;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.camel.main.Main;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Modified from NATS documentation @ https://docs.nats.io/developing-with-nats/security/tls
 *
 * This implementation requires certificates to be in the java keystore format (.jks).
 * openssl is used to generate a pkcs12 file (.p12) from client-cert.pem and client-key.pem.
 * The resulting file is then imported into the java keystore using keytool.
 * keytool is also used to import the CA certificate into the truststore.
 */
class SSLUtils {
    private static String truststore = "lfh.connect.ssl.truststore.filename";
    private static String truststorePwd = "lfh.connect.ssl.truststore.password";
    private static String keystore = "lfh.connect.ssl.keystore.filename";
    private static String keystorePwd = "lfh.connect.ssl.keystore.password";

    private final static Logger logger = LoggerFactory.getLogger(SSLUtils.class);

    static KeyStore loadKeystore(String file, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");

        try {
            InputStream in = ClassLoader.getSystemResourceAsStream(file);
            store.load(in, password.toCharArray());
        } catch (Exception ex) {
            logger.error("Error loading keystore: "+ex);
        }
        return store;
    }

    static KeyManager[] createTestKeyManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty(keystore), tlsProps.getProperty(keystorePwd));
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, tlsProps.getProperty(keystorePwd).toCharArray());
        return factory.getKeyManagers();
    }

    static TrustManager[] createTestTrustManagers(Properties tlsProps) throws Exception {
        KeyStore store = loadKeystore(tlsProps.getProperty(truststore), tlsProps.getProperty(truststorePwd));
        TrustManagerFactory factory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    static SSLContext createSSLContext(Properties tlsProps, Main camelMain) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(createTestKeyManagers(tlsProps), createTestTrustManagers(tlsProps), new SecureRandom());
        camelMain.bind("sslContextParameters", createSSLContextParameters(tlsProps));
        return ctx;
    }

    static SSLContextParameters createSSLContextParameters(Properties tlsProps) {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(tlsProps.getProperty(keystore));
        ksp.setPassword(tlsProps.getProperty(keystorePwd));
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(tlsProps.getProperty(keystorePwd));
        kmp.setKeyStore(ksp);
        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(tlsProps.getProperty(truststore));
        tsp.setPassword(tlsProps.getProperty(truststorePwd));
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);
        SSLContextParameters sslCtxParams = new SSLContextParameters();
        sslCtxParams.setKeyManagers(kmp);
        sslCtxParams.setTrustManagers(tmp);

        return sslCtxParams;
    }
}

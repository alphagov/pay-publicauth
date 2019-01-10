package uk.gov.pay.publicauth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

class TrustStoreLoader {
    private static final Logger logger = LoggerFactory.getLogger(TrustStoreLoader.class);

    private static final String CERTS_PATH;
    private static final String TRUST_STORE_PASSWORD = "";

    private static final KeyStore TRUST_STORE;

    private static final SSLContext SSL_CONTEXT;

    static {
        CERTS_PATH = System.getenv("CERTS_PATH");

        try {
            TRUST_STORE = KeyStore.getInstance(KeyStore.getDefaultType());
            TRUST_STORE.load(null, TRUST_STORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Could not create a keystore", e);
        }

        if (CERTS_PATH == null) {
            logger.warn("CERTS_PATH environment variable not set - not loading any certificates");
        } else {
            try {
                Files.walk(Paths.get(CERTS_PATH))
                        .filter(Files::isRegularFile)
                        .forEach(TrustStoreLoader::loadCertificate);
            } catch (NoSuchFileException nsfe) {
                logger.warn("Did not find any certificates to load in {}", CERTS_PATH);
            } catch (IOException ioe) {
                logger.error("Error walking certs directory {}", CERTS_PATH, ioe);
            }
        }

        try {
            SSL_CONTEXT = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(TRUST_STORE);
            SSL_CONTEXT.init(null, tmf.getTrustManagers(), null);
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not initialize SSLContext", e);
        }
    }

    private static void loadCertificate(Path certificate) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate certData = cf.generateCertificate(new ByteArrayInputStream(Files.readAllBytes(certificate)));
            TRUST_STORE.setCertificateEntry(certificate.getFileName().toString(), certData);
            logger.info("Loaded cert {}", certificate);
        } catch (SecurityException | KeyStoreException | CertificateException | IOException e) {
            logger.error("Could not load {}", certificate, e);
        }
    }

    static SSLContext getSSLContext() {
        return SSL_CONTEXT;
    }
}

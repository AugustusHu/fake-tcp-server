package com.example.faketcp.tcp;

import com.example.faketcp.config.MockTlsProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

@Component
public class MockTlsServerSocketFactory {
    public SSLServerSocket create(MockTlsProperties properties, String host, int port, int backlog) throws Exception {
        SSLContext context = createContext(properties);
        SSLServerSocket serverSocket = (SSLServerSocket) context.getServerSocketFactory()
                .createServerSocket(port, backlog, InetAddress.getByName(host));
        if (properties != null && properties.getEnabledProtocols() != null && !properties.getEnabledProtocols().isEmpty()) {
            serverSocket.setEnabledProtocols(properties.getEnabledProtocols().toArray(new String[0]));
        }
        return serverSocket;
    }

    SSLContext createContext(MockTlsProperties properties) throws Exception {
        if (properties == null || isBlank(properties.getKeyStore())) {
            return createSelfSignedContext(properties);
        }

        KeyStore keyStore = KeyStore.getInstance(blankToDefault(properties.getKeyStoreType(), "PKCS12"));
        char[] keyStorePassword = password(properties.getKeyStorePassword());
        try (InputStream inputStream = ResourceUtils.getURL(properties.getKeyStore()).openStream()) {
            keyStore.load(inputStream, keyStorePassword);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password(isBlank(properties.getKeyPassword()) ? properties.getKeyStorePassword() : properties.getKeyPassword()));

        SSLContext context = SSLContext.getInstance(blankToDefault(properties.getProtocol(), "TLS"));
        context.init(keyManagerFactory.getKeyManagers(), null, null);
        return context;
    }

    private SSLContext createSelfSignedContext(MockTlsProperties properties) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        Certificate certificate = selfSignedCertificate(keyPair);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        char[] password = new char[0];
        keyStore.setKeyEntry("faker-mock", keyPair.getPrivate(), password, new Certificate[] { certificate });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        SSLContext context = SSLContext.getInstance(properties == null ? "TLS" : blankToDefault(properties.getProtocol(), "TLS"));
        context.init(keyManagerFactory.getKeyManagers(), null, null);
        return context;
    }

    private Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        AlgorithmIdentifier signatureAlgorithm = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption, DERNull.INSTANCE);
        X500Name subject = new X500Name("CN=Faker Mock TLS");
        Date now = new Date();
        Date notBefore = new Date(now.getTime() - 60_000L);
        Date notAfter = new Date(now.getTime() + 3650L * 24L * 60L * 60L * 1000L);

        V3TBSCertificateGenerator tbsGenerator = new V3TBSCertificateGenerator();
        tbsGenerator.setSerialNumber(new ASN1Integer(new BigInteger(160, new SecureRandom()).abs()));
        tbsGenerator.setSignature(signatureAlgorithm);
        tbsGenerator.setIssuer(subject);
        tbsGenerator.setStartDate(new Time(notBefore));
        tbsGenerator.setEndDate(new Time(notAfter));
        tbsGenerator.setSubject(subject);
        tbsGenerator.setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(keyPair.getPublic().getEncoded())));

        TBSCertificate tbsCertificate = tbsGenerator.generateTBSCertificate();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(tbsCertificate.getEncoded(ASN1Encoding.DER));

        ASN1EncodableVector certificateVector = new ASN1EncodableVector();
        certificateVector.add(tbsCertificate);
        certificateVector.add(signatureAlgorithm);
        certificateVector.add(new DERBitString(signature.sign()));

        byte[] encoded = new DERSequence(certificateVector).getEncoded(ASN1Encoding.DER);
        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));
    }

    private char[] password(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

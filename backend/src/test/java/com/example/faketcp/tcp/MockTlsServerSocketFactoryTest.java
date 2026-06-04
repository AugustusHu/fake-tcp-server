package com.example.faketcp.tcp;

import com.example.faketcp.config.MockTlsProperties;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockTlsServerSocketFactoryTest {
    @Test
    void createsServerSocketWithGeneratedSelfSignedCertificateWhenKeyStoreIsMissing() throws Exception {
        MockTlsServerSocketFactory factory = new MockTlsServerSocketFactory();

        SSLContext context = factory.createContext(new MockTlsProperties());

        assertThat(context.getServerSocketFactory()).isNotNull();
    }
}

package com.example.faketcp.config;

import java.util.ArrayList;
import java.util.List;

public class MockTlsProperties {
    private String keyStore;
    private String keyStoreType = "PKCS12";
    private String keyStorePassword;
    private String keyPassword;
    private String protocol = "TLS";
    private List<String> enabledProtocols = new ArrayList<>();

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public List<String> getEnabledProtocols() {
        return enabledProtocols;
    }

    public void setEnabledProtocols(List<String> enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }
}

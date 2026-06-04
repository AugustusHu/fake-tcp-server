package com.example.faketcp.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class TcpProperties {
    private String host = "0.0.0.0";
    @Min(14400)
    @Max(14700)
    private int port = 14400;
    private int readTimeoutMs = 60000;
    private boolean tlsEnabled = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
}

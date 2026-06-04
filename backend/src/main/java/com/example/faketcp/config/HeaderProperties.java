package com.example.faketcp.config;

public class HeaderProperties {
    private boolean enabled;
    private int length;
    private HeaderResponseMode responseMode = HeaderResponseMode.NONE;
    private String fixedValueHex;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public HeaderResponseMode getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(HeaderResponseMode responseMode) {
        this.responseMode = responseMode;
    }

    public String getFixedValueHex() {
        return fixedValueHex;
    }

    public void setFixedValueHex(String fixedValueHex) {
        this.fixedValueHex = fixedValueHex;
    }

    public enum HeaderResponseMode {
        NONE,
        ECHO,
        FIXED
    }
}

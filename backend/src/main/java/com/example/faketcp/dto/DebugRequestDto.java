package com.example.faketcp.dto;

public class DebugRequestDto {
    private String id;
    private String name;
    private String capability;
    private String requestXml;
    private String pin;
    private boolean saveTidConfig;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getRequestXml() {
        return requestXml;
    }

    public void setRequestXml(String requestXml) {
        this.requestXml = requestXml;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public boolean isSaveTidConfig() {
        return saveTidConfig;
    }

    public void setSaveTidConfig(boolean saveTidConfig) {
        this.saveTidConfig = saveTidConfig;
    }
}

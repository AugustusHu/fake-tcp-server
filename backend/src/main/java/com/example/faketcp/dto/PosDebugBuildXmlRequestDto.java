package com.example.faketcp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class PosDebugBuildXmlRequestDto {
    private String environmentId;
    private String capability;
    private String tid;
    private String sn;
    private Map<String, String> environmentFields = new LinkedHashMap<>();
    private Map<String, String> dynamicFields = new LinkedHashMap<>();
    private Map<String, String> fields = new LinkedHashMap<>();

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public Map<String, String> getEnvironmentFields() {
        return environmentFields;
    }

    public void setEnvironmentFields(Map<String, String> environmentFields) {
        this.environmentFields = environmentFields;
    }

    public Map<String, String> getDynamicFields() {
        return dynamicFields;
    }

    public void setDynamicFields(Map<String, String> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}

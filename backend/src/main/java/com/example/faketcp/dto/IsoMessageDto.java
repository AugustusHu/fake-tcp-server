package com.example.faketcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashMap;
import java.util.Map;

public class IsoMessageDto {
    private String mti;
    private Map<String, String> fields = new LinkedHashMap<>();
    private String bitmapMacFieldHint;

    public String getMti() {
        return mti;
    }

    public void setMti(String mti) {
        this.mti = mti;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    @JsonIgnore
    public String getBitmapMacFieldHint() {
        return bitmapMacFieldHint;
    }

    public void setBitmapMacFieldHint(String bitmapMacFieldHint) {
        this.bitmapMacFieldHint = bitmapMacFieldHint;
    }
}

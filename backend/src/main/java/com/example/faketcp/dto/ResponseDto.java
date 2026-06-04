package com.example.faketcp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseDto {
    private String mti;
    private Map<String, ResponseFieldDto> fields = new LinkedHashMap<>();

    public String getMti() {
        return mti;
    }

    public void setMti(String mti) {
        this.mti = mti;
    }

    public Map<String, ResponseFieldDto> getFields() {
        return fields;
    }

    public void setFields(Map<String, ResponseFieldDto> fields) {
        this.fields = fields;
    }
}

package com.example.faketcp.dto;

public class ResponseFieldDto {
    private ResponseValueType type = ResponseValueType.FIXED;
    private String value;
    private String sourceField;
    private String pattern;
    private Integer length;

    public ResponseValueType getType() {
        return type;
    }

    public void setType(ResponseValueType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public enum ResponseValueType {
        FIXED,
        COPY_REQUEST_FIELD,
        CURRENT_DATETIME,
        RANDOM_NUMERIC,
        RANDOM_ALPHANUMERIC
    }
}

package com.example.faketcp.dto;

public class PackagerFieldDto {
    private int id;
    private String type;
    private String valueType;
    private int length;
    private int maxPackedLength;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMaxPackedLength() {
        return maxPackedLength;
    }

    public void setMaxPackedLength(int maxPackedLength) {
        this.maxPackedLength = maxPackedLength;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

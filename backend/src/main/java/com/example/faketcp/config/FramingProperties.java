package com.example.faketcp.config;

public class FramingProperties {
    private FramingType type = FramingType.BINARY_2;
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private LengthIncludes lengthIncludes = LengthIncludes.PAYLOAD;

    public FramingType getType() {
        return type;
    }

    public void setType(FramingType type) {
        this.type = type;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public LengthIncludes getLengthIncludes() {
        return lengthIncludes;
    }

    public void setLengthIncludes(LengthIncludes lengthIncludes) {
        this.lengthIncludes = lengthIncludes;
    }

    public enum FramingType {
        BINARY_2,
        ASCII_4
    }

    public enum ByteOrder {
        BIG_ENDIAN,
        LITTLE_ENDIAN
    }

    public enum LengthIncludes {
        PAYLOAD,
        PAYLOAD_PLUS_LENGTH
    }
}

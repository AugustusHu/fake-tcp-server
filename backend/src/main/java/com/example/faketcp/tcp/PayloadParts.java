package com.example.faketcp.tcp;

public class PayloadParts {
    private final byte[] header;
    private final byte[] isoBody;

    public PayloadParts(byte[] header, byte[] isoBody) {
        this.header = header;
        this.isoBody = isoBody;
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getIsoBody() {
        return isoBody;
    }
}

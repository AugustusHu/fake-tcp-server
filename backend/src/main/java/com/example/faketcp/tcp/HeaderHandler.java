package com.example.faketcp.tcp;

import com.example.faketcp.config.HeaderProperties;
import com.example.faketcp.iso.HexUtils;
import java.util.Arrays;

public class HeaderHandler {
    private final HeaderProperties properties;

    public HeaderHandler(HeaderProperties properties) {
        this.properties = properties;
    }

    public PayloadParts split(byte[] payload) {
        if (!properties.isEnabled() || properties.getLength() <= 0) {
            return new PayloadParts(new byte[0], payload);
        }
        if (payload.length < properties.getLength()) {
            throw new IllegalArgumentException("Payload is shorter than configured header length");
        }
        return new PayloadParts(
                Arrays.copyOfRange(payload, 0, properties.getLength()),
                Arrays.copyOfRange(payload, properties.getLength(), payload.length));
    }

    public byte[] combine(byte[] requestHeader, byte[] isoBody) {
        byte[] responseHeader = responseHeader(requestHeader);
        byte[] payload = Arrays.copyOf(responseHeader, responseHeader.length + isoBody.length);
        System.arraycopy(isoBody, 0, payload, responseHeader.length, isoBody.length);
        return payload;
    }

    private byte[] responseHeader(byte[] requestHeader) {
        if (!properties.isEnabled()) {
            return new byte[0];
        }
        switch (properties.getResponseMode()) {
            case ECHO:
                return requestHeader == null ? new byte[0] : requestHeader;
            case FIXED:
                return HexUtils.fromHex(properties.getFixedValueHex());
            case NONE:
            default:
                return new byte[0];
        }
    }
}

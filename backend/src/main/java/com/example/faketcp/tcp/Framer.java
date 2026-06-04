package com.example.faketcp.tcp;

import com.example.faketcp.config.FramingProperties;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Framer {
    private final FramingProperties properties;

    public Framer(FramingProperties properties) {
        this.properties = properties;
    }

    public byte[] readFrame(InputStream inputStream) throws IOException {
        int lengthHeaderSize = lengthHeaderSize();
        byte[] lengthHeader = readExactly(inputStream, lengthHeaderSize);
        int length = decodeLength(lengthHeader);
        if (properties.getLengthIncludes() == FramingProperties.LengthIncludes.PAYLOAD_PLUS_LENGTH) {
            length -= lengthHeaderSize;
        }
        if (length < 0) {
            throw new IOException("Invalid frame length: " + length);
        }
        return readExactly(inputStream, length);
    }

    public byte[] writeFrame(byte[] payload) {
        int lengthHeaderSize = lengthHeaderSize();
        int length = payload.length;
        if (properties.getLengthIncludes() == FramingProperties.LengthIncludes.PAYLOAD_PLUS_LENGTH) {
            length += lengthHeaderSize;
        }
        byte[] header = encodeLength(length);
        byte[] framed = Arrays.copyOf(header, header.length + payload.length);
        System.arraycopy(payload, 0, framed, header.length, payload.length);
        return framed;
    }

    private int lengthHeaderSize() {
        return properties.getType() == FramingProperties.FramingType.ASCII_4 ? 4 : 2;
    }

    private int decodeLength(byte[] header) {
        if (properties.getType() == FramingProperties.FramingType.ASCII_4) {
            return Integer.parseInt(new String(header, StandardCharsets.US_ASCII));
        }
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(properties.getByteOrder() == FramingProperties.ByteOrder.LITTLE_ENDIAN
                ? ByteOrder.LITTLE_ENDIAN
                : ByteOrder.BIG_ENDIAN);
        return buffer.getShort() & 0xFFFF;
    }

    private byte[] encodeLength(int length) {
        if (properties.getType() == FramingProperties.FramingType.ASCII_4) {
            return String.format("%04d", length).getBytes(StandardCharsets.US_ASCII);
        }
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(properties.getByteOrder() == FramingProperties.ByteOrder.LITTLE_ENDIAN
                ? ByteOrder.LITTLE_ENDIAN
                : ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) length);
        return buffer.array();
    }

    private byte[] readExactly(InputStream inputStream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = inputStream.read(bytes, offset, length - offset);
            if (read < 0) {
                throw new EOFException("Connection closed while reading frame");
            }
            offset += read;
        }
        return bytes;
    }
}

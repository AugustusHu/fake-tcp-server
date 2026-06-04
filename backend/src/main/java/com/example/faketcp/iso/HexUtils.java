package com.example.faketcp.iso;

public final class HexUtils {
    private HexUtils() {
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null || hex.trim().isEmpty()) {
            return new byte[0];
        }
        String clean = hex.replaceAll("\\s+", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex value length must be even");
        }
        byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < clean.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(clean.substring(i, i + 2), 16);
        }
        return bytes;
    }
}

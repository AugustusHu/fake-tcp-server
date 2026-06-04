package com.example.faketcp.iso;

import java.util.Locale;
import org.jpos.iso.ISOFieldPackager;

public enum IsoFieldValueType {
    NUMERIC,
    TEXT,
    BINARY,
    UNKNOWN;

    public static IsoFieldValueType from(ISOFieldPackager fieldPackager) {
        if (fieldPackager == null) {
            return UNKNOWN;
        }
        String className = fieldPackager.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        if (className.contains("BINARY") || className.contains("BITMAP")) {
            return BINARY;
        }
        if (className.contains("NUM") || className.contains("AMOUNT")) {
            return NUMERIC;
        }
        if (className.contains("CHAR") || className.contains("STRING")) {
            return TEXT;
        }
        return UNKNOWN;
    }
}

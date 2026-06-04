package com.example.faketcp.iso;

import java.util.Locale;

public final class IsoFieldReferences {
    private IsoFieldReferences() {
    }

    public static String canonical(String field) {
        if (field == null) {
            return null;
        }
        String value = field.trim();
        if (value.isEmpty()) {
            return value;
        }
        if ("MTI".equalsIgnoreCase(value)) {
            return "0";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.startsWith("DE")) {
            String number = upper.substring(2).trim();
            if (isNumeric(number)) {
                return stripLeadingZeros(number);
            }
        }
        if (isNumeric(value)) {
            return stripLeadingZeros(value);
        }
        return value;
    }

    public static boolean isMti(String field) {
        String value = canonical(field);
        return "0".equals(value);
    }

    private static boolean isNumeric(String value) {
        return value != null && value.matches("\\d+");
    }

    private static String stripLeadingZeros(String value) {
        return value.replaceFirst("^0+(?!$)", "");
    }
}

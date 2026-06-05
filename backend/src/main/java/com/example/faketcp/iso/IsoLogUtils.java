package com.example.faketcp.iso;

import com.example.faketcp.dto.IsoMessageDto;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class IsoLogUtils {
    private static final int MAX_VALUE_LENGTH = 96;
    private static final Set<String> MASKED_FIELDS = new HashSet<>(Arrays.asList("2", "35", "52", "55"));

    private IsoLogUtils() {
    }

    public static String messageSummary(IsoMessageDto message) {
        if (message == null) {
            return "<null>";
        }
        return "mti=" + safe(message.getMti()) + ", fields=" + fieldsSummary(message.getFields());
    }

    public static String fieldsSummary(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "{}";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String field = IsoFieldReferences.canonical(entry.getKey());
            joiner.add("DE" + field + "=" + fieldValue(field, entry.getValue()));
        }
        return joiner.toString();
    }

    public static String fieldValue(String field, String value) {
        if (value == null) {
            return "<null>";
        }
        String canonicalField = IsoFieldReferences.canonical(field);
        if (MASKED_FIELDS.contains(canonicalField)) {
            return maskedSensitiveValue(canonicalField, value);
        }
        return truncate(value);
    }

    public static String keyFingerprint(String value) {
        if (isBlank(value)) {
            return "<blank>";
        }
        String compact = value.replaceAll("\\s+", "");
        int tailStart = Math.max(0, compact.length() - 6);
        return "len=" + compact.length() + ", tail=" + compact.substring(tailStart);
    }

    public static String presence(String value) {
        if (isBlank(value)) {
            return "present=false";
        }
        return "present=true, len=" + value.replaceAll("\\s+", "").length();
    }

    private static String maskedSensitiveValue(String field, String value) {
        String compact = value.replaceAll("\\s+", "");
        if ("2".equals(field)) {
            return maskPan(compact);
        }
        if ("35".equals(field)) {
            return maskTrack2(compact);
        }
        return "<present len=" + compact.length() + ">";
    }

    private static String maskPan(String value) {
        if (value.length() <= 10) {
            return "<present len=" + value.length() + ">";
        }
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }

    private static String maskTrack2(String value) {
        int separator = Math.max(value.indexOf('D'), value.indexOf('='));
        if (separator <= 0) {
            return "<present len=" + value.length() + ">";
        }
        return maskPan(value.substring(0, separator)) + value.charAt(separator) + "...";
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH) + "...(len=" + value.length() + ")";
    }

    private static String safe(String value) {
        return value == null ? "<null>" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

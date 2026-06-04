package com.example.faketcp.rule;

import com.example.faketcp.dto.ConditionDto;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.ResponseFieldDto;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.iso.IsoFieldReferences;
import com.example.faketcp.iso.IsoFieldValueType;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();

    public Optional<RuleMatchResult> match(List<RuleDto> rules, IsoMessageDto request) {
        return match(rules, request, Collections.emptyMap());
    }

    public Optional<RuleMatchResult> match(List<RuleDto> rules, IsoMessageDto request, Map<String, IsoFieldValueType> fieldValueTypes) {
        for (RuleDto rule : rules) {
            if (matches(rule, request, fieldValueTypes == null ? Collections.emptyMap() : fieldValueTypes)) {
                return Optional.of(new RuleMatchResult(rule, buildResponse(rule, request)));
            }
        }
        return Optional.empty();
    }

    public IsoMessageDto buildResponse(RuleDto rule, IsoMessageDto request) {
        IsoMessageDto response = new IsoMessageDto();
        response.setMti(resolveMti(rule.getResponse().getMti(), request.getMti()));
        response.setBitmapMacFieldHint(request.getBitmapMacFieldHint());
        response.getFields().putAll(request.getFields());
        for (Map.Entry<String, ResponseFieldDto> entry : rule.getResponse().getFields().entrySet()) {
            String field = IsoFieldReferences.canonical(entry.getKey());
            if (!IsoFieldReferences.isMti(field)) {
                response.getFields().put(field, resolveField(entry.getValue(), request));
            }
        }
        return response;
    }

    public IsoMessageDto defaultResponse(IsoMessageDto request, String responseCode) {
        IsoMessageDto response = new IsoMessageDto();
        response.setMti(resolveMti(null, request.getMti()));
        response.setBitmapMacFieldHint(request.getBitmapMacFieldHint());
        response.getFields().putAll(request.getFields());
        response.getFields().put("39", responseCode);
        return response;
    }

    private boolean matches(RuleDto rule, IsoMessageDto request, Map<String, IsoFieldValueType> fieldValueTypes) {
        if (!matchesSystemConditions(rule, request, fieldValueTypes)) {
            return false;
        }
        List<Boolean> results = rule.getConditions().stream()
                .map(condition -> matches(condition, request, fieldValueTypes))
                .collect(Collectors.toList());
        if (rule.getMatchMode() == RuleDto.MatchMode.ANY) {
            return results.stream().anyMatch(Boolean::booleanValue);
        }
        return results.stream().allMatch(Boolean::booleanValue);
    }

    private boolean matchesSystemConditions(RuleDto rule, IsoMessageDto request, Map<String, IsoFieldValueType> fieldValueTypes) {
        if (rule.getSystemConditions() == null || rule.getSystemConditions().isEmpty()) {
            return true;
        }
        return rule.getSystemConditions().stream().allMatch(condition -> matches(condition, request, fieldValueTypes));
    }

    private boolean matches(ConditionDto condition, IsoMessageDto request, Map<String, IsoFieldValueType> fieldValueTypes) {
        String actual = fieldValue(condition.getField(), request);
        String expected = condition.getValue();
        IsoFieldValueType valueType = fieldValueType(condition.getField(), fieldValueTypes);
        ConditionDto.Operator operator = condition.getOperator() == null ? ConditionDto.Operator.EQ : condition.getOperator();
        switch (operator) {
            case EXISTS:
                return actual != null;
            case NOT_EXISTS:
                return actual == null;
            case EQ:
                return equalsByType(actual, expected, valueType);
            case NE:
                return !equalsByType(actual, expected, valueType);
            case CONTAINS:
                return actual != null && expected != null && actual.contains(expected);
            case NOT_CONTAINS:
                return actual == null || expected == null || !actual.contains(expected);
            case GT:
                Integer gtComparison = compareNumeric(actual, expected, valueType);
                return gtComparison != null && gtComparison > 0;
            case GTE:
                Integer gteComparison = compareNumeric(actual, expected, valueType);
                return gteComparison != null && gteComparison >= 0;
            case LT:
                Integer ltComparison = compareNumeric(actual, expected, valueType);
                return ltComparison != null && ltComparison < 0;
            case LTE:
                Integer lteComparison = compareNumeric(actual, expected, valueType);
                return lteComparison != null && lteComparison <= 0;
            case REGEX:
                return actual != null && expected != null && Pattern.compile(expected).matcher(actual).find();
            case IN:
                return matchesAny(actual, expected, valueType);
            default:
                return false;
        }
    }

    private String fieldValue(String field, IsoMessageDto request) {
        if (field == null) {
            return null;
        }
        String canonicalField = IsoFieldReferences.canonical(field);
        if (IsoFieldReferences.isMti(canonicalField)) {
            return request.getMti();
        }
        return request.getFields().get(canonicalField);
    }

    private IsoFieldValueType fieldValueType(String field, Map<String, IsoFieldValueType> fieldValueTypes) {
        String canonicalField = IsoFieldReferences.canonical(field);
        if (IsoFieldReferences.isMti(canonicalField)) {
            return fieldValueTypes.getOrDefault(canonicalField, IsoFieldValueType.NUMERIC);
        }
        return fieldValueTypes.getOrDefault(canonicalField, IsoFieldValueType.UNKNOWN);
    }

    private boolean equalsByType(String actual, String expected, IsoFieldValueType valueType) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        if (isNumericComparable(valueType)) {
            BigDecimal actualNumber = numericValue(actual);
            BigDecimal expectedNumber = numericValue(expected);
            if (actualNumber != null && expectedNumber != null) {
                return actualNumber.compareTo(expectedNumber) == 0;
            }
        }
        return actual.equals(expected);
    }

    private boolean matchesAny(String actual, String expected, IsoFieldValueType valueType) {
        if (actual == null || expected == null) {
            return false;
        }
        return Arrays.stream(expected.split(","))
                .map(String::trim)
                .anyMatch(item -> equalsByType(actual, item, valueType));
    }

    private Integer compareNumeric(String actual, String expected, IsoFieldValueType valueType) {
        if (!isNumericComparable(valueType)) {
            return null;
        }
        BigDecimal actualNumber = numericValue(actual);
        BigDecimal expectedNumber = numericValue(expected);
        if (actualNumber == null || expectedNumber == null) {
            return null;
        }
        return actualNumber.compareTo(expectedNumber);
    }

    private boolean isNumericComparable(IsoFieldValueType valueType) {
        return valueType == IsoFieldValueType.NUMERIC || valueType == IsoFieldValueType.UNKNOWN;
    }

    private BigDecimal numericValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(",", "");
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 1 && (normalized.charAt(0) == 'C' || normalized.charAt(0) == 'c')) {
            normalized = normalized.substring(1);
        } else if (normalized.length() > 1 && (normalized.charAt(0) == 'D' || normalized.charAt(0) == 'd')) {
            normalized = "-" + normalized.substring(1);
        }
        if (!normalized.matches("[+-]?\\d+(\\.\\d+)?")) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    private String resolveMti(String configuredMti, String requestMti) {
        if (configuredMti != null && !configuredMti.trim().isEmpty() && !"AUTO".equalsIgnoreCase(configuredMti)) {
            return configuredMti;
        }
        if (requestMti == null || requestMti.length() != 4) {
            return requestMti;
        }
        char[] chars = requestMti.toCharArray();
        chars[2] = (char) (chars[2] + 1);
        return new String(chars);
    }

    private String resolveField(ResponseFieldDto field, IsoMessageDto request) {
        switch (field.getType()) {
            case COPY_REQUEST_FIELD:
                String sourceField = IsoFieldReferences.canonical(field.getSourceField());
                if (IsoFieldReferences.isMti(sourceField)) {
                    return request.getMti() == null ? "" : request.getMti();
                }
                return request.getFields().getOrDefault(sourceField, "");
            case CURRENT_DATETIME:
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                        field.getPattern() == null || field.getPattern().trim().isEmpty() ? "MMddHHmmss" : field.getPattern()));
            case RANDOM_NUMERIC:
                return randomString("0123456789", field.getLength() == null ? 6 : field.getLength());
            case RANDOM_ALPHANUMERIC:
                return randomString(ALPHANUMERIC, field.getLength() == null ? 6 : field.getLength());
            case FIXED:
            default:
                return field.getValue();
        }
    }

    private String randomString(String alphabet, int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString().toUpperCase(Locale.ROOT);
    }
}

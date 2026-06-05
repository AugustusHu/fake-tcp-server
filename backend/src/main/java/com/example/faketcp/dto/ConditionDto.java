package com.example.faketcp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ConditionDto {
    private String field;
    private Operator operator = Operator.EQ;
    private String value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public enum Operator {
        EQ,
        NE,
        EXISTS,
        NOT_EXISTS,
        CONTAINS,
        NOT_CONTAINS,
        GT,
        GTE,
        LT,
        LTE,
        REGEX,
        IN;

        @JsonCreator
        public static Operator fromJson(String value) {
            if (value == null || value.trim().isEmpty()) {
                return EQ;
            }
            String normalized = value.trim().toUpperCase();
            if (normalized.contains(" ")) {
                normalized = normalized.substring(0, normalized.indexOf(' '));
            }
            switch (normalized) {
                case "=":
                case "==":
                    return EQ;
                case "!=":
                case "<>":
                    return NE;
                case ">":
                    return GT;
                case ">=":
                    return GTE;
                case "<":
                    return LT;
                case "<=":
                    return LTE;
                default:
                    return Operator.valueOf(normalized);
            }
        }

        @JsonValue
        public String toJson() {
            return name();
        }
    }
}

package com.example.faketcp.dto;

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
        IN
    }
}

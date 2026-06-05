package com.example.faketcp.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ConditionDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsDisplayOperatorWhenDeserializing() throws Exception {
        ConditionDto condition = objectMapper.readValue(
                "{\"field\":\"4\",\"operator\":\"< 小于\",\"value\":\"000000000200\"}",
                ConditionDto.class);

        assertThat(condition.getOperator()).isEqualTo(ConditionDto.Operator.LT);
    }

    @Test
    void serializesCanonicalOperatorName() throws Exception {
        ConditionDto condition = new ConditionDto();
        condition.setField("4");
        condition.setOperator(ConditionDto.Operator.LT);
        condition.setValue("000000000200");

        assertThat(objectMapper.writeValueAsString(condition)).contains("\"operator\":\"LT\"");
    }
}

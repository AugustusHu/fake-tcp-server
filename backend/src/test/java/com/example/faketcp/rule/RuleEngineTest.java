package com.example.faketcp.rule;

import com.example.faketcp.dto.ConditionDto;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.ResponseDto;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.iso.IsoFieldValueType;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {
    private final RuleEngine ruleEngine = new RuleEngine();

    @Test
    void numericGreaterThanUsesFieldValueType() {
        RuleDto rule = rule(condition("4", ConditionDto.Operator.GT, "10000"));
        IsoMessageDto request = request("4", "000020000");

        assertThat(ruleEngine.match(Collections.singletonList(rule), request, Collections.singletonMap("4", IsoFieldValueType.NUMERIC)))
                .isPresent();
    }

    @Test
    void numericEqualityIgnoresLeadingZeros() {
        RuleDto rule = rule(condition("4", ConditionDto.Operator.EQ, "20000"));
        IsoMessageDto request = request("4", "000020000");

        assertThat(ruleEngine.match(Collections.singletonList(rule), request, Collections.singletonMap("4", IsoFieldValueType.NUMERIC)))
                .isPresent();
    }

    @Test
    void textFieldDoesNotUseMagnitudeComparison() {
        RuleDto rule = rule(condition("41", ConditionDto.Operator.GT, "100"));
        IsoMessageDto request = request("41", "200");

        assertThat(ruleEngine.match(Collections.singletonList(rule), request, Collections.singletonMap("41", IsoFieldValueType.TEXT)))
                .isNotPresent();
    }

    @Test
    void missingFieldDoesNotMatchLessThan() {
        RuleDto rule = rule(condition("4", ConditionDto.Operator.LT, "100"));
        IsoMessageDto request = request("11", "123456");

        assertThat(ruleEngine.match(Collections.singletonList(rule), request, Collections.singletonMap("4", IsoFieldValueType.NUMERIC)))
                .isNotPresent();
    }

    @Test
    void numericFieldFallsBackToStringEqualityWhenValueIsNotNumeric() {
        RuleDto rule = rule(condition("3", ConditionDto.Operator.EQ, "9A0000"));
        IsoMessageDto request = request("3", "9A0000");

        assertThat(ruleEngine.match(Collections.singletonList(rule), request, Collections.singletonMap("3", IsoFieldValueType.NUMERIC)))
                .isPresent();
    }

    private RuleDto rule(ConditionDto condition) {
        RuleDto rule = new RuleDto();
        rule.setEnabled(true);
        rule.setMatchMode(RuleDto.MatchMode.ALL);
        rule.setConditions(Collections.singletonList(condition));
        ResponseDto response = new ResponseDto();
        response.setMti("0210");
        rule.setResponse(response);
        return rule;
    }

    private ConditionDto condition(String field, ConditionDto.Operator operator, String value) {
        ConditionDto condition = new ConditionDto();
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }

    private IsoMessageDto request(String field, String value) {
        IsoMessageDto request = new IsoMessageDto();
        request.setMti("0200");
        request.getFields().put(field, value);
        return request;
    }
}

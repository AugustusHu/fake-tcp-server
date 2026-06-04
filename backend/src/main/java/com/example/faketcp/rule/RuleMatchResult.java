package com.example.faketcp.rule;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.RuleDto;

public class RuleMatchResult {
    private final RuleDto rule;
    private final IsoMessageDto response;

    public RuleMatchResult(RuleDto rule, IsoMessageDto response) {
        this.rule = rule;
        this.response = response;
    }

    public RuleDto getRule() {
        return rule;
    }

    public IsoMessageDto getResponse() {
        return response;
    }
}

package com.example.faketcp.dto;

public class TestMatchResponse {
    private boolean matched;
    private RuleDto rule;
    private ActionDto action;
    private IsoMessageDto response;

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public RuleDto getRule() {
        return rule;
    }

    public void setRule(RuleDto rule) {
        this.rule = rule;
    }

    public ActionDto getAction() {
        return action;
    }

    public void setAction(ActionDto action) {
        this.action = action;
    }

    public IsoMessageDto getResponse() {
        return response;
    }

    public void setResponse(IsoMessageDto response) {
        this.response = response;
    }
}

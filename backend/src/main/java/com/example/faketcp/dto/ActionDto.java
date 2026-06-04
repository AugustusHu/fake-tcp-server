package com.example.faketcp.dto;

public class ActionDto {
    private ActionType type = ActionType.RESPOND;
    private long delayMs;

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public enum ActionType {
        RESPOND,
        DELAY_RESPOND,
        TIMEOUT,
        DISCONNECT
    }
}

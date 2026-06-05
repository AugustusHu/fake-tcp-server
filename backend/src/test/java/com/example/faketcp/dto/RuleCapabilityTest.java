package com.example.faketcp.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleCapabilityTest {
    @Test
    void reversalUsesAdviceMtiDefaults() {
        assertThat(RuleCapability.REVERSAL.getRequestMti()).isEqualTo("0420");
        assertThat(RuleCapability.REVERSAL.getProcessCode()).isEqualTo("000000");
        assertThat(RuleCapability.REVERSAL.getResponseMti()).isEqualTo("0421");
    }
}

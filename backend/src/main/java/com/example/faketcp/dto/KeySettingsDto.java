package com.example.faketcp.dto;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeySettingsDto {
    private String channelId;
    private String tpkPlain;
    private String tskPlain;
    private String macField = "128";
    private String macAlgorithm = "ANSI_X9_19";
    private String testTid;
    private String testPan;
    private String testPin;
    private String testDe14;
    private String testDe42;
    private String testDe18;
    private String testDe43;
    private String testDe49;
    private Instant updatedAt;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTpkPlain() {
        return tpkPlain;
    }

    public void setTpkPlain(String tpkPlain) {
        this.tpkPlain = tpkPlain;
    }

    public String getTskPlain() {
        return tskPlain;
    }

    public void setTskPlain(String tskPlain) {
        this.tskPlain = tskPlain;
    }

    public String getMacField() {
        return macField;
    }

    public void setMacField(String macField) {
        this.macField = macField;
    }

    public String getMacAlgorithm() {
        return macAlgorithm;
    }

    public void setMacAlgorithm(String macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
    }

    public String getTestTid() {
        return testTid;
    }

    public void setTestTid(String testTid) {
        this.testTid = testTid;
    }

    public String getTestPan() {
        return testPan;
    }

    public void setTestPan(String testPan) {
        this.testPan = testPan;
    }

    public String getTestPin() {
        return testPin;
    }

    public void setTestPin(String testPin) {
        this.testPin = testPin;
    }

    public String getTestDe14() {
        return testDe14;
    }

    public void setTestDe14(String testDe14) {
        this.testDe14 = testDe14;
    }

    public String getTestDe42() {
        return testDe42;
    }

    public void setTestDe42(String testDe42) {
        this.testDe42 = testDe42;
    }

    public String getTestDe18() {
        return testDe18;
    }

    public void setTestDe18(String testDe18) {
        this.testDe18 = testDe18;
    }

    public String getTestDe43() {
        return testDe43;
    }

    public void setTestDe43(String testDe43) {
        this.testDe43 = testDe43;
    }

    public String getTestDe49() {
        return testDe49;
    }

    public void setTestDe49(String testDe49) {
        this.testDe49 = testDe49;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isRestartRequired() {
        return false;
    }

    public List<String> getRestartRequiredFields() {
        return Collections.emptyList();
    }

    public List<String> getHotEffectiveFields() {
        return Arrays.asList(
                "tpkPlain",
                "tskPlain",
                "macField",
                "macAlgorithm",
                "testTid",
                "testPan",
                "testPin",
                "testDe14",
                "testDe42",
                "testDe18",
                "testDe43",
                "testDe49");
    }
}

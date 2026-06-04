package com.example.faketcp.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DebugEnvironmentDto {
    private String id;
    private Long userId;
    private String channelId;
    private String name;
    private String targetIp;
    private Integer targetPort;
    private String tmkPlain;
    private String tpkPlain;
    private String tskPlain;
    private String macField;
    private String macAlgorithm;
    private String pinAlgorithm;
    private String testTid;
    private String testSn;
    private String testPan;
    private String testDe14;
    private String testDe52;
    private String testDe42;
    private String testDe18;
    private String testDe43;
    private String testDe49;
    private List<DebugEnvironmentVariableDto> variables = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTargetIp() { return targetIp; }
    public void setTargetIp(String targetIp) { this.targetIp = targetIp; }
    public Integer getTargetPort() { return targetPort; }
    public void setTargetPort(Integer targetPort) { this.targetPort = targetPort; }
    public String getTmkPlain() { return tmkPlain; }
    public void setTmkPlain(String tmkPlain) { this.tmkPlain = tmkPlain; }
    public String getTpkPlain() { return tpkPlain; }
    public void setTpkPlain(String tpkPlain) { this.tpkPlain = tpkPlain; }
    public String getTskPlain() { return tskPlain; }
    public void setTskPlain(String tskPlain) { this.tskPlain = tskPlain; }
    public String getMacField() { return macField; }
    public void setMacField(String macField) { this.macField = macField; }
    public String getMacAlgorithm() { return macAlgorithm; }
    public void setMacAlgorithm(String macAlgorithm) { this.macAlgorithm = macAlgorithm; }
    public String getPinAlgorithm() { return pinAlgorithm; }
    public void setPinAlgorithm(String pinAlgorithm) { this.pinAlgorithm = pinAlgorithm; }
    public String getTestTid() { return testTid; }
    public void setTestTid(String testTid) { this.testTid = testTid; }
    public String getTestSn() { return testSn; }
    public void setTestSn(String testSn) { this.testSn = testSn; }
    public String getTestPan() { return testPan; }
    public void setTestPan(String testPan) { this.testPan = testPan; }
    public String getTestDe14() { return testDe14; }
    public void setTestDe14(String testDe14) { this.testDe14 = testDe14; }
    public String getTestDe52() { return testDe52; }
    public void setTestDe52(String testDe52) { this.testDe52 = testDe52; }
    public String getTestDe42() { return testDe42; }
    public void setTestDe42(String testDe42) { this.testDe42 = testDe42; }
    public String getTestDe18() { return testDe18; }
    public void setTestDe18(String testDe18) { this.testDe18 = testDe18; }
    public String getTestDe43() { return testDe43; }
    public void setTestDe43(String testDe43) { this.testDe43 = testDe43; }
    public String getTestDe49() { return testDe49; }
    public void setTestDe49(String testDe49) { this.testDe49 = testDe49; }
    public List<DebugEnvironmentVariableDto> getVariables() { return variables; }
    public void setVariables(List<DebugEnvironmentVariableDto> variables) { this.variables = variables == null ? new ArrayList<>() : variables; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

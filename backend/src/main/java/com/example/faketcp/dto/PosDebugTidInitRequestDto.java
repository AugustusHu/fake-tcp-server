package com.example.faketcp.dto;

public class PosDebugTidInitRequestDto {
    private String environmentId;
    private String tid;
    private String sn;
    private boolean saveKey;
    private String targetIp;
    private Integer targetPort;

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public boolean isSaveKey() {
        return saveKey;
    }

    public void setSaveKey(boolean saveKey) {
        this.saveKey = saveKey;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }
}

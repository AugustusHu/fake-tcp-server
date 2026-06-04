package com.example.faketcp.dto;

import java.util.ArrayList;
import java.util.List;

public class PosDebugResponseDto {
    private String channelId;
    private String channelCode;
    private String targetIp;
    private Integer targetPort;
    private String packager;
    private boolean success;
    private String report;
    private List<PosDebugStepDto> steps = new ArrayList<>();

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
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

    public String getPackager() {
        return packager;
    }

    public void setPackager(String packager) {
        this.packager = packager;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public List<PosDebugStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<PosDebugStepDto> steps) {
        this.steps = steps;
    }
}

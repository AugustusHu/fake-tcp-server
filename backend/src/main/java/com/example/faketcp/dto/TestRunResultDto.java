package com.example.faketcp.dto;

public class TestRunResultDto {
    private Long caseId;
    private String name;
    private boolean success;
    private String responseXml;
    private String error;

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getResponseXml() { return responseXml; }
    public void setResponseXml(String responseXml) { this.responseXml = responseXml; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

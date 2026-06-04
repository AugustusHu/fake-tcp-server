package com.example.faketcp.dto;

public enum RuleCapability {
    DEBIT("扣款", "0200", "000000", "0210"),
    BALANCE_QUERY("余额查询", "0100", "310000", "0110"),
    PRE_AUTH("预授权", "0100", "600000", "0110"),
    PRE_AUTH_COMPLETION("预授权完成", "0220", "610000", "0230"),
    REVERSAL("冲正", "0400", "000000", "0410"),
    REFUND("退款", "0200", "200000", "0210"),
    TID_INIT_9A("TID-TMK", "0800", "9A0000", "0810"),
    TID_INIT_9G("TID-TPK", "0800", "9G0000", "0810"),
    TID_INIT_9B("TID-TSK", "0800", "9B0000", "0810"),
    PARAMETER_DOWNLOAD("参数下载", "0800", "9C0000", "0810"),
    CALLHOME("Callhome", "0800", "9D0000", "0810");

    private final String label;
    private final String requestMti;
    private final String processCode;
    private final String responseMti;

    RuleCapability(String label, String requestMti, String processCode, String responseMti) {
        this.label = label;
        this.requestMti = requestMti;
        this.processCode = processCode;
        this.responseMti = responseMti;
    }

    public String getLabel() {
        return label;
    }

    public String getRequestMti() {
        return requestMti;
    }

    public String getProcessCode() {
        return processCode;
    }

    public String getResponseMti() {
        return responseMti;
    }
}

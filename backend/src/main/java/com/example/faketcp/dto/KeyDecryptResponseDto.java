package com.example.faketcp.dto;

public class KeyDecryptResponseDto {
    private String ctmkPlain;
    private String tmkPlain;
    private String tpkPlain;
    private String tskPlain;
    private String ctmkSource;
    private String algorithm;

    public String getCtmkPlain() {
        return ctmkPlain;
    }

    public void setCtmkPlain(String ctmkPlain) {
        this.ctmkPlain = ctmkPlain;
    }

    public String getTmkPlain() {
        return tmkPlain;
    }

    public void setTmkPlain(String tmkPlain) {
        this.tmkPlain = tmkPlain;
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

    public String getCtmkSource() {
        return ctmkSource;
    }

    public void setCtmkSource(String ctmkSource) {
        this.ctmkSource = ctmkSource;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}

package com.example.faketcp.dto;

public class KeyDecryptRequestDto {
    private String ctmk1;
    private String ctmk2;
    private String encryptedTmk;
    private String encryptedTpk;
    private String encryptedTsk;

    public String getCtmk1() {
        return ctmk1;
    }

    public void setCtmk1(String ctmk1) {
        this.ctmk1 = ctmk1;
    }

    public String getCtmk2() {
        return ctmk2;
    }

    public void setCtmk2(String ctmk2) {
        this.ctmk2 = ctmk2;
    }

    public String getEncryptedTmk() {
        return encryptedTmk;
    }

    public void setEncryptedTmk(String encryptedTmk) {
        this.encryptedTmk = encryptedTmk;
    }

    public String getEncryptedTpk() {
        return encryptedTpk;
    }

    public void setEncryptedTpk(String encryptedTpk) {
        this.encryptedTpk = encryptedTpk;
    }

    public String getEncryptedTsk() {
        return encryptedTsk;
    }

    public void setEncryptedTsk(String encryptedTsk) {
        this.encryptedTsk = encryptedTsk;
    }
}

package com.example.faketcp.config;

import javax.validation.Valid;

public class Iso8583Properties {
    @Valid
    private PackagerProperties packager = new PackagerProperties();

    public PackagerProperties getPackager() {
        return packager;
    }

    public void setPackager(PackagerProperties packager) {
        this.packager = packager;
    }
}

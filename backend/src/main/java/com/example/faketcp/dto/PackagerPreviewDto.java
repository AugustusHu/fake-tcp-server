package com.example.faketcp.dto;

import java.util.ArrayList;
import java.util.List;

public class PackagerPreviewDto {
    private String channelId;
    private String channelName;
    private String packagerClass;
    private String description;
    private List<PackagerFieldDto> fields = new ArrayList<>();

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getPackagerClass() {
        return packagerClass;
    }

    public void setPackagerClass(String packagerClass) {
        this.packagerClass = packagerClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PackagerFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<PackagerFieldDto> fields) {
        this.fields = fields;
    }
}

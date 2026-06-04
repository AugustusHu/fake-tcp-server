package com.example.faketcp.config;

public class PackagerProperties {
    private PackagerType type = PackagerType.CLASS;
    private PackagerConfigMode configMode = PackagerConfigMode.CLASS_NAME;
    private String location;
    private String fileName;
    private String content;
    private String className = "org.jpos.iso.packager.ISO87APackager";

    public PackagerType getType() {
        return type;
    }

    public void setType(PackagerType type) {
        this.type = type;
    }

    public PackagerConfigMode getConfigMode() {
        return configMode;
    }

    public void setConfigMode(PackagerConfigMode configMode) {
        this.configMode = configMode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public enum PackagerType {
        XML,
        CLASS,
        CUSTOM
    }

    public enum PackagerConfigMode {
        XML_CONTENT,
        XML_FILE,
        CLASS_NAME,
        JAVA_SOURCE
    }
}

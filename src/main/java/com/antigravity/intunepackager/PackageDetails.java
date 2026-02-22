package com.antigravity.intunepackager;

public class PackageDetails {

    private String sourcePath;
    private String sourceFileName;
    private String sourceType;

    private String appName;
    private String publisher;
    private String version;
    private String description;

    private String installCmd;
    private String uninstallCmd;
    private String detectionRule;
    private String detectionScript;
    private String preInstallScript;

    // Getters and Setters

    public String getPreInstallScript() {
        return preInstallScript;
    }

    public void setPreInstallScript(String preInstallScript) {
        this.preInstallScript = preInstallScript;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstallCmd() {
        return installCmd;
    }

    public void setInstallCmd(String installCmd) {
        this.installCmd = installCmd;
    }

    public String getUninstallCmd() {
        return uninstallCmd;
    }

    public void setUninstallCmd(String uninstallCmd) {
        this.uninstallCmd = uninstallCmd;
    }

    public String getDetectionRule() {
        return detectionRule;
    }

    public void setDetectionRule(String detectionRule) {
        this.detectionRule = detectionRule;
    }

    public String getDetectionScript() {
        return detectionScript;
    }

    public void setDetectionScript(String detectionScript) {
        this.detectionScript = detectionScript;
    }
}

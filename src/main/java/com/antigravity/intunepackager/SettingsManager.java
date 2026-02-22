package com.antigravity.intunepackager;

import java.util.prefs.Preferences;

public class SettingsManager {

    // Preference Keys
    private static final String PREF_TENANT_ID = "tenantId";
    private static final String PREF_CLIENT_ID = "clientId";
    private static final String PREF_TOOL_PATH = "toolPath";

    private final Preferences prefs;

    public SettingsManager() {
        // Creates a node in the backing store (e.g., Windows Registry) specific to this
        // class
        this.prefs = Preferences.userNodeForPackage(SettingsManager.class);
    }

    public boolean isSetupComplete() {
        return getTenantId() != null && !getTenantId().isEmpty() &&
                getClientId() != null && !getClientId().isEmpty() &&
                getToolPath() != null && !getToolPath().isEmpty();
    }

    public void saveSettings(String tenantId, String clientId, String toolPath) {
        prefs.put(PREF_TENANT_ID, tenantId);
        prefs.put(PREF_CLIENT_ID, clientId);
        prefs.put(PREF_TOOL_PATH, toolPath);
    }

    public String getTenantId() {
        return prefs.get(PREF_TENANT_ID, null);
    }

    public String getClientId() {
        return prefs.get(PREF_CLIENT_ID, null);
    }

    public String getToolPath() {
        return prefs.get(PREF_TOOL_PATH, null);
    }
}

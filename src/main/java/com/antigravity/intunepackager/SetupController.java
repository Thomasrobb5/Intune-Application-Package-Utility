package com.antigravity.intunepackager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

public class SetupController {

    @FXML
    private TextField tenantIdField;
    @FXML
    private TextField clientIdField;
    @FXML
    private TextField toolPathField;
    @FXML
    private Label errorLabel;

    private SettingsManager settingsManager;

    @FXML
    public void initialize() {
        settingsManager = new SettingsManager();

        // Pre-fill if values already exist (useful when editing settings from
        // dashboard)
        if (settingsManager.getTenantId() != null) {
            tenantIdField.setText(settingsManager.getTenantId());
        }
        if (settingsManager.getClientId() != null) {
            clientIdField.setText(settingsManager.getClientId());
        }
        if (settingsManager.getToolPath() != null) {
            toolPathField.setText(settingsManager.getToolPath());
        }
    }

    @FXML
    private void handleBrowseTool() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select IntuneWinAppUtil.exe");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable Files", "*.exe"));

        File file = fileChooser.showOpenDialog(toolPathField.getScene().getWindow());
        if (file != null) {
            toolPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleDownloadTool() {
        MainApp.getAppHostServices().showDocument("https://go.microsoft.com/fwlink/?linkid=2065730");
    }

    @FXML
    private void handleSave() {
        String tenantId = tenantIdField.getText().trim();
        String clientId = clientIdField.getText().trim();
        String toolPath = toolPathField.getText().trim();

        if (tenantId.isEmpty() || clientId.isEmpty() || toolPath.isEmpty()) {
            errorLabel.setText("Please fill out all required fields.");
            return;
        }

        File toolFile = new File(toolPath);
        if (!toolFile.exists() || !toolFile.getName().toLowerCase().endsWith(".exe")) {
            errorLabel.setText("Invalid IntuneWinAppUtil.exe path.");
            return;
        }

        // Save to Preferences
        settingsManager.saveSettings(tenantId, clientId, toolPath);

        // Switch back to Dashboard
        try {
            MainApp.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load dashboard.");
        }
    }
}

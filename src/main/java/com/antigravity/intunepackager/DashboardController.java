package com.antigravity.intunepackager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class DashboardController {

    // --- Sidebar Indicators ---
    @FXML
    private HBox step1Indicator;
    @FXML
    private HBox step2Indicator;
    @FXML
    private HBox step3Indicator;
    @FXML
    private HBox step4Indicator;

    // --- Wizard Content Boxes ---
    @FXML
    private StackPane contentStack;
    @FXML
    private VBox step1Box;
    @FXML
    private VBox step2Box;
    @FXML
    private VBox step3Box;
    @FXML
    private VBox step4Box;

    // --- Form Fields ---
    @FXML
    private Label selectedFileLabel;
    private File selectedSourceFile;
    @FXML
    private TextField appNameField;
    @FXML
    private TextField publisherField;
    @FXML
    private TextField versionField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField installCmdField;
    @FXML
    private TextField uninstallCmdField;
    @FXML
    private TextField detectionRuleField;
    @FXML
    private TextArea preInstallScriptArea;
    @FXML
    private TextField outputFolderField;

    // --- Progress and Logs ---
    @FXML
    private VBox generationProgressBox;
    @FXML
    private ProgressBar packageProgressBar;
    @FXML
    private Label packageStatusLabel;
    @FXML
    private TextArea logArea;

    // --- Action Buttons ---
    @FXML
    private Button backButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button generateButton;
    @FXML
    private Button uploadButton;

    private int currentStep = 1;
    private SettingsManager settingsManager;

    @FXML
    public void initialize() {
        settingsManager = new SettingsManager();
        showStep(1);
        logMessage("Intune Application Packager Wizard initialized.");
    }

    // ==========================================
    // Navigation Logic
    // ==========================================

    @FXML
    public void handleNext(ActionEvent event) {
        if (currentStep == 1) {
            if (selectedSourceFile == null) {
                // Should show an alert, but keeping it simple for now
                return;
            }
        } else if (currentStep == 2) {
            if (appNameField.getText().isEmpty() || installCmdField.getText().isEmpty()) {
                return;
            }
        }

        if (currentStep < 4) {
            currentStep++;
            showStep(currentStep);
        }
    }

    @FXML
    public void handleBack(ActionEvent event) {
        if (currentStep > 1) {
            currentStep--;
            showStep(currentStep);
        }
    }

    private void showStep(int step) {
        // Hide all boxes
        step1Box.setVisible(false);
        step2Box.setVisible(false);
        step3Box.setVisible(false);
        step4Box.setVisible(false);

        // Reset Indicators
        setIndicatorState(step1Indicator, step > 1 ? "completed" : "inactive");
        setIndicatorState(step2Indicator, step > 2 ? "completed" : "inactive");
        setIndicatorState(step3Indicator, step > 3 ? "completed" : "inactive");
        setIndicatorState(step4Indicator, step > 4 ? "completed" : "inactive");

        // Action Button States
        backButton.setDisable(step == 1);
        nextButton.setVisible(step < 3);
        nextButton.setManaged(step < 3);

        generateButton.setVisible(step == 3);
        generateButton.setManaged(step == 3);

        uploadButton.setVisible(step == 4);
        uploadButton.setManaged(step == 4);

        if (step == 4) {
            nextButton.setVisible(false);
            nextButton.setManaged(false);
        }

        // Show active step
        switch (step) {
            case 1:
                step1Box.setVisible(true);
                setIndicatorState(step1Indicator, "active");
                break;
            case 2:
                step2Box.setVisible(true);
                setIndicatorState(step2Indicator, "active");
                break;
            case 3:
                step3Box.setVisible(true);
                setIndicatorState(step3Indicator, "active");
                break;
            case 4:
                step4Box.setVisible(true);
                setIndicatorState(step4Indicator, "active");
                break;
        }
    }

    private void setIndicatorState(HBox indicator, String state) {
        indicator.getStyleClass().removeAll("step-indicator-active", "step-indicator-inactive",
                "step-indicator-completed");
        indicator.getStyleClass().add("step-indicator-" + state);
    }

    // ==========================================
    // File Browsing Logic
    // ==========================================

    @FXML
    public void handleBrowseSource(javafx.scene.input.MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Installer File (MSI or EXE)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Installers", "*.msi", "*.exe"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedSourceFile = file;
            selectedFileLabel.setText(file.getAbsolutePath());
            logMessage("Selected source file: " + file.getAbsolutePath());

            // Auto-fill App details based on filename
            appNameField.setText(file.getName().replace(".msi", "").replace(".exe", ""));

            // Basic auto-fill for MSI
            if (file.getName().toLowerCase().endsWith(".msi")) {
                Map<String, String> props = MsiInspector.getMsiProperties(file.getAbsolutePath());
                appNameField.setText(props.getOrDefault("ProductName", file.getName().replace(".msi", "")));
                publisherField.setText(props.getOrDefault("Manufacturer", "Unknown Publisher"));
                versionField.setText(props.getOrDefault("ProductVersion", "1.0.0"));

                String productCode = props.getOrDefault("ProductCode", "{PRODUCT-CODE}");
                installCmdField.setText(
                        "%SystemRoot%\\sysnative\\WindowsPowerShell\\v1.0\\powershell.exe -windowstyle hidden -executionpolicy bypass -command .\\install.ps1");
                uninstallCmdField.setText(
                        "%SystemRoot%\\sysnative\\WindowsPowerShell\\v1.0\\powershell.exe -windowstyle hidden -executionpolicy bypass -command .\\uninstall.ps1");
                detectionRuleField.setText(productCode);
                logMessage("Auto-filled MSI details using PowerShell inspector.");
            } else if (file.getName().toLowerCase().endsWith(".exe")) {
                installCmdField.setText(
                        "%SystemRoot%\\sysnative\\WindowsPowerShell\\v1.0\\powershell.exe -windowstyle hidden -executionpolicy bypass -command .\\install.ps1");
                uninstallCmdField.setText(
                        "%SystemRoot%\\sysnative\\WindowsPowerShell\\v1.0\\powershell.exe -windowstyle hidden -executionpolicy bypass -command .\\uninstall.ps1");
                detectionRuleField.setText("C:\\Program Files\\...");
                logMessage("Auto-filled default EXE commands.");
            }
        }
    }

    @FXML
    public void handleBrowseOutput(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Output Folder for .intunewin");
        File dir = dirChooser.showDialog(null);
        if (dir != null) {
            outputFolderField.setText(dir.getAbsolutePath());
            logMessage("Selected output folder: " + dir.getAbsolutePath());
        }
    }

    // ==========================================
    // Packaging & Upload Logic
    // ==========================================

    @FXML
    public void handleGeneratePackage(ActionEvent event) {
        logMessage("Starting script generation process...");
        if (selectedSourceFile == null || outputFolderField.getText().isEmpty()) {
            logMessage("Error: Missing source file or output folder.");
            return;
        }

        generationProgressBox.setVisible(true);
        packageStatusLabel.setText("Preparing workspace...");
        packageProgressBar.setProgress(0.2);

        try {
            File sourceFile = selectedSourceFile;
            File outputDir = new File(outputFolderField.getText());

            // Create staging directory
            File stagingDir = new File(outputDir, "staging");
            if (!stagingDir.exists()) {
                stagingDir.mkdirs();
            }

            // Copy source file to staging
            packageStatusLabel.setText("Copying installer to staging...");
            File stagedSourceFile = new File(stagingDir, sourceFile.getName());
            Files.copy(sourceFile.toPath(), stagedSourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            packageProgressBar.setProgress(0.4);

            PackageDetails details = new PackageDetails();
            details.setSourcePath(sourceFile.getAbsolutePath());
            details.setSourceFileName(sourceFile.getName());
            details.setSourceType(sourceFile.getName().toLowerCase().endsWith(".msi") ? "MSI" : "EXE");
            details.setAppName(appNameField.getText());
            details.setPublisher(publisherField.getText());
            details.setVersion(versionField.getText());
            details.setDescription(descriptionArea.getText());
            details.setInstallCmd(installCmdField.getText());
            details.setUninstallCmd(uninstallCmdField.getText());
            details.setDetectionRule(detectionRuleField.getText());
            details.setPreInstallScript(preInstallScriptArea.getText());

            packageStatusLabel.setText("Generating PowerShell scripts...");
            ScriptGenerator generator = new ScriptGenerator();
            generator.generateInstallScript(stagingDir, details);
            generator.generateUninstallScript(stagingDir, details);
            generator.generateDetectScript(stagingDir, details);

            // Capture the content of the detection script for Intune upload
            File detectFile = new File(stagingDir, "detect.ps1");
            if (detectFile.exists()) {
                details.setDetectionScript(Files.readString(detectFile.toPath()));
            }

            packageProgressBar.setProgress(0.6);

            packageStatusLabel.setText("Running IntuneWinAppUtil.exe...");
            PackagerService packager = new PackagerService();
            String toolPath = settingsManager.getToolPath();

            if (toolPath == null || toolPath.isEmpty()) {
                logMessage("Error: IntuneWinAppUtil.exe path not configured in settings.");
                return;
            }

            boolean packageSuccess = packager.packageApp(stagingDir, "install.ps1", outputDir, toolPath);

            if (packageSuccess) {
                packageProgressBar.setProgress(1.0);
                packageStatusLabel.setText("Success!");
                logMessage("Successfully created .intunewin package in: " + outputDir.getAbsolutePath());
                handleNext(null); // Auto-advance to Step 4
            } else {
                packageProgressBar.setProgress(0.0);
                packageStatusLabel.setText("Failed.");
                logMessage("Failed to create .intunewin package.");
            }

        } catch (Exception e) {
            packageStatusLabel.setText("Error");
            logMessage("Error generating scripts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleOpenStagingFolder(ActionEvent event) {
        String outputDir = outputFolderField.getText();
        if (outputDir != null && !outputDir.isEmpty()) {
            File stagingDir = new File(outputDir, "staging");
            if (stagingDir.exists()) {
                MainApp.getAppHostServices().showDocument(stagingDir.toURI().toString());
            } else {
                MainApp.getAppHostServices().showDocument(new File(outputDir).toURI().toString());
            }
        }
    }

    @FXML
    public void handleTestPackage(ActionEvent event) {
        try {
            String outputDir = outputFolderField.getText();
            File stagingDir = new File(outputDir, "staging");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("test_mode.fxml"));
            javafx.scene.Parent root = loader.load();
            TestModeController controller = loader.getController();
            controller.setStagingDirectory(stagingDir);
            controller
                    .setLogCallback(msg -> javafx.application.Platform.runLater(() -> logMessage("[Testing] " + msg)));

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Testing Mode - " + appNameField.getText());
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.show();
        } catch (Exception e) {
            logMessage("Error opening Testing Mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUploadIntune(ActionEvent event) {
        try {
            String clientId = settingsManager.getClientId();
            String tenantId = settingsManager.getTenantId();

            if (clientId == null || tenantId == null || clientId.isEmpty() || tenantId.isEmpty()) {
                logMessage("Error: Azure AD credentials not configured in settings.");
                return;
            }

            File outputDir = new File(outputFolderField.getText());
            File intunewinFile = new File(outputDir, "install.intunewin");

            if (!intunewinFile.exists()) {
                logMessage("Could not find install.intunewin. Generate the package first.");
                return;
            }

            // Prepare details for the upload
            PackageDetails details = new PackageDetails();
            details.setAppName(appNameField.getText());
            details.setPublisher(publisherField.getText());
            details.setVersion(versionField.getText());
            details.setDescription(descriptionArea.getText());
            details.setInstallCmd(installCmdField.getText());
            details.setUninstallCmd(uninstallCmdField.getText());
            details.setDetectionRule(detectionRuleField.getText());

            // Read the generated detection script back in for the upload
            try {
                File stagingDir = new File(outputDir, "staging");
                File detectFile = new File(stagingDir, "detect.ps1");
                if (detectFile.exists()) {
                    details.setDetectionScript(Files.readString(detectFile.toPath()));
                }
            } catch (Exception e) {
                logMessage("Warning: Could not read detection script for metadata: " + e.getMessage());
            }

            logMessage("Initiating Intune authentication...");
            GraphAuthService authService = new GraphAuthService(clientId, tenantId);
            com.microsoft.graph.serviceclient.GraphServiceClient graphClient = authService.getGraphClient();

            // Open the upload progress dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("upload_view.fxml"));
            javafx.scene.Parent root = loader.load();
            UploadController uploadController = loader.getController();
            uploadController.initData(graphClient, authService, intunewinFile, details);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Uploading to Intune - " + details.getAppName());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

            logMessage("Upload dialog opened.");

        } catch (Exception e) {
            logMessage("Error during upload initiation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleOpenSettings(ActionEvent event) {
        try {
            MainApp.showSetup();
        } catch (Exception e) {
            logMessage("Error opening settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logMessage(String message) {
        logArea.appendText(message + "\n");
    }
}

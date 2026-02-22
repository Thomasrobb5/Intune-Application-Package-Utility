package com.antigravity.intunepackager;

import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class IntuneUploadService {

    private static final Logger LOGGER = Logger.getLogger(IntuneUploadService.class.getName());
    private final GraphServiceClient graphClient;
    private final GraphAuthService authService;
    private Consumer<String> statusCallback;
    private Consumer<Double> progressCallback;

    public IntuneUploadService(GraphServiceClient graphClient, GraphAuthService authService) {
        this.graphClient = graphClient;
        this.authService = authService;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setProgressCallback(Consumer<Double> callback) {
        this.progressCallback = callback;
    }

    private void updateStatus(String msg) {
        LOGGER.info(msg);
        if (statusCallback != null)
            statusCallback.accept(msg);
    }

    private void updateProgress(double progress) {
        if (progressCallback != null)
            progressCallback.accept(progress);
    }

    /**
     * Uploads a .intunewin package to Intune by creating metadata object in Java
     * and delegating the complex binary upload and linkage handshake to PowerShell.
     */
    public void uploadIntunewin(File intunewinFile, PackageDetails details) throws Exception {
        updateStatus("Initializing Intune upload via robust PowerShell engine...");
        updateProgress(0.05);

        // 1. Create the App Entry (Java SDK is fine for this part)
        updateStatus("Preparing application metadata object...");
        Win32LobApp app = new Win32LobApp();
        app.setDisplayName(details.getAppName());
        app.setPublisher(details.getPublisher());
        app.setDeveloper(details.getPublisher());
        app.setDescription(details.getPublisher() + " - " + details.getVersion());

        if (app.getAdditionalData() == null) {
            app.setAdditionalData(new HashMap<>());
        }
        app.getAdditionalData().put("displayVersion", details.getVersion());
        app.setInstallCommandLine(details.getInstallCmd());
        app.setUninstallCommandLine(details.getUninstallCmd());
        app.setFileName(intunewinFile.getName());
        app.setSetupFilePath("install.ps1");

        app.setMinimumSupportedWindowsRelease("1607");
        app.setApplicableArchitectures(java.util.EnumSet.of(WindowsArchitecture.X86, WindowsArchitecture.X64));
        app.setMinimumCpuSpeedInMHz(0);
        app.setMinimumMemoryInMB(0);
        app.setMinimumFreeDiskSpaceInMB(0);
        app.setMinimumNumberOfProcessors(0);

        Win32LobAppInstallExperience experience = new Win32LobAppInstallExperience();
        experience.setRunAsAccount(RunAsAccountType.System);
        experience.setDeviceRestartBehavior(Win32LobAppRestartBehavior.Suppress);
        app.setInstallExperience(experience);

        List<Win32LobAppReturnCode> codes = new ArrayList<>();
        Win32LobAppReturnCode successCode = new Win32LobAppReturnCode();
        successCode.setReturnCode(0);
        successCode.setType(Win32LobAppReturnCodeType.Success);
        codes.add(successCode);

        Win32LobAppReturnCode softRebootCode = new Win32LobAppReturnCode();
        softRebootCode.setReturnCode(3010);
        softRebootCode.setType(Win32LobAppReturnCodeType.SoftReboot);
        codes.add(softRebootCode);

        app.setReturnCodes(codes);

        if (details.getDetectionScript() != null && !details.getDetectionScript().isEmpty()) {
            Win32LobAppPowerShellScriptRule scriptRule = new Win32LobAppPowerShellScriptRule();
            scriptRule.setRuleType(Win32LobAppRuleType.Detection);
            scriptRule.setScriptContent(Base64.getEncoder().encodeToString(details.getDetectionScript().getBytes()));
            scriptRule.setEnforceSignatureCheck(false);
            scriptRule.setRunAs32Bit(false);
            app.setRules(java.util.Collections.singletonList(scriptRule));
        }

        updateStatus("Syncing application metadata to Intune...");
        MobileApp createdApp = graphClient.deviceAppManagement().mobileApps().post(app);
        String appId = createdApp.getId();
        updateStatus("Metadata synced! APP_ID: " + appId);
        updateProgress(0.15);

        // 2. Delegate the binary upload and complex linkage to PowerShell
        updateStatus("Extracting token and handing off to PowerShell...");
        String accessToken = authService.getAccessToken();

        // Robust path resolution for the PowerShell script (works in IDE and as
        // installed app)
        String appPath = MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File appDir = new File(appPath).getParentFile();
        File scriptFile = new File(appDir, "IntuneUpload.ps1");

        // Fallback for execution from project root (IDE)
        if (!scriptFile.exists()) {
            scriptFile = new File("IntuneUpload.ps1");
        }

        updateStatus("Using script at: " + scriptFile.getAbsolutePath());

        java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(
                "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", scriptFile.getAbsolutePath(),
                "-AccessToken", accessToken,
                "-IntunewinFile", intunewinFile.getAbsolutePath(),
                "-AppId", appId,
                "-AppName", details.getAppName(),
                "-Publisher", details.getPublisher(),
                "-Version", details.getVersion(),
                "-Description", details.getDescription());
        pb.redirectErrorStream(true);

        java.lang.Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("STATUS: ")) {
                    updateStatus(line.substring(8));
                } else if (line.contains("PROGRESS: ")) {
                    try {
                        String pStr = line.split("PROGRESS: ")[1].split("%")[0];
                        updateProgress(0.15 + (0.85 * (Double.parseDouble(pStr) / 100.0)));
                    } catch (Exception ignored) {
                    }
                } else if (line.startsWith("ERROR: ")) {
                    updateStatus("PowerShell Fault: " + line.substring(7));
                }
                LOGGER.info("[PowerShell Engine] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("PowerShell execution failed (Exit Code: " + exitCode + ")");
        }

        updateStatus("Intune Deployment Successful! App is now Ready.");
        updateProgress(1.0);
    }
}

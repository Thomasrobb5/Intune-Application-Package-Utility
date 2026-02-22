package com.antigravity.intunepackager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import java.io.File;
import java.util.function.Consumer;

public class TestModeController {

    @FXML
    private TextArea consoleArea;

    private File stagingDirectory;
    private Consumer<String> logCallback;

    public void setLogCallback(Consumer<String> logCallback) {
        this.logCallback = logCallback;
    }

    public void setStagingDirectory(File stagingDirectory) {
        this.stagingDirectory = stagingDirectory;
        logToConsole("Staging directory set to: " + stagingDirectory.getAbsolutePath());
    }

    @FXML
    private void handleRunInstall() {
        runScript("install.ps1");
    }

    @FXML
    private void handleRunUninstall() {
        runScript("uninstall.ps1");
    }

    @FXML
    private void handleRunDetect() {
        runScript("detect.ps1");
    }

    @FXML
    private void handleClearConsole() {
        consoleArea.clear();
    }

    private void runScript(String scriptName) {
        File scriptFile = new File(stagingDirectory, scriptName);
        if (!scriptFile.exists()) {
            logToConsole("Error: Script not found: " + scriptFile.getAbsolutePath());
            return;
        }

        logToConsole("\n--- Attempting to run " + scriptName + " (Elevated) ---");

        new Thread(() -> {
            File logFile = null;
            File runnerFile = null;
            try {
                logFile = new File(stagingDirectory, "test_output.txt");
                runnerFile = new File(stagingDirectory, "test_runner.ps1");

                if (logFile.exists())
                    logFile.delete();
                if (runnerFile.exists())
                    runnerFile.delete();

                String logPath = logFile.getAbsolutePath();
                String targetPath = scriptFile.getAbsolutePath();

                logToConsole("Target Script: " + targetPath);
                logToConsole("Runner Script: " + runnerFile.getAbsolutePath());

                // Create a temporary runner script
                String runnerContent = "Start-Transcript -Path '" + logPath.replace("'", "''") + "' -Force\n" +
                        "Set-Location '" + stagingDirectory.getAbsolutePath().replace("'", "''") + "'\n" +
                        "Write-Host '--- STARTING SCRIPT: " + scriptName + " ---'\n" +
                        "& '" + targetPath.replace("'", "''") + "'\n" +
                        "Write-Host '--- SCRIPT FINISHED ---'\n" +
                        "Stop-Transcript";

                java.nio.file.Files.write(runnerFile.toPath(),
                        runnerContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Simplified command with comma-separated ArgumentList for PowerShell
                // Using double quotes for the runner path inside single quotes for ArgumentList
                // entry
                String psCommand = "Start-Process powershell -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-NoExit', '-File', '\""
                        + runnerFile.getAbsolutePath() + "\"' -Verb RunAs -Wait";

                logToConsole("Executing: " + psCommand);
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommand);
                pb.directory(stagingDirectory);
                Process process = pb.start();

                // Polling loop to read log file while process is running
                long lastPos = 0;
                int exitGraceCount = 0;
                while (process.isAlive() || exitGraceCount < 5) {
                    if (logFile.exists() && logFile.length() > lastPos) {
                        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(logFile, "r")) {
                            raf.seek(lastPos);
                            byte[] bytes = new byte[(int) (logFile.length() - lastPos)];
                            raf.readFully(bytes);
                            String newContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                            if (!newContent.isEmpty()) {
                                Platform.runLater(() -> logToConsole(newContent));
                            }
                            lastPos = logFile.length();
                        } catch (Exception e) {
                            // File might be locked
                        }
                    }
                    if (!process.isAlive()) {
                        exitGraceCount++;
                    }
                    Thread.sleep(1000);
                }

                Platform.runLater(() -> logToConsole("--- Execution Finished ---"));

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                Platform.runLater(() -> logToConsole("Fatal Error: " + errorMsg));
                e.printStackTrace();
            } finally {
                // Ensure files are removed after we are done polling
                // BUT let's keep them if there was a fatal error for debugging
                // if (logFile != null && logFile.exists()) logFile.delete();
                // if (runnerFile != null && runnerFile.exists()) runnerFile.delete();
            }
        }).start();
    }

    private void logToConsole(String message) {
        consoleArea.appendText(message + "\n");
        System.out.println("[TestMode] " + message);
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }
}

package com.antigravity.intunepackager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class PackagerService {
    private static final Logger LOGGER = Logger.getLogger(PackagerService.class.getName());

    /**
     * Packages the given staging directory into an .intunewin file.
     *
     * @param stagingDir The directory containing the source files and generated
     *                   scripts.
     * @param setupFile  The main setup file name (e.g., install.ps1 or the .msi).
     * @param outputDir  The directory where the .intunewin file should be created.
     * @param toolPath   The path to IntuneWinAppUtil.exe.
     * @return true if successful, false otherwise.
     */
    public boolean packageApp(File stagingDir, String setupFile, File outputDir, String toolPath) throws Exception {
        File toolExe = new File(toolPath);
        if (!toolExe.exists()) {
            throw new Exception("IntuneWinAppUtil.exe not found at: " + toolExe.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(
                toolExe.getAbsolutePath(),
                "-c", stagingDir.getAbsolutePath(),
                "-s", setupFile,
                "-o", outputDir.getAbsolutePath(),
                "-q");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }
}

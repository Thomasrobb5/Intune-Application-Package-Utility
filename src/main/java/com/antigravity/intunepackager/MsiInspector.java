package com.antigravity.intunepackager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MsiInspector {

    /**
     * Executes a PowerShell script to utilize the WindowsInstaller COM object
     * and extracts the core properties of an MSI file.
     *
     * @param msiPath Absolute path to the MSI file.
     * @return A map of properties (e.g., ProductCode, ProductName, ProductVersion,
     *         Manufacturer)
     */
    public static Map<String, String> getMsiProperties(String msiPath) {
        Map<String, String> props = new HashMap<>();
        try {
            String script = "$path = '" + msiPath.replace("'", "''") + "'; " +
                    "$wi = New-Object -com WindowsInstaller.Installer; " +
                    "$db = $wi.OpenDatabase($path, 0); " +
                    "$view = $db.OpenView(\"SELECT Property, Value FROM Property\"); " +
                    "$view.Execute(); " +
                    "while ($record = $view.Fetch()) { " +
                    "    if ($record.StringData(1) -match '^(ProductCode|ProductName|ProductVersion|Manufacturer)$') { "
                    +
                    "        Write-Output ($record.StringData(1) + '=' + $record.StringData(2)); " +
                    "    } " +
                    "}";

            String encodedScript = java.util.Base64.getEncoder().encodeToString(script.getBytes("UTF-16LE"));
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand",
                    encodedScript);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            props.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return props;
    }
}

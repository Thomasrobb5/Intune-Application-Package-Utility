import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestMsi {
    public static void main(String[] args) {
        String msiPath = "C:\\Users\\Thoma\\Downloads\\AmazonTest.msi";
        String script = "$path = '" + msiPath.replace("'", "''") + "'; " +
                "$wi = New-Object -com WindowsInstaller.Installer; " +
                "$db = $wi.OpenDatabase($path, 0); " +
                "$view = $db.OpenView(\"SELECT Property, Value FROM Property WHERE Property IN ('ProductCode', 'ProductName', 'ProductVersion', 'Manufacturer')\"); "
                +
                "$view.Execute(); " +
                "while ($record = $view.Fetch()) { " +
                "    Write-Output ($record.StringData(1) + '=' + $record.StringData(2)); " +
                "}";

        System.out.println("Script: " + script);
        try {
            String encoded = java.util.Base64.getEncoder().encodeToString(script.getBytes("UTF-16LE"));
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand",
                    encoded);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("OUT: " + line);
                }
            }
            p.waitFor();
            System.out.println("Exit: " + p.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

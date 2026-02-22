package com.antigravity.intunepackager;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

public class ScriptGenerator {

    private final VelocityEngine engine;

    public ScriptGenerator() {
        engine = new VelocityEngine();
        Properties props = new Properties();
        // Load templates from classpath
        props.setProperty("resource.loaders", "class");
        props.setProperty("resource.loader.class.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        engine.init(props);
    }

    public void generateInstallScript(File outputDir, PackageDetails details) throws Exception {
        generateScript(outputDir, "install.ps1", "/templates/install.ps1.vm", details);
    }

    public void generateUninstallScript(File outputDir, PackageDetails details) throws Exception {
        generateScript(outputDir, "uninstall.ps1", "/templates/uninstall.ps1.vm", details);
    }

    public void generateDetectScript(File outputDir, PackageDetails details) throws Exception {
        generateScript(outputDir, "detect.ps1", "/templates/detect.ps1.vm", details);
    }

    private void generateScript(File outputDir, String outputFileName, String templatePath, PackageDetails details)
            throws Exception {
        Template template = engine.getTemplate(templatePath);
        VelocityContext context = new VelocityContext();

        context.put("appName", details.getAppName());
        context.put("version", details.getVersion());
        context.put("publisher", details.getPublisher());
        context.put("sourceType", details.getSourceType());
        context.put("sourceFileName", details.getSourceFileName());

        // Command mappings
        if ("MSI".equals(details.getSourceType())) {
            context.put("installCmd", details.getInstallCmd()); // we pass msiexec /i ...
            context.put("uninstallCmd", details.getUninstallCmd()); // msiexec /x ...
        } else {
            // For EXE, we might want to isolate the args vs the main executable name
            context.put("installCmdArgs", details.getInstallCmd());
            context.put("uninstallCmdArgs", details.getUninstallCmd());
        }

        context.put("detectionRule", details.getDetectionRule());
        context.put("preInstallScript", details.getPreInstallScript());

        File outputFile = new File(outputDir, outputFileName);
        try (Writer writer = new FileWriter(outputFile)) {
            template.merge(context, writer);
        }
    }
}

# Intune Application Package Utility

A powerful, modern desktop application designed to streamline the process of packaging and uploading Win32 applications to Microsoft Intune. Built with JavaFX and the latest Microsoft Graph SDK, this utility automates the tedious parts of application management.

## 💾 Download

**[Download the latest installer (MSI) here](https://github.com/Thomasrobb5/Intune-Application-Package-Utility/releases/latest)**

*Note: Visit the [Releases](https://github.com/Thomasrobb5/Intune-Application-Package-Utility/releases) page for other versions.*

## 🚀 Features

- **Automated Script Generation**: Generate high-quality PowerShell installation, uninstallation, and detection scripts using customizable templates.
- **Smart Packaging**: Seamlessly integrate with `IntuneWinAppUtil.exe` to create `.intunewin` packages.
- **Direct Intune Upload**: Register and upload your applications directly to Microsoft Intune via the MS Graph API v6.
- **Modern UI**: A clean, responsive dashboard built with JavaFX and modern styling for a premium user experience.
- **Template Driven**: Uses Apache Velocity for flexible and extensible script templates.
- **Secure Authentication**: Integrated with Azure Identity for secure OAuth 2.0 authentication.

## 🛠️ Prerequisites

- **Java Development Kit (JDK) 21**: The application is built using Java 21 features.
- **Apache Maven**: Required for building the project from source.
- **Azure App Registration**: You'll need a Client ID, Client Secret, and Tenant ID with the following permissions:
  - `DeviceManagementApps.ReadWrite.All`

## 📖 Walkthrough

### 1. Initial Setup
On the first launch, you'll be prompted to enter your Azure App Registration details. These are stored locally to facilitate future uploads.

### 2. Dashboard
The main dashboard provides an overview of your packaging tasks and quick access to the creation wizard.

### 3. Packaging a New App
1. **Source Selection**: Drag and drop or browse for your application installer (MSI, EXE, etc.).
2. **Configure Scripts**: The utility will automatically suggest install and uninstall commands. You can customize the templates as needed.
3. **Detection Rules**: Define how Intune should detect the application (File, Registry, or MSI Product Code).
4. **Build**: Click 'Build' to generate the `.intunewin` package.

### 4. Uploading to Intune
Once packaged, fill in the application metadata (Name, Description, Publisher, etc.) and click 'Upload'. The utility handles the chunked upload to Azure Blob Storage and finalizes the registration in Intune.

## 🏗️ Building from Source

To build the standalone executable:

```powershell
mvn clean package
```

The shaded JAR with all dependencies will be located in the `target/` directory.

## 🤝 Created By

**Thomas Robb** - *Solutions Engineer*

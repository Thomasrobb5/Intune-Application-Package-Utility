package com.antigravity.intunepackager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static javafx.application.HostServices hostServices;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        hostServices = getHostServices();
        primaryStage.setTitle("Intune Application Package Utility");

        // Set application icon
        try {
            URL iconUrl = getClass().getResource("/com/antigravity/intunepackager/intune.ico");
            if (iconUrl != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        SettingsManager settings = new SettingsManager();

        if (settings.isSetupComplete()) {
            showDashboard();
        } else {
            showSetup();
        }
    }

    public static void showDashboard() throws Exception {
        URL fxmlUrl = MainApp.class.getResource("/com/antigravity/intunepackager/dashboard.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("Could not find dashboard.fxml");
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1100, 850);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showSetup() throws Exception {
        URL fxmlUrl = MainApp.class.getResource("/com/antigravity/intunepackager/setup.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("Could not find setup.fxml");
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static javafx.application.HostServices getAppHostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.antigravity.intunepackager;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.File;

public class UploadController {

    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea uploadLogArea;
    @FXML
    private Button closeButton;

    private IntuneUploadService uploadService;
    private File intunewinFile;
    private PackageDetails details;

    public void initData(GraphServiceClient graphClient, GraphAuthService authService, File intunewinFile,
            PackageDetails details) {
        this.uploadService = new IntuneUploadService(graphClient, authService);
        this.intunewinFile = intunewinFile;
        this.details = details;

        uploadService.setStatusCallback(msg -> Platform.runLater(() -> {
            statusLabel.setText(msg);
            uploadLogArea.appendText(msg + "\n");
        }));

        uploadService.setProgressCallback(progress -> Platform.runLater(() -> {
            progressBar.setProgress(progress);
        }));

        startUpload();
    }

    private void startUpload() {
        new Thread(() -> {
            try {
                uploadService.uploadIntunewin(intunewinFile, details);
                Platform.runLater(() -> {
                    closeButton.setDisable(false);
                    statusLabel.setText("Upload Successful!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Upload Failed");
                    uploadLogArea.appendText("Error: " + e.getMessage() + "\n");
                    closeButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}

package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for editing an existing camera
 */
public class EditCameraDialog {
    private static final Logger logger = LoggerFactory.getLogger(EditCameraDialog.class);
    
    private Stage stage;
    private CameraService cameraService;
    private Camera camera;
    private TextField cameraNameField;
    private TextField locationField;
    private TextField urlField;
    private ComboBox<String> resolutionBox;
    private ComboBox<String> statusBox;
    private Runnable onSaveCallback;

    public EditCameraDialog(Window owner, CameraService cameraService, Camera camera) {
        this.cameraService = cameraService;
        this.camera = camera;
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.setTitle("Edit Camera");
        this.stage.setWidth(400);
        this.stage.setHeight(400);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void show() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(15));
        gridPane.setHgap(10);
        gridPane.setVgap(15);

        // Camera ID (read-only)
        Label idLabel = new Label("Camera ID:");
        Label idValue = new Label(String.valueOf(camera.getId()));
        gridPane.add(idLabel, 0, 0);
        gridPane.add(idValue, 1, 0);

        // Camera Name
        Label nameLabel = new Label("Camera Name:");
        cameraNameField = new TextField();
        cameraNameField.setText(camera.getCameraName());
        gridPane.add(nameLabel, 0, 1);
        gridPane.add(cameraNameField, 1, 1);

        // Location
        Label locationLabel = new Label("Location:");
        locationField = new TextField();
        locationField.setText(camera.getLocation());
        gridPane.add(locationLabel, 0, 2);
        gridPane.add(locationField, 1, 2);

        // Camera URL
        Label urlLabel = new Label("Stream URL:");
        urlField = new TextField();
        urlField.setText(camera.getCameraUrl());
        gridPane.add(urlLabel, 0, 3);
        gridPane.add(urlField, 1, 3);

        // Resolution
        Label resolutionLabel = new Label("Resolution:");
        resolutionBox = new ComboBox<>();
        resolutionBox.getItems().addAll("640x480", "1280x720", "1920x1080");
        resolutionBox.setValue(camera.getResolution());
        gridPane.add(resolutionLabel, 0, 4);
        gridPane.add(resolutionBox, 1, 4);

        // Status
        Label statusLabel = new Label("Status:");
        statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE", "DISABLED");
        statusBox.setValue(camera.getStatus());
        gridPane.add(statusLabel, 0, 5);
        gridPane.add(statusBox, 1, 5);

        // Buttons
        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-padding: 8 30");
        saveButton.setOnAction(e -> saveCameraAction());

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-padding: 8 30");
        cancelButton.setOnAction(e -> stage.close());

        GridPane buttonPane = new GridPane();
        buttonPane.setHgap(10);
        buttonPane.add(saveButton, 0, 0);
        buttonPane.add(cancelButton, 1, 0);

        gridPane.add(new Separator(), 0, 6);
        GridPane.setColumnSpan(new Separator(), 2);

        gridPane.add(buttonPane, 0, 7);
        GridPane.setColumnSpan(buttonPane, 2);

        Scene scene = new Scene(gridPane);
        stage.setScene(scene);
        stage.show();
    }

    private void saveCameraAction() {
        try {
            String cameraName = cameraNameField.getText().trim();
            String location = locationField.getText().trim();
            String url = urlField.getText().trim();
            String resolution = resolutionBox.getValue();
            String status = statusBox.getValue();

            if (cameraName.isEmpty() || url.isEmpty()) {
                showError("Validation Error", "Camera name and URL are required");
                return;
            }

            camera.setCameraName(cameraName);
            camera.setLocation(location);
            camera.setCameraUrl(url);
            camera.setResolution(resolution);
            camera.setStatus(status);

            cameraService.updateCamera(camera);
            logger.info("Camera updated: " + cameraName);
            showInfo("Success", "Camera updated successfully");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            stage.close();
        } catch (Exception e) {
            logger.error("Error updating camera", e);
            showError("Error", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

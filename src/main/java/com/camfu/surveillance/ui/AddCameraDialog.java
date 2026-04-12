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
 * Dialog for adding a new camera
 */
public class AddCameraDialog {
    private static final Logger logger = LoggerFactory.getLogger(AddCameraDialog.class);
    
    private Stage stage;
    private CameraService cameraService;
    private TextField cameraNameField;
    private TextField locationField;
    private TextField urlField;
    private ComboBox<String> resolutionBox;
    private Runnable onSaveCallback;

    public AddCameraDialog(Window owner, CameraService cameraService) {
        this.cameraService = cameraService;
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.setTitle("Add Camera");
        this.stage.setWidth(400);
        this.stage.setHeight(350);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void show() {
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(15));
        gridPane.setHgap(10);
        gridPane.setVgap(15);

        // Camera Name
        Label nameLabel = new Label("Camera Name:");
        cameraNameField = new TextField();
        cameraNameField.setPromptText("e.g., Main Entrance");
        gridPane.add(nameLabel, 0, 0);
        gridPane.add(cameraNameField, 1, 0);

        // Location
        Label locationLabel = new Label("Location:");
        locationField = new TextField();
        locationField.setPromptText("e.g., Floor 1 - North");
        gridPane.add(locationLabel, 0, 1);
        gridPane.add(locationField, 1, 1);

        // Camera URL
        Label urlLabel = new Label("Stream URL:");
        urlField = new TextField();
        urlField.setPromptText("rtsp://... or http://...");
        gridPane.add(urlLabel, 0, 2);
        gridPane.add(urlField, 1, 2);

        // Resolution
        Label resolutionLabel = new Label("Resolution:");
        resolutionBox = new ComboBox<>();
        resolutionBox.getItems().addAll("640x480", "1280x720", "1920x1080");
        resolutionBox.setValue("1280x720");
        gridPane.add(resolutionLabel, 0, 3);
        gridPane.add(resolutionBox, 1, 3);

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

        gridPane.add(new Separator(), 0, 4);
        GridPane.setColumnSpan(new Separator(), 2);

        gridPane.add(buttonPane, 0, 5);
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

            if (cameraName.isEmpty() || url.isEmpty()) {
                showError("Validation Error", "Camera name and URL are required");
                return;
            }

            Camera camera = new Camera();
            camera.setCameraName(cameraName);
            camera.setLocation(location);
            camera.setCameraUrl(url);
            camera.setResolution(resolution);
            camera.setStatus("ACTIVE");

            cameraService.addCamera(camera);
            logger.info("Camera added: " + cameraName);
            showInfo("Success", "Camera added successfully");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            stage.close();
        } catch (Exception e) {
            logger.error("Error adding camera", e);
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
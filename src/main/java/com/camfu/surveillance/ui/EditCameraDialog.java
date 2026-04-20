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
    
    private double xOffset = 0;
    private double yOffset = 0;

    public EditCameraDialog(Window owner, CameraService cameraService, Camera camera) {
        this.cameraService = cameraService;
        this.camera = camera;
        this.stage = new Stage();
        this.stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        this.stage.initOwner(owner);
        this.stage.setTitle("Edit Camera");
        this.stage.setWidth(400);
        this.stage.setHeight(400);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void show() {
        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        root.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #374151; -fx-border-width: 1;");

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
        saveButton.setOnAction(e -> saveCameraAction());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> stage.close());

        GridPane buttonPane = new GridPane();
        buttonPane.setHgap(10);
        buttonPane.add(saveButton, 0, 0);
        buttonPane.add(cancelButton, 1, 0);

        gridPane.add(new Separator(), 0, 6);
        GridPane.setColumnSpan(new Separator(), 2);

        gridPane.add(buttonPane, 0, 7);
        GridPane.setColumnSpan(buttonPane, 2);

        root.setTop(createTitleBar());
        root.setCenter(gridPane);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
        } catch (Exception e) {
            logger.warn("Could not load stylesheet", e);
        }
        stage.setScene(scene);
        stage.show();
    }

    private javafx.scene.layout.HBox createTitleBar() {
        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox();
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(8, 10, 8, 15));
        titleBar.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; -fx-border-width: 0 0 1 0;");

        javafx.scene.layout.HBox titleContent = new javafx.scene.layout.HBox(10);
        titleContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        try {
            var logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(logoStream));
                logoView.setFitHeight(18);
                logoView.setPreserveRatio(true);
                titleContent.getChildren().add(logoView);
            }
        } catch (Exception e) {}

        Label titleLabel = new Label("Edit Camera");
        titleLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-font-size: 13px;");
        titleContent.getChildren().add(titleLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #E11D48; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));

        titleBar.getChildren().addAll(titleContent, spacer, closeBtn);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
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
        styleAlert(alert);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleAlert(alert);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("dialog-pane");
        } catch (Exception e) {}
    }
}

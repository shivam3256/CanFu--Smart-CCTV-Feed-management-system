package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Window for managing cameras
 */
public class ManageCamerasWindow {
    private static final Logger logger = LoggerFactory.getLogger(ManageCamerasWindow.class);
    
    private Stage stage;
    private CameraService cameraService;
    private TableView<Camera> tableView;

    public ManageCamerasWindow(Window owner, CameraService cameraService) {
        this.cameraService = cameraService;
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.setTitle("Manage Cameras");
        this.stage.setWidth(800);
        this.stage.setHeight(600);
    }

    public void show() {
        VBox root = new VBox();
        root.setPadding(new Insets(15));
        root.setSpacing(10);

        Label titleLabel = new Label("Manage Cameras");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold");

        // Create table
        tableView = new TableView<>();
        tableView.setStyle("-fx-font-size: 12");

        // Columns
        TableColumn<Camera, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));

        TableColumn<Camera, String> nameCol = new TableColumn<>("Camera Name");
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCameraName()));

        TableColumn<Camera, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLocation()));

        TableColumn<Camera, String> urlCol = new TableColumn<>("Stream URL");
        urlCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCameraUrl()));

        TableColumn<Camera, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));

        tableView.getColumns().addAll(idCol, nameCol, locationCol, urlCol, statusCol);

        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);

        // Button panel
        Button addButton = new Button("Add Camera");
        addButton.setOnAction(e -> {
            AddCameraDialog dialog = new AddCameraDialog(stage, cameraService);
            dialog.setOnSaveCallback(this::refreshCameras);
            dialog.show();
        });

        Button editButton = new Button("Edit");
        editButton.setDisable(true);
        editButton.setOnAction(e -> editSelectedCamera());

        Button deleteButton = new Button("Delete");
        deleteButton.setDisable(true);
        deleteButton.setOnAction(e -> deleteSelectedCamera());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshCameras());

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());

        // Enable/Disable edit and delete buttons based on selection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editButton.setDisable(newVal == null);
            deleteButton.setDisable(newVal == null);
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(addButton, editButton, deleteButton, new Separator(), refreshButton, new Separator(), closeButton);

        root.getChildren().addAll(titleLabel, tableView, buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        // Load cameras
        refreshCameras();
    }

    private void editSelectedCamera() {
        Camera selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Error", "Please select a camera to edit");
            return;
        }

        EditCameraDialog dialog = new EditCameraDialog(stage, cameraService, selected);
        dialog.setOnSaveCallback(this::refreshCameras);
        dialog.show();
    }

    private void deleteSelectedCamera() {
        Camera selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection Error", "Please select a camera to delete");
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Camera?");
        confirmAlert.setContentText("Are you sure you want to delete camera: " + selected.getCameraName() + "?");
        
        java.util.Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        new Thread(() -> {
            try {
                cameraService.deleteCamera(selected.getId());
                logger.info("Camera deleted: " + selected.getCameraName());
                javafx.application.Platform.runLater(() -> {
                    showInfo("Success", "Camera deleted successfully");
                    refreshCameras();
                });
            } catch (Exception e) {
                logger.error("Error deleting camera", e);
                javafx.application.Platform.runLater(() -> {
                    showError("Error", "Failed to delete camera: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshCameras() {
        new Thread(() -> {
            try {
                java.util.List<Camera> cameras = cameraService.getAllCameras();
                javafx.application.Platform.runLater(() -> {
                    tableView.getItems().clear();
                    tableView.getItems().addAll(cameras);
                });
            } catch (Exception e) {
                logger.error("Error loading cameras", e);
            }
        }).start();
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
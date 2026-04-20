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
    
    private double xOffset = 0;
    private double yOffset = 0;

    public ManageCamerasWindow(Window owner, CameraService cameraService) {
        this.cameraService = cameraService;
        this.stage = new Stage();
        this.stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        this.stage.initOwner(owner);
        this.stage.setTitle("Manage Cameras");
        this.stage.setWidth(800);
        this.stage.setHeight(600);
    }

    public void show() {
        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        root.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #374151; -fx-border-width: 1;");

        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(15));
        contentBox.setSpacing(10);

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

        contentBox.getChildren().addAll(tableView, buttonBox);
        root.setTop(createTitleBar());
        root.setCenter(contentBox);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
        } catch (Exception e) {
            logger.warn("Could not load stylesheet", e);
        }
        stage.setScene(scene);
        stage.show();

        // Load cameras
        refreshCameras();
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(8, 10, 8, 15));
        titleBar.setStyle("-fx-background-color: #111827; -fx-border-color: #1F2937; -fx-border-width: 0 0 1 0;");

        HBox titleContent = new HBox(10);
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

        Label titleLabel = new Label("Manage Cameras");
        titleLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-font-size: 13px;");
        titleContent.getChildren().add(titleLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

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
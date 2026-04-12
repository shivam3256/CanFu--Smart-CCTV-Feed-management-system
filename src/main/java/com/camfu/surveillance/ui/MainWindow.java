package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.camfu.surveillance.service.AIEngineService;
import com.camfu.surveillance.service.CameraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main window for CamFu Desktop Application
 * Displays camera feeds with priority-based ranking
 */
public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private Stage primaryStage;
    private AIEngineService aiEngineService;
    private CameraService cameraService;
    private FeedGridPanel feedGridPanel;
    private PriorityTablePanel priorityTablePanel;

    public MainWindow(Stage primaryStage, AIEngineService aiEngineService) {
        this.primaryStage = primaryStage;
        this.aiEngineService = aiEngineService;
        this.cameraService = new CameraService();
    }

    public void show() {
        try {
            BorderPane root = new BorderPane();
            root.setStyle("-fx-font-family: 'Segoe UI'");

            // Top menu bar
            root.setTop(createMenuBar());

            // Center content with feed grid and priority table
            root.setCenter(createCenterContent());

            // Bottom status bar
            root.setBottom(createStatusBar());

            Scene scene = new Scene(root, 1600, 900);
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            } catch (Exception e) {
                logger.warn("Could not load stylesheet", e);
            }

            primaryStage.setTitle("CamFu - Intelligent Surveillance System");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1600);
            primaryStage.setHeight(900);
            primaryStage.setX(100);
            primaryStage.setY(100);
            
            // Setup proper shutdown handling
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Main window closing - initiating clean shutdown...");
                if (feedGridPanel != null) {
                    feedGridPanel.shutdown();
                }
                logger.info("Application shut down successfully");
            });
            
            // Show and focus window
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();

            logger.info("Main window displayed successfully at position (" + primaryStage.getX() + ", " + primaryStage.getY() + ")");
        } catch (Exception e) {
            logger.error("Error displaying main window", e);
            showError("Failed to initialize main window", e);
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().add(exitItem);

        // Camera Menu
        Menu cameraMenu = new Menu("Cameras");
        MenuItem addCameraItem = new MenuItem("Add Camera");
        addCameraItem.setOnAction(e -> openAddCameraDialog());
        MenuItem manageCameraItem = new MenuItem("Manage Cameras");
        manageCameraItem.setOnAction(e -> openManageCamerasWindow());
        cameraMenu.getItems().addAll(addCameraItem, new SeparatorMenuItem(), manageCameraItem);

        // Settings Menu
        Menu settingsMenu = new Menu("Settings");
        MenuItem preferencesItem = new MenuItem("Preferences");
        preferencesItem.setOnAction(e -> openPreferencesWindow());
        settingsMenu.getItems().add(preferencesItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, cameraMenu, settingsMenu, helpMenu);
        return menuBar;
    }

    private SplitPane createCenterContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.7);

        // Left side - Feed Grid Panel
        feedGridPanel = new FeedGridPanel();
        feedGridPanel.initialize(cameraService);

        // Right side - Priority Table Panel
        priorityTablePanel = new PriorityTablePanel();
        priorityTablePanel.initialize(cameraService);

        splitPane.getItems().addAll(feedGridPanel.getPanel(), priorityTablePanel.getPanel());
        return splitPane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0");

        Label statusLabel = new Label("Ready");
        Label aiEngineStatus = new Label("AI Engine: Connecting...");
        Label feedCountLabel = new Label("Feeds: 0");

        // Update AI engine status
        updateAIEngineStatus(aiEngineStatus);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        statusBar.getChildren().addAll(statusLabel, spacer, feedCountLabel, new Separator(), aiEngineStatus);
        return statusBar;
    }

    private void updateAIEngineStatus(Label statusLabel) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                boolean isRunning = aiEngineService.isEngineRunning();
                javafx.application.Platform.runLater(() -> {
                    if (isRunning) {
                        statusLabel.setText("AI Engine: Running");
                        statusLabel.setStyle("-fx-text-fill: green");
                    } else {
                        statusLabel.setText("AI Engine: Offline");
                        statusLabel.setStyle("-fx-text-fill: red");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void openAddCameraDialog() {
        logger.info("Opening Add Camera dialog");
        AddCameraDialog dialog = new AddCameraDialog(primaryStage, cameraService);
        dialog.show();
    }

    private void openManageCamerasWindow() {
        logger.info("Opening Manage Cameras window");
        ManageCamerasWindow window = new ManageCamerasWindow(primaryStage, cameraService);
        window.show();
    }

    private void openPreferencesWindow() {
        logger.info("Opening Preferences window");
        PreferencesWindow window = new PreferencesWindow(primaryStage);
        window.show();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About CamFu");
        alert.setHeaderText("CamFu - Intelligent Surveillance System");
        alert.setContentText("Version 1.0.0\n\nAn AI-driven intelligent surveillance system with priority-based feed ranking.\n\n© 2024 CamFu Project");
        alert.showAndWait();
    }

    private void showError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
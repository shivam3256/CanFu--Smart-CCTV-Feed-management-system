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
    
    private double xOffset = 0;
    private double yOffset = 0;

    public MainWindow(Stage primaryStage, AIEngineService aiEngineService) {
        this.primaryStage = primaryStage;
        this.aiEngineService = aiEngineService;
        this.cameraService = new CameraService();
    }

    public void show() {
        try {
            BorderPane root = new BorderPane();
            // Styles are handled by application.css

            // Top panel (Title Bar + Menu)
            root.setTop(createTopPanel());

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

    private VBox createTopPanel() {
        VBox topPanel = new VBox();
        topPanel.getChildren().addAll(createTitleBar(), createMenuBar());
        return topPanel;
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(8, 10, 8, 15));
        titleBar.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #1F2937; -fx-border-width: 0 0 1 0;");

        HBox titleContent = new HBox(10);
        titleContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        try {
            var logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(logoStream));
                logoView.setFitHeight(20);
                logoView.setPreserveRatio(true);
                titleContent.getChildren().add(logoView);
            }
        } catch (Exception e) {}

        Label titleLabel = new Label("CamFu - Intelligent Surveillance System");
        titleLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-font-size: 13px;");
        titleContent.getChildren().add(titleLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox windowControls = new HBox(5);
        windowControls.setAlignment(javafx.geometry.Pos.CENTER);

        Button minimizeBtn = new Button("—");
        minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;");
        minimizeBtn.setOnAction(e -> primaryStage.setIconified(true));
        minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));
        minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));

        Button maximizeBtn = new Button("□");
        maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;");
        maximizeBtn.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));
        maximizeBtn.setOnMouseEntered(e -> maximizeBtn.setStyle("-fx-background-color: #1F2937; -fx-text-fill: #E2E8F0; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));
        maximizeBtn.setOnMouseExited(e -> maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            primaryStage.fireEvent(new javafx.stage.WindowEvent(primaryStage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            primaryStage.close();
        });
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #E11D48; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-padding: 2 10; -fx-cursor: hand;"));

        windowControls.getChildren().addAll(minimizeBtn, maximizeBtn, closeBtn);
        titleBar.getChildren().addAll(titleContent, spacer, windowControls);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            if (!primaryStage.isMaximized()) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                primaryStage.setMaximized(!primaryStage.isMaximized());
            }
        });

        return titleBar;
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
        statusBar.setStyle("-fx-border-color: #1F2937; -fx-border-width: 1 0 0 0; -fx-background-color: #111827;");

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
        styleAlert(alert);
        alert.setTitle("About CamFu");
        alert.setHeaderText("CamFu - Intelligent Surveillance System");
        alert.setContentText("Version 1.0.0\n\nAn AI-driven intelligent surveillance system with priority-based feed ranking.\n\n© 2024 CamFu Project");
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("dialog-pane");
        } catch (Exception e) {}
    }

    private void showError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleAlert(alert);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
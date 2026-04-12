package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preferences window for application settings
 */
public class PreferencesWindow {
    private static final Logger logger = LoggerFactory.getLogger(PreferencesWindow.class);
    
    private Stage stage;

    public PreferencesWindow(Window owner) {
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.setTitle("Preferences");
        this.stage.setWidth(500);
        this.stage.setHeight(400);
    }

    public void show() {
        VBox root = new VBox();
        root.setPadding(new Insets(15));
        root.setSpacing(15);

        Label titleLabel = new Label("Application Preferences");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold");

        // Settings tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // General Tab
        Tab generalTab = new Tab("General", createGeneralSettingsPane());
        generalTab.setDisable(false);

        // Database Tab
        Tab dbTab = new Tab("Database", createDatabaseSettingsPane());
        dbTab.setDisable(false);

        // AI Engine Tab
        Tab aiTab = new Tab("AI Engine", createAISettingsPane());
        aiTab.setDisable(false);

        tabPane.getTabs().addAll(generalTab, dbTab, aiTab);

        // Button panel
        Button saveButton = new Button("Save");
        saveButton.setPrefWidth(80);
        saveButton.setOnAction(e -> {
            logger.info("Preferences saved");
            showInfo("Preferences", "Preferences saved successfully");
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(saveButton, cancelButton);

        VBox.setVgrow(tabPane, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, tabPane, new Separator(), buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createGeneralSettingsPane() {
        VBox pane = new VBox();
        pane.setPadding(new Insets(15));
        pane.setSpacing(10);

        CheckBox darkModeCheckBox = new CheckBox("Enable Dark Mode");
        CheckBox autoStartCheckBox = new CheckBox("Auto-start on application launch");
        CheckBox notificationsCheckBox = new CheckBox("Enable notifications");

        Label refreshLabel = new Label("Refresh Interval (ms):");
        Spinner<Integer> refreshSpinner = new Spinner<>(1000, 10000, 2000, 1000);

        pane.getChildren().addAll(
            darkModeCheckBox,
            autoStartCheckBox,
            notificationsCheckBox,
            new Separator(),
            refreshLabel,
            refreshSpinner
        );

        return pane;
    }

    private VBox createDatabaseSettingsPane() {
        VBox pane = new VBox();
        pane.setPadding(new Insets(15));
        pane.setSpacing(10);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label hostLabel = new Label("Host:");
        TextField hostField = new TextField("localhost");

        Label portLabel = new Label("Port:");
        Spinner<Integer> portSpinner = new Spinner<>(1, 65535, 3306);

        Label userLabel = new Label("Username:");
        TextField userField = new TextField("root");

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        gridPane.add(hostLabel, 0, 0);
        gridPane.add(hostField, 1, 0);
        gridPane.add(portLabel, 0, 1);
        gridPane.add(portSpinner, 1, 1);
        gridPane.add(userLabel, 0, 2);
        gridPane.add(userField, 1, 2);
        gridPane.add(passLabel, 0, 3);
        gridPane.add(passField, 1, 3);

        Button testButton = new Button("Test Connection");
        testButton.setOnAction(e -> showInfo("Database", "Connection successful"));

        pane.getChildren().addAll(gridPane, testButton);
        return pane;
    }

    private VBox createAISettingsPane() {
        VBox pane = new VBox();
        pane.setPadding(new Insets(15));
        pane.setSpacing(10);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label hostLabel = new Label("AI Engine Host:");
        TextField hostField = new TextField("localhost");

        Label portLabel = new Label("Port:");
        Spinner<Integer> portSpinner = new Spinner<>(1, 65535, 5000);

        Label timeoutLabel = new Label("Timeout (ms):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5000, 60000, 30000, 5000);

        gridPane.add(hostLabel, 0, 0);
        gridPane.add(hostField, 1, 0);
        gridPane.add(portLabel, 0, 1);
        gridPane.add(portSpinner, 1, 1);
        gridPane.add(timeoutLabel, 0, 2);
        gridPane.add(timeoutSpinner, 1, 2);

        Button testButton = new Button("Test Connection");
        testButton.setOnAction(e -> showInfo("AI Engine", "Connection successful"));

        pane.getChildren().addAll(gridPane, testButton);
        return pane;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
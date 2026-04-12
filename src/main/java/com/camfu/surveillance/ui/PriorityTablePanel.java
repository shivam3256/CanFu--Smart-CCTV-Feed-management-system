package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.model.PriorityScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Panel that displays feed priority rankings in a table
 */
public class PriorityTablePanel {
    private static final Logger logger = LoggerFactory.getLogger(PriorityTablePanel.class);

    private VBox panel;
    private TableView<PriorityScore> tableView;

    public VBox getPanel() {
        return panel;
    }

    public void initialize(CameraService cameraService) {
        panel = new VBox();
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #ffffff");

        // Title
        Label titleLabel = new Label("Priority Rankings");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 5");

        // Create table
        tableView = new TableView<>();
        tableView.setStyle("-fx-font-size: 11");

        // Columns
        TableColumn<PriorityScore, String> cameraCol = new TableColumn<>("Camera");
        cameraCol.setCellValueFactory(new PropertyValueFactory<>("cameraName"));
        cameraCol.setPrefWidth(100);

        TableColumn<PriorityScore, Double> motionCol = new TableColumn<>("Motion");
        motionCol.setCellValueFactory(new PropertyValueFactory<>("motionScore"));
        motionCol.setPrefWidth(70);

        TableColumn<PriorityScore, Double> crowdCol = new TableColumn<>("Crowd");
        crowdCol.setCellValueFactory(new PropertyValueFactory<>("crowdDensityScore"));
        crowdCol.setPrefWidth(70);

        TableColumn<PriorityScore, Double> behaviorCol = new TableColumn<>("Behavior");
        behaviorCol.setCellValueFactory(new PropertyValueFactory<>("unusualBehaviorScore"));
        behaviorCol.setPrefWidth(70);

        TableColumn<PriorityScore, Double> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("overallPriorityScore"));
        priorityCol.setPrefWidth(80);

        tableView.getColumns().addAll(cameraCol, motionCol, crowdCol, behaviorCol, priorityCol);

        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
        panel.getChildren().addAll(titleLabel, tableView);

        // Load priority data
        loadPriorityScores(cameraService);
    }

    private void loadPriorityScores(CameraService cameraService) {
        new Thread(() -> {
            while (true) {
                try {
                    List<PriorityScore> scores = cameraService.getPriorityScores();
                    javafx.application.Platform.runLater(() -> {
                        ObservableList<PriorityScore> data = FXCollections.observableArrayList(scores);
                        tableView.setItems(data);
                    });
                    Thread.sleep(2000); // Update every 2 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error loading priority scores", e);
                }
            }
        }).start();
    }
}
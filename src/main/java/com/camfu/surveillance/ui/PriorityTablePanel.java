package com.camfu.surveillance.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.model.PriorityScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Panel that displays feed priority rankings in a table.
 *
 * CRITICAL FIX: The original infinite loop had NO sleep on exceptions.
 * When the DB was unavailable, it would spin at 100% CPU in a tight
 * error loop — creating hundreds of Platform.runLater() calls per second
 * and starving the JavaFX render thread, causing "Not Responding".
 *
 * Fix: Added 5-second back-off sleep on any exception, and made the
 * polling thread interruptible so it stops cleanly on window close.
 */
public class PriorityTablePanel {
    private static final Logger logger = LoggerFactory.getLogger(PriorityTablePanel.class);

    private static final long POLL_INTERVAL_MS  = 3_000;  // normal refresh: every 3 s
    private static final long ERROR_BACKOFF_MS  = 5_000;  // back-off after error: 5 s

    private VBox panel;
    private TableView<PriorityScore> tableView;

    /** The polling thread — kept as a field so shutdown() can stop it. */
    private volatile Thread pollingThread;

    public VBox getPanel() { return panel; }

    public void initialize(CameraService cameraService) {
        panel = new VBox();
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: transparent;");

        Label titleLabel = new Label("Priority Rankings");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 5;");

        tableView = new TableView<>();
        tableView.setStyle("-fx-font-size: 13px;");
        tableView.setPlaceholder(new Label("No priority data yet — AI engine is analysing feeds."));

        TableColumn<PriorityScore, String> cameraCol = new TableColumn<>("Camera");
        cameraCol.setCellValueFactory(new PropertyValueFactory<>("cameraName"));
        cameraCol.setPrefWidth(100);

        TableColumn<PriorityScore, Double> motionCol = new TableColumn<>("Motion");
        motionCol.setCellValueFactory(new PropertyValueFactory<>("motionScore"));
        motionCol.setPrefWidth(65);

        TableColumn<PriorityScore, Double> crowdCol = new TableColumn<>("Crowd");
        crowdCol.setCellValueFactory(new PropertyValueFactory<>("crowdDensityScore"));
        crowdCol.setPrefWidth(65);

        TableColumn<PriorityScore, Double> behaviorCol = new TableColumn<>("Behavior");
        behaviorCol.setCellValueFactory(new PropertyValueFactory<>("unusualBehaviorScore"));
        behaviorCol.setPrefWidth(70);

        TableColumn<PriorityScore, Double> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("overallPriorityScore"));
        priorityCol.setPrefWidth(70);

        tableView.getColumns().addAll(cameraCol, motionCol, crowdCol, behaviorCol, priorityCol);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        panel.getChildren().addAll(titleLabel, tableView);

        startPolling(cameraService);
    }

    private void startPolling(CameraService cameraService) {
        pollingThread = new Thread(() -> {
            logger.debug("Priority table polling started");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<PriorityScore> scores = cameraService.getPriorityScores();

                    // Only touch the UI on the JavaFX thread
                    Platform.runLater(() -> {
                        ObservableList<PriorityScore> data = FXCollections.observableArrayList(scores);
                        tableView.setItems(data);
                    });

                    Thread.sleep(POLL_INTERVAL_MS);

                } catch (InterruptedException e) {
                    // Shutdown requested — exit cleanly
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // DB or service error — back off before retrying to avoid busy-spin
                    logger.warn("Priority score fetch failed, retrying in {}ms: {}",
                            ERROR_BACKOFF_MS, e.getMessage());
                    try {
                        Thread.sleep(ERROR_BACKOFF_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            logger.debug("Priority table polling stopped");
        }, "PriorityPoller");

        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    /** Stop the polling thread — call when the window is closing. */
    public void shutdown() {
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }
}
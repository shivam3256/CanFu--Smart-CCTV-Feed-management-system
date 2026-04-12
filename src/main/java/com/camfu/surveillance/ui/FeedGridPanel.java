package com.camfu.surveillance.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.util.VLCStreamPlayer;
import com.camfu.surveillance.util.VideoStreamUtil;
import com.camfu.surveillance.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Professional VLC-based camera feed display panel
 * 
 * Architecture:
 * - Display Layer: VLC (native, low-latency, hardware-accelerated)
 * - AI/Analysis Layer: FFmpeg (separate frame extraction for processing)
 * 
 * This separation provides:
 * - Native codec support and hardware acceleration for display
 * - Independent frame extraction for AI analysis
 * - Minimal latency for surveillance viewing
 * - Resilient streaming with automatic reconnection
 */
public class FeedGridPanel {
    private static final Logger logger = LoggerFactory.getLogger(FeedGridPanel.class);
    private static final int GRID_COLUMNS = 4;
    private static final int FEED_WIDTH = 320;
    private static final int FEED_HEIGHT = 240;

    private VBox panel;
    private GridPane gridPane;
    private ScrollPane scrollPane;
    private CameraService cameraService;
    private Map<Integer, VLCStreamPanel> vlcPanels = new ConcurrentHashMap<>();
    private Map<Integer, Thread> aiFrameThreads = new ConcurrentHashMap<>();
    private volatile boolean shouldContinue = true;

    public VBox getPanel() {
        return panel;
    }

    /**
     * Initialize panel with VLC streaming and AI frame extraction
     */
    public void initialize(CameraService cameraService) {
        this.cameraService = cameraService;
        
        try {
            // Initialize VLC system globally
            VLCStreamPlayer.initializeVLC();
            logger.info("VLC system initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize VLC: " + e.getMessage(), e);
        }
        
        panel = new VBox();
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f0f0f0");

        // Create top bar with title and controls
        HBox topBar = createTopBar();

        // Create grid for camera feeds
        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));

        scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-control-inner-background: #f0f0f0");

        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        panel.getChildren().addAll(topBar, scrollPane);

        // Load and display feeds
        loadFeeds();
    }
    
    /**
     * Create top control bar
     */
    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setSpacing(10);
        topBar.setPadding(new Insets(5));
        
        Label titleLabel = new Label("Live Camera Feeds - VLC Professional Streaming");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-padding: 5 15");
        refreshButton.setOnAction(e -> refreshFeeds());
        
        Button stopButton = new Button("Stop All");
        stopButton.setStyle("-fx-padding: 5 15");
        stopButton.setOnAction(e -> stopAllFeeds());
        
        topBar.getChildren().addAll(titleLabel, refreshButton, stopButton);
        return topBar;
    }

    /**
     * Refresh all camera feeds
     */
    public void refreshFeeds() {
        logger.info("Refreshing camera feeds...");
        stopAllFeeds();
        loadFeeds();
    }

    /**
     * Stop all streams and cleanup
     */
    public void stopAllFeeds() {
        logger.info("Stopping all camera feeds...");
        
        // Stop AI frame extraction threads
        for (Thread thread : aiFrameThreads.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        aiFrameThreads.clear();
        
        // Stop VLC streams
        for (VLCStreamPanel panel : vlcPanels.values()) {
            try {
                panel.stop();
                panel.cleanup();
            } catch (Exception e) {
                logger.warn("Error stopping panel: " + e.getMessage());
            }
        }
        vlcPanels.clear();
        
        logger.info("All feeds stopped");
    }

    /**
     * Load and display all camera feeds
     */
    private void loadFeeds() {
        new Thread(() -> {
            try {
                List<Camera> cameras = cameraService.getActiveCameras();
                javafx.application.Platform.runLater(() -> {
                    displayFeeds(cameras);
                });
            } catch (Exception e) {
                logger.error("Error loading feeds", e);
            }
        }).start();
    }

    /**
     * Display camera feeds in grid
     */
    private void displayFeeds(List<Camera> cameras) {
        gridPane.getChildren().clear();
        vlcPanels.clear();

        int row = 0, col = 0;
        for (Camera camera : cameras) {
            VBox feedContainer = createFeedContainer(camera);
            gridPane.add(feedContainer, col, row);

            col++;
            if (col >= GRID_COLUMNS) {
                col = 0;
                row++;
            }
        }
        
        logger.info("Displaying " + cameras.size() + " camera feeds with VLC");
    }

    /**
     * Create container with VLC display and camera info
     */
    private VBox createFeedContainer(Camera camera) {
        VBox container = new VBox();
        container.setStyle("-fx-border-color: #333333; -fx-border-width: 1");
        container.setPrefWidth(FEED_WIDTH);
        container.setPrefHeight(FEED_HEIGHT);
        container.setSpacing(5);

        // Create VLC stream panel
        VLCStreamPanel vlcPanel = new VLCStreamPanel(camera);
        vlcPanel.setPrefSize(FEED_WIDTH, FEED_HEIGHT - 40);
        vlcPanels.put(camera.getId(), vlcPanel);

        // Camera info label
        Label infoLabel = new Label(camera.getCameraName() + " - ");
        infoLabel.setStyle("-fx-text-fill: white; -fx-padding: 3 5; -fx-font-size: 10; -fx-background-color: #1a1a1a");

        container.getChildren().addAll(vlcPanel, infoLabel);
        
        // Start AI frame extraction thread (independent of display)
        startAIFrameExtractor(camera, infoLabel);

        return container;
    }

    /**
     * Separate thread for AI frame analysis
     * Uses FFmpeg for frame extraction, independent from VLC display
     */
    private void startAIFrameExtractor(Camera camera, Label statusLabel) {
        Thread aiThread = new Thread(() -> {
            logger.info("Starting AI frame extractor for: " + camera.getCameraName());
            int[] frameCount = {0}; // Use array to allow modification in lambda
            int[] errorCount = {0};
            final int ERROR_THRESHOLD = 10;
            final long RETRY_DELAY = 3000;

            while (!Thread.currentThread().isInterrupted() && shouldContinue) {
                try {
                    // Extract frame for AI analysis (FFmpeg)
                    // This is separate from the VLC display stream
                    var frame = VideoStreamUtil.fetchFrameFromMJPEG(camera.getStreamUrl());
                    
                    if (frame != null) {
                        frameCount[0]++;
                        errorCount[0] = 0;
                        
                        // Update status occasionally
                        if (frameCount[0] % 50 == 0) {
                            final int fc = frameCount[0];
                            javafx.application.Platform.runLater(() -> {
                                statusLabel.setText(camera.getCameraName() + " - Frames: " + fc);
                            });
                            logger.debug("AI frames: " + frameCount[0] + " for " + camera.getCameraName());
                        }
                    } else {
                        errorCount[0]++;
                        if (errorCount[0] > ERROR_THRESHOLD) {
                            logger.warn("AI frame extraction unavailable for: " + camera.getCameraName());
                            javafx.application.Platform.runLater(() -> {
                                statusLabel.setText(camera.getCameraName() + " ⚠ No AI frames");
                            });
                        }
                    }

                    // AI processing interval (can be different from display)
                    Thread.sleep(500);
                    
                } catch (InterruptedException e) {
                    logger.info("AI frame extractor stopped for: " + camera.getCameraName());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.debug("AI frame extraction error for: " + camera.getCameraName() + " - " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            logger.info("AI frame extractor terminated for: " + camera.getCameraName());
        });
        
        aiThread.setName("AI-Extractor-" + camera.getId());
        aiThread.setDaemon(true);
        aiFrameThreads.put(camera.getId(), aiThread);
        aiThread.start();
    }

    /**
     * Shutdown panel and cleanup all resources
     */
    public void shutdown() {
        logger.info("Shutting down FeedGridPanel");
        shouldContinue = false;
        stopAllFeeds();
        VLCStreamPlayer.shutdownVLC();
    }
}
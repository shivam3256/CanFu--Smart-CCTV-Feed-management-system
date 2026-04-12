package com.camfu.surveillance.ui;

import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.util.VideoStreamUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Direct MJPEG frame display panel for JavaFX cameras
 * Uses FFmpeg frame extraction for robust stream handling
 */
public class VLCStreamPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(VLCStreamPanel.class);
    
    private final Camera camera;
    private volatile boolean isPlaying = false;
    private volatile boolean stopRequested = false;
    private String currentStreamUrl;
    private ImageView imageView;
    private Thread streamThread;

    public VLCStreamPanel(Camera camera) {
        this.camera = camera;
        this.currentStreamUrl = camera.getStreamUrl();
        
        // Set up basic panel styling
        this.setStyle("-fx-border-color: #333333; -fx-border-width: 1; -fx-background-color: #000000;");
        this.setPrefSize(320, 240);
        
        // Initialize display
        initializePanel();
    }
    
    private void initializePanel() {
        try {
            String camName = camera.getName();
            logger.info("Initializing camera display for: " + camName);
            
            // Create image view for displaying frames
            imageView = new ImageView();
            imageView.setFitWidth(320);
            imageView.setFitHeight(240);
            imageView.setPreserveRatio(false);
            
            // Create stack pane with image and status overlay
            StackPane stackPane = new StackPane();
            stackPane.setStyle("-fx-background-color: #000000;");
            stackPane.setPrefSize(320, 240);
            stackPane.getChildren().add(imageView);
            
            this.setCenter(stackPane);
            
            // Start frame capture thread
            startFrameCapture();
            
            logger.info("Camera display initialized for: " + camName);
        } catch (Exception e) {
            logger.error("Failed to initialize camera panel: " + e.getMessage(), e);
            showErrorPlaceholder(e.getMessage());
        }
    }
    
    private void showPlaceholder(String camName) {
        // Create placeholder with dark background
        VBox centerBox = new VBox(15);
        centerBox.setStyle("-fx-background-color: #000000;");
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPrefSize(320, 240);
        
        // Camera name
        Label cameraLabel = new Label(camName);
        cameraLabel.setStyle("-fx-font-size: 16; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Streaming indicator
        Label streamLabel = new Label("▶ Fetching stream...");
        streamLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #00FF00;");
        
        // Stream URL
        Label urlLabel = new Label(currentStreamUrl != null ? currentStreamUrl : "No Stream URL");
        urlLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #CCCCCC; -fx-wrap-text: true;");
        
        centerBox.getChildren().addAll(cameraLabel, streamLabel, urlLabel);
        this.setCenter(centerBox);
        
        isPlaying = true;
        logger.info("Placeholder display shown for: " + camName);
    }
    
    private void showErrorPlaceholder(String errorMsg) {
        VBox errorBox = new VBox(10);
        errorBox.setStyle("-fx-background-color: #1a0000;");
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPrefSize(320, 240);
        
        Label errorLabel = new Label("⚠ Stream Error");
        errorLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #FF6666;");
        
        Label msgLabel = new Label(errorMsg);
        msgLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #FFCCCC; -fx-wrap-text: true;");
        
        errorBox.getChildren().addAll(errorLabel, msgLabel);
        this.setCenter(errorBox);
    }
    
    private void startFrameCapture() {
        stopRequested = false;
        streamThread = new Thread(() -> {
            logger.info("Frame capture thread started for: " + camera.getName());
            int consecutiveErrors = 0;
            
            while (!stopRequested && consecutiveErrors < 10) {
                try {
                    // Fetch frame from camera using existing MJPEG extraction
                    Image image = VideoStreamUtil.fetchFrameFromMJPEG(currentStreamUrl);
                    
                    if (image != null) {
                        // Display the image
                        Platform.runLater(() -> {
                            try {
                                imageView.setImage(image);
                                if (!isPlaying) {
                                    isPlaying = true;
                                    logger.info("Stream started displaying for: " + camera.getName());
                                }
                            } catch (Exception e) {
                                logger.debug("Error displaying frame: " + e.getMessage());
                            }
                        });
                        
                        consecutiveErrors = 0;
                        Thread.sleep(500); // 2 FPS frame rate
                    } else {
                        consecutiveErrors++;
                        logger.warn("Frame fetch returned null for: " + camera.getName());
                        Thread.sleep(1000);
                    }
                    
                } catch (InterruptedException e) {
                    logger.debug("Frame capture interrupted");
                    break;
                } catch (Exception e) {
                    consecutiveErrors++;
                    logger.warn("Error fetching frame: " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
            
            if (consecutiveErrors >= 10) {
                logger.warn("Frame capture stopped after repeated errors for: " + camera.getName());
            }
        }, "FrameCapture-" + camera.getId());
        
        streamThread.setDaemon(true);
        streamThread.start();
    }
    
    private void loadVLCComponent() {
        SwingUtilities.invokeLater(() -> {
            try {
                mediaComponent = VLCStreamPlayer.createMediaComponent();
                if (mediaComponent != null && VLCStreamPlayer.isValidStreamUrl(currentStreamUrl)) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            swingNode = new SwingNode();
                            swingNode.setContent(mediaComponent);
                            this.setCenter(swingNode);
                            VLCStreamPlayer.playStream(mediaComponent, currentStreamUrl);
                            logger.info("VLC component loaded for: " + camera.getName());
                        } catch (Exception e) {
                            logger.error("Error loading VLC: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                logger.debug("Could not load VLC: " + e.getMessage());
            }
        });
    }

    public void updateStreamUrl(String newUrl) {
        if (newUrl != null && !newUrl.equals(currentStreamUrl)) {
            this.currentStreamUrl = newUrl;
            logger.debug("Updated stream URL for: " + camera.getName());
        }
    }

    public void pause() {
        stopRequested = true;
        if (streamThread != null && streamThread.isAlive()) {
            try {
                streamThread.join(1000);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping frame capture");
            }
        }
        isPlaying = false;
        logger.debug("Stream paused for: " + camera.getName());
    }

    public void resume() {
        if (!isPlaying) {
            stopRequested = false;
            startFrameCapture();
            logger.debug("Stream resumed for: " + camera.getName());
        }
    }

    public void shutdown() {
        pause();
        logger.info("Stream panel shutdown for: " + camera.getName());
    }
}

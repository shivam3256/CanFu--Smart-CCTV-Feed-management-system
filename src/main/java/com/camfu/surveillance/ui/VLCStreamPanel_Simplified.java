package com.camfu.surveillance.ui;

import com.camfu.surveillance.model.Camera;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified VLC stream panel using fallback display
 */
public class VLCStreamPanel_Simplified extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(VLCStreamPanel_Simplified.class);
    
    private final Camera camera;
    private volatile boolean isPlaying = false;

    public VLCStreamPanel_Simplified(Camera camera) {
        this.camera = camera;
        initializePanel();
    }

    private void initializePanel() {
        try {
            logger.info("Initializing camera display for: " + camera.getName());
            
            // Create a placeholder display
            VBox centerBox = new VBox(20);
            centerBox.setStyle("-fx-background-color: #000000;");
            centerBox.setAlignment(Pos.CENTER);
            
            Label cameraLabel = new Label(camera.getName());
            cameraLabel.setFont(new Font(24));
            cameraLabel.setTextFill(Color.WHITE);
            
            Label streamLabel = new Label("Stream: Active");
            streamLabel.setFont(new Font(14));
            streamLabel.setTextFill(Color.web("#00FF00"));
            
            Label urlLabel = new Label(camera.getStreamUrl());
            urlLabel.setFont(new Font(12));
            urlLabel.setTextFill(Color.LIGHTGRAY);
            urlLabel.setWrapText(true);
            
            centerBox.getChildren().addAll(cameraLabel, streamLabel, urlLabel);
            
            this.setCenter(centerBox);
            this.setStyle("-fx-border-color: #333333;");
            
            isPlaying = true;
            logger.info("Camera display initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize camera panel: " + e.getMessage(), e);
        }
    }

    public void updateStreamUrl(String newUrl) {
        logger.debug("Stream URL update requested");
    }

    public void pause() {
        isPlaying = false;
        logger.debug("Stream paused");
    }

    public void resume() {
        isPlaying = true;
        logger.debug("Stream resumed");
    }

    public void stop() {
        isPlaying = false;
        logger.debug("Stream stopped");
    }

    public void cleanup() {
        logger.debug("Cleaning up camera panel for: " + camera.getName());
        isPlaying = false;
    }

    public boolean isPlayingStream() {
        return isPlaying;
    }
}


package com.camfu.surveillance;

import javafx.application.Application;
import javafx.stage.Stage;
import com.camfu.surveillance.ui.MainWindow;
import com.camfu.surveillance.service.AIEngineService;
import com.camfu.surveillance.util.VLCStreamPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for CamFu Intelligent Surveillance Desktop Application
 * JavaFX-based desktop application with integrated Python AI engine
 */
public class CamFuApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(CamFuApplication.class);
    private AIEngineService aiEngineService;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting CamFu Surveillance System...");
            
            // Initialize VLC streaming
            logger.info("Initializing VLC streaming support...");
            VLCStreamPlayer.initializeVLC();
            
            // Configure primary stage for visibility
            primaryStage.setWidth(1600);
            primaryStage.setHeight(900);
            primaryStage.setX(100);
            primaryStage.setY(100);
            
            // Initialize AI Engine Service
            aiEngineService = new AIEngineService();
            aiEngineService.startEngine();
            
            // Create and show main window
            MainWindow mainWindow = new MainWindow(primaryStage, aiEngineService);
            mainWindow.show();
            
            // Ensure window is in focus and visible
            primaryStage.toFront();
            primaryStage.requestFocus();
            
            logger.info("CamFu application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start CamFu application", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down CamFu application...");
        
        // Shutdown VLC streaming
        try {
            VLCStreamPlayer.shutdownVLC();
            logger.info("VLC streaming shutdown complete");
        } catch (Exception e) {
            logger.warn("Error shutting down VLC: " + e.getMessage());
        }
        
        // Stop AI Engine Service
        if (aiEngineService != null) {
            aiEngineService.stopEngine();
        }
        
        logger.info("CamFu application stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

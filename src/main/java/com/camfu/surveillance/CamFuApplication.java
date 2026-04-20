package com.camfu.surveillance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.camfu.surveillance.ui.MainWindow;
import com.camfu.surveillance.service.AIEngineService;
import com.camfu.surveillance.util.VLCStreamPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for CamFu Intelligent Surveillance Desktop Application.
 *
 * CRITICAL FIX: The original code called aiEngineService.startEngine() directly
 * on the JavaFX Application Thread. That method polls /health with Thread.sleep(1000)
 * for up to 30 seconds — freezing the entire UI ("Not Responding") on every launch.
 *
 * Fix: AI engine is started on a background thread. The UI shows immediately.
 * A status indicator in the window updates once the engine is ready.
 */
public class CamFuApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(CamFuApplication.class);
    private AIEngineService aiEngineService;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting CamFu Surveillance System...");

            // VLC init is fast (native library load) — safe to do on FX thread
            logger.info("Initializing VLC streaming support...");
            try {
                VLCStreamPlayer.initializeVLC();
            } catch (Exception e) {
                logger.warn("VLC init skipped: {}", e.getMessage());
            }

            primaryStage.setWidth(1600);
            primaryStage.setHeight(900);
            primaryStage.setX(100);
            primaryStage.setY(100);

            // Build and show the window IMMEDIATELY — no blocking
            aiEngineService = new AIEngineService();
            MainWindow mainWindow = new MainWindow(primaryStage, aiEngineService);
            mainWindow.show();

            primaryStage.toFront();
            primaryStage.requestFocus();

            // Start AI engine in background — does NOT block the FX thread
            Thread aiStartThread = new Thread(() -> {
                try {
                    aiEngineService.startEngine();
                    logger.info("AI Engine ready (background startup complete)");
                } catch (Exception e) {
                    logger.warn("AI Engine startup failed (non-fatal): {}", e.getMessage());
                }
            }, "AIEngineStarter");
            aiStartThread.setDaemon(true);
            aiStartThread.start();

            logger.info("CamFu application started successfully");

        } catch (Exception e) {
            logger.error("Failed to start CamFu application", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down CamFu application...");
        try {
            VLCStreamPlayer.shutdownVLC();
        } catch (Exception e) {
            logger.warn("Error shutting down VLC: {}", e.getMessage());
        }
        if (aiEngineService != null) {
            aiEngineService.stopEngine();
        }
        logger.info("CamFu application stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

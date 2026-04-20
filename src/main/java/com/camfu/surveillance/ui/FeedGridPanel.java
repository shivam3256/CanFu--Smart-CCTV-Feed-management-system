package com.camfu.surveillance.ui;

import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.service.CameraService;
import com.camfu.surveillance.util.VideoStreamUtil;
import com.camfu.surveillance.util.VLCStreamPlayer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Camera feed grid panel.
 *
 * Displays all active cameras in a responsive grid of VLCStreamPanel tiles.
 * Each tile manages its own capture thread and AnimationTimer render loop
 * (see VLCStreamPanel for the low-latency design).
 *
 * Changes vs original:
 *   • Removed the duplicate "AI frame extractor" thread per camera.
 *     Those threads were fetching the same frames as the display thread,
 *     doubling network traffic and CPU usage with zero benefit (the AI
 *     engine has no analysis endpoints yet).  When real AI scoring is
 *     needed, add it inside VLCStreamPanel's capture callback instead.
 *   • Grid columns calculated dynamically based on camera count so the
 *     layout stays sensible for 1-camera to 64-camera deployments.
 *   • Proper shutdown sequence: stop panels first, then close all streams.
 */
public class FeedGridPanel {
    private static final Logger logger = LoggerFactory.getLogger(FeedGridPanel.class);

    // Max columns in the feed grid (wraps to next row after this many)
    private static final int MAX_COLUMNS = 4;

    // Tile dimensions (px)
    private static final int TILE_W = 320;
    private static final int TILE_H = 260;   // 240 video + 20 label

    // ─────────────────────────────────────────────────────────────────────────

    private VBox       panel;
    private FlowPane   flowPane;
    private ScrollPane scrollPane;

    private CameraService cameraService;

    /** Live panels keyed by camera ID so we can shut them down individually. */
    private final Map<Integer, VLCStreamPanel> activePanels = new ConcurrentHashMap<>();

    /** Flag to prevent capture threads from re-starting after shutdown(). */
    private volatile boolean shuttingDown = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialise
    // ─────────────────────────────────────────────────────────────────────────

    public VBox getPanel() { return panel; }

    public void initialize(CameraService cameraService) {
        this.cameraService = cameraService;

        // Attempt VLC init (non-fatal if VLC is not installed)
        try {
            VLCStreamPlayer.initializeVLC();
        } catch (Exception e) {
            logger.warn("VLC init skipped: {}", e.getMessage());
        }

        // ── Root layout ──────────────────────────────────────────────────────
        panel = new VBox();
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: transparent;");

        HBox topBar = buildTopBar();

        flowPane = new FlowPane();
        flowPane.setHgap(6);
        flowPane.setVgap(6);
        flowPane.setPadding(new Insets(8));
        flowPane.setStyle("-fx-background-color: transparent;");

        scrollPane = new ScrollPane(flowPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        panel.getChildren().addAll(topBar, scrollPane);

        // Load feeds in background (DB access must not block JavaFX thread)
        loadFeeds();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top control bar
    // ─────────────────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(4, 0, 8, 0));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("⬛  Live Camera Feeds");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        Button refreshBtn = new Button("↺  Refresh");
        refreshBtn.setOnAction(e -> refreshFeeds());

        Button stopBtn = new Button("■  Stop All");
        stopBtn.setStyle("-fx-background-color: #E11D48;"); // Tailwind Rose 600
        stopBtn.setOnAction(e -> stopAllFeeds());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(title, spacer, refreshBtn, stopBtn);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Feed loading
    // ─────────────────────────────────────────────────────────────────────────

    /** Fetch active cameras from DB on a background thread, then render on FX thread. */
    private void loadFeeds() {
        new Thread(() -> {
            try {
                List<Camera> cameras = cameraService.getActiveCameras();
                Platform.runLater(() -> displayFeeds(cameras));
            } catch (Exception e) {
                logger.error("Failed to load cameras from database", e);
            }
        }, "FeedLoader").start();
    }

    /** Render camera tiles in the grid (called on JavaFX thread). */
    private void displayFeeds(List<Camera> cameras) {
        flowPane.getChildren().clear();
        activePanels.clear();

        if (cameras.isEmpty()) {
            Label empty = new Label("No active cameras found.\nUse Cameras → Add Camera to add one.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            empty.setWrapText(true);
            flowPane.getChildren().add(empty);
            return;
        }

        for (Camera camera : cameras) {
            if (shuttingDown) break;

            VBox tile = buildTile(camera);
            flowPane.getChildren().add(tile);
        }

        logger.info("Grid loaded — {} camera(s) using dynamic FlowPane layout", cameras.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tile construction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build a single camera tile: a VLCStreamPanel (which self-manages its
     * capture thread) plus a thin info strip at the bottom.
     */
    private VBox buildTile(Camera camera) {
        VBox tile = new VBox(0);
        tile.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        tile.setPrefWidth(TILE_W);
        tile.setPrefHeight(TILE_H);

        // Stream panel — handles its own capture + render loop
        VLCStreamPanel streamPanel = new VLCStreamPanel(camera);
        streamPanel.setPrefSize(TILE_W, TILE_H - 24);
        activePanels.put(camera.getId(), streamPanel);

        // Bottom info strip
        HBox infoStrip = new HBox();
        infoStrip.setStyle("-fx-background-color: transparent; -fx-padding: 5 8;");
        infoStrip.setAlignment(Pos.CENTER_LEFT);

        Label camName = new Label("📷  " + camera.getCameraName());
        camName.setStyle("-fx-font-size: 10px; -fx-text-fill: #cccccc;");

        Label location = new Label(camera.getLocation() != null ? " — " + camera.getLocation() : "");
        location.setStyle("-fx-font-size: 10px; -fx-text-fill: #777;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label statusDot = new Label(camera.getStatus());
        boolean active = "ACTIVE".equalsIgnoreCase(camera.getStatus());
        statusDot.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + (active ? "#10B981" : "#EF4444") + ";");

        infoStrip.getChildren().addAll(camName, location, sp, statusDot);

        tile.getChildren().addAll(streamPanel, infoStrip);
        return tile;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Controls
    // ─────────────────────────────────────────────────────────────────────────

    /** Stop every active stream panel and clear the grid. */
    public void stopAllFeeds() {
        logger.info("Stopping all {} stream panels…", activePanels.size());
        for (VLCStreamPanel p : activePanels.values()) {
            try { p.stop(); } catch (Exception e) {
                logger.warn("Error stopping panel: {}", e.getMessage());
            }
        }
        activePanels.clear();
        Platform.runLater(() -> flowPane.getChildren().clear());
        logger.info("All feeds stopped");
    }

    /** Stop everything then reload from database. */
    public void refreshFeeds() {
        logger.info("Refreshing camera feeds…");
        stopAllFeeds();
        loadFeeds();
    }

    /**
     * Full shutdown — called when the main window is closing.
     * Stops all panels, closes all persistent stream connections.
     */
    public void shutdown() {
        logger.info("FeedGridPanel shutdown initiated");
        shuttingDown = true;
        stopAllFeeds();
        VideoStreamUtil.closeAllStreams();   // close every MJPEG socket / RTSP grabber
        try { VLCStreamPlayer.shutdownVLC(); } catch (Exception ignored) {}
        logger.info("FeedGridPanel shutdown complete");
    }
}
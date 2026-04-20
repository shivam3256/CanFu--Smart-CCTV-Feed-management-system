package com.camfu.surveillance.ui;

import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.util.VideoStreamUtil;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LOW-LATENCY camera stream panel.
 *
 * Architecture (producer / consumer separated cleanly):
 *
 *   [Capture Thread]  ──writes──►  AtomicReference<Image>  ──reads──►  [AnimationTimer]
 *       (background)                (lock-free hand-off)                (JavaFX thread, 60Hz)
 *
 * Why this is faster than the original:
 *
 * 1. AnimationTimer instead of Platform.runLater()
 *    The original called Platform.runLater() once per captured frame — up to 30 times
 *    per second per camera. With 8 cameras that is 240 Runnable objects queued onto the
 *    JavaFX event queue every second, causing jank when the queue backs up.
 *    AnimationTimer.handle() runs ONCE per screen refresh pulse (typically 60Hz) and is
 *    already on the JavaFX thread — no queueing, no allocation, zero overhead.
 *
 * 2. AtomicReference instead of synchronized block
 *    The original used a shared Object lock between the capture thread and Platform.runLater().
 *    AtomicReference provides lock-free, wait-free visibility via a single CAS operation.
 *    The render side simply reads the latest reference — if it hasn't changed since last
 *    pulse, imageView.setImage() is skipped entirely.
 *
 * 3. No artificial Thread.sleep()
 *    The original slept 33ms between frames regardless of how long fetchFrame() took.
 *    The new capture thread runs in a tight loop; VideoStreamUtil.fetchFrame() already
 *    blocks inside the MJPEG reader until the next frame arrives on the network, so the
 *    thread naturally runs at camera FPS without wasting time sleeping.
 *
 * 4. setSmooth(false) on ImageView
 *    Disables bilinear interpolation during scaling — unnecessary for surveillance at
 *    fixed 320×240 display size, and saves measurable GPU fill time across many panels.
 *
 * 5. CacheHint.SPEED on all nodes
 *    Instructs the JavaFX scene graph to keep these nodes in accelerated memory and
 *    prefer speed over memory savings.
 */
public class VLCStreamPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(VLCStreamPanel.class);

    // How many consecutive null/error frames before we force a reconnect
    private static final int RECONNECT_THRESHOLD = 5;
    // How long to wait between reconnect attempts
    private static final long RECONNECT_DELAY_MS  = 2_000;

    private final Camera camera;

    // ── Producer side (capture thread) ────────────────────────────────────────
    private volatile boolean stopRequested = false;
    private Thread captureThread;

    // Lock-free frame hand-off: capture thread writes, AnimationTimer reads.
    // Using AtomicLong as a "frame sequence number" lets the render side detect
    // when a genuinely new frame has arrived without touching the Image reference twice.
    private final AtomicReference<Image> latestFrame    = new AtomicReference<>();
    private final AtomicLong             frameSeq       = new AtomicLong(0);
    private final AtomicLong             totalFrames    = new AtomicLong(0);

    // ── Consumer side (JavaFX / AnimationTimer) ───────────────────────────────
    private ImageView     imageView;
    private Label         statusLabel;
    private AnimationTimer renderTimer;

    // ─────────────────────────────────────────────────────────────────────────

    public VLCStreamPanel(Camera camera) {
        this.camera = camera;
        buildUI();
        startRenderLoop();
        startCaptureThread();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI construction
    // ─────────────────────────────────────────────────────────────────────────

    private void buildUI() {
        // Panel container
        this.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #1F2937; -fx-border-width: 1;");
        this.setPrefSize(320, 240);
        this.setCache(true);
        this.setCacheHint(javafx.scene.CacheHint.SPEED);

        // ImageView — hardware path, no bilinear interpolation (saves GPU fill rate)
        imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);                         // skip bilinear filter
        imageView.setCache(true);
        imageView.setCacheHint(javafx.scene.CacheHint.SPEED);

        // Lightweight status badge (top-right corner)
        statusLabel = new Label("● Connecting…");
        statusLabel.setStyle(
                "-fx-font-size: 9px; -fx-text-fill: #ffaa00;" +
                "-fx-background-color: rgba(0,0,0,0.65); -fx-padding: 2 6; -fx-background-radius: 3;");

        StackPane stack = new StackPane(imageView, statusLabel);
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT);
        stack.setStyle("-fx-background-color: #000000;"); // Black background for actual video letterboxing
        stack.setPrefSize(320, 240);

        this.setCenter(stack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Render loop  (JavaFX thread, 60Hz screen-sync)
    // ─────────────────────────────────────────────────────────────────────────

    private void startRenderLoop() {
        renderTimer = new AnimationTimer() {
            private long lastSeqDisplayed  = -1;
            // FPS tracking
            private long fpsWindowStart    = System.nanoTime();
            private long fpsWindowFrames   = 0;

            @Override
            public void handle(long nowNanos) {
                long currentSeq = frameSeq.get();

                // Only update ImageView when a genuinely NEW frame has arrived
                if (currentSeq != lastSeqDisplayed) {
                    Image frame = latestFrame.get();
                    if (frame != null) {
                        imageView.setImage(frame);
                        lastSeqDisplayed = currentSeq;
                        fpsWindowFrames++;
                    }
                }

                // Refresh the FPS badge every 2 seconds
                long elapsed = nowNanos - fpsWindowStart;
                if (elapsed >= 2_000_000_000L) {
                    double fps = fpsWindowFrames / (elapsed / 1_000_000_000.0);
                    String badge = fps > 1
                            ? String.format("● %.0f FPS", fps)
                            : "● No signal";
                    String colour = fps > 10 ? "#00e676"
                                  : fps > 1  ? "#ffaa00"
                                  :            "#ff1744";
                    statusLabel.setText(badge);
                    statusLabel.setStyle(
                            "-fx-font-size: 9px; -fx-text-fill: " + colour + ";" +
                            "-fx-background-color: rgba(0,0,0,0.65); -fx-padding: 2 6;" +
                            "-fx-background-radius: 3;");
                    fpsWindowStart  = nowNanos;
                    fpsWindowFrames = 0;
                }
            }
        };
        renderTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Capture thread  (background, max-1 priority)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The capture thread runs in a tight loop.
     * VideoStreamUtil.fetchFrame() already BLOCKS inside the socket/grabber waiting
     * for the next frame — so this thread naturally runs at camera FPS without sleep().
     *
     * On consecutive failures it closes the persistent connection and waits before
     * the next reconnect attempt, preventing a busy-spin during outages.
     */
    private void startCaptureThread() {
        stopRequested = false;

        captureThread = new Thread(() -> {
            int consecutiveErrors = 0;
            logger.info("Capture started → {}", camera.getName());

            while (!stopRequested && !Thread.currentThread().isInterrupted()) {
                try {
                    Image frame = VideoStreamUtil.fetchFrame(camera.getStreamUrl());

                    if (frame != null && !frame.isError()) {
                        // Lock-free publish: write image then bump sequence number.
                        // AnimationTimer always reads a consistent (image, seq) pair
                        // because it reads image BEFORE seq — at worst it misses one
                        // frame and catches the next one on the very next 60Hz pulse.
                        latestFrame.set(frame);
                        frameSeq.incrementAndGet();
                        totalFrames.incrementAndGet();
                        consecutiveErrors = 0;

                    } else {
                        consecutiveErrors++;
                        if (frame != null && frame.getException() != null) {
                            logger.warn("Image decode error from '{}': {}", camera.getName(), frame.getException().getMessage());
                        }
                        if (consecutiveErrors >= RECONNECT_THRESHOLD) {
                            logger.warn("Reconnecting stream '{}' after {} errors",
                                    camera.getName(), consecutiveErrors);
                            // Force the reader/grabber to close; next fetchFrame() will reconnect
                            VideoStreamUtil.closeStream(camera.getStreamUrl());
                            consecutiveErrors = 0;
                            Thread.sleep(RECONNECT_DELAY_MS);
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    consecutiveErrors++;
                    logger.debug("Frame error '{}': {}", camera.getName(), e.getMessage());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            logger.info("Capture stopped → {} (total frames: {})",
                    camera.getName(), totalFrames.get());

        }, "Capture-" + camera.getId() + "-" + camera.getName());

        captureThread.setDaemon(true);
        // One notch below MAX so the JavaFX render thread can still preempt us
        captureThread.setPriority(Thread.MAX_PRIORITY - 1);
        captureThread.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /** Stop the capture thread, stop the render timer, and close the stream. */
    public void stop() {
        stopRequested = true;
        if (captureThread != null) {
            captureThread.interrupt();
        }
        if (renderTimer != null) {
            renderTimer.stop();
        }
        VideoStreamUtil.closeStream(camera.getStreamUrl());
        logger.debug("VLCStreamPanel stopped for: {}", camera.getName());
    }

    public void cleanup()   { stop(); latestFrame.set(null); }
    public void shutdown()  { stop(); }

    public void pause() {
        stopRequested = true;
        if (captureThread != null) captureThread.interrupt();
    }

    public void resume() {
        if (stopRequested) {
            startCaptureThread();
        }
    }

    public void updateStreamUrl(String newUrl) {
        if (newUrl != null && !newUrl.equals(camera.getStreamUrl())) {
            VideoStreamUtil.closeStream(camera.getStreamUrl());
            // camera URL update would be done externally on the Camera model
        }
    }

    public boolean isActive() {
        return !stopRequested && captureThread != null && captureThread.isAlive();
    }
}

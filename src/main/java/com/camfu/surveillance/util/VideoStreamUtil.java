package com.camfu.surveillance.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.embed.swing.SwingFXUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.awt.image.BufferedImage;

/**
 * Utility for capturing video frames from multiple sources:
 * - HTTP MJPEG streams (IP Webcam, etc.)
 * - RTSP streams (security cameras, IP cameras - optimized for low latency)
 * Uses FFmpeg for RTSP with fast encoding settings to minimize latency
 */
public class VideoStreamUtil {
    private static final Logger logger = LoggerFactory.getLogger(VideoStreamUtil.class);
    private static final int TIMEOUT = 30000; // 30 seconds for HTTP streams
    private static final Java2DFrameConverter converter = new Java2DFrameConverter();

    /**
     * Fetch a single frame from a video stream (HTTP MJPEG or RTSP)
     * Automatically detects stream type and handles accordingly
     */
    public static Image fetchFrameFromMJPEG(String streamUrl) {
        // Check if this is an RTSP stream
        if (streamUrl.toLowerCase().startsWith("rtsp://")) {
            return fetchFrameFromRTSPFFmpeg(streamUrl);
        }
        
        // Handle HTTP/MJPEG streams
        return fetchFrameFromHTTP(streamUrl);
    }

    /**
     * Fetch frame from HTTP MJPEG stream
     */
    private static Image fetchFrameFromHTTP(String streamUrl) {
        try {
            // Determine the correct image URL
            String imageUrl = getImageUrl(streamUrl);
            logger.debug("Fetching frame from: " + imageUrl + " (original: " + streamUrl + ")");

            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT);
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("GET");
            // Add headers to look like a browser request
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/*,*/*");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Cache-Control", "no-cache");

            logger.debug("Connecting to: " + imageUrl + " (timeout: " + TIMEOUT + "ms)");
            int responseCode = conn.getResponseCode();
            logger.debug("HTTP Response Code: " + responseCode + " for URL: " + imageUrl);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                long contentLength = conn.getContentLength();
                logger.debug("Frame received - Content Length: " + contentLength + " bytes");
                Image image = new Image(inputStream);
                inputStream.close();
                conn.disconnect();
                logger.debug("Frame fetched and loaded successfully");
                return image;
            } else {
                logger.warn("Failed to fetch frame. HTTP response: " + responseCode + " for URL: " + imageUrl);
                logger.warn("Response message: " + conn.getResponseMessage());
            }
            conn.disconnect();
        } catch (Exception e) {
            logger.warn("Failed to fetch frame from " + streamUrl + ": " + e.getMessage());
            logger.debug("Exception details: ", e);
        }
        return null;
    }

    /**
     * Fetch frame from RTSP stream using FFmpeg command line - OPTIMIZED FOR LOW LATENCY
     * Uses fast JPEG output to minimize frame extraction time
     */
    private static Image fetchFrameFromRTSPFFmpeg(String streamUrl) {
        String tempImageFile = null;
        try {
            logger.debug("Fetching RTSP frame from: " + streamUrl);
            
            // Check if FFmpeg is available
            if (!isFFmpegAvailable()) {
                logger.warn("FFmpeg not found - RTSP streams require FFmpeg. Install FFmpeg to enable RTSP support.");
                return null;
            }
            
            String ffmpegPath = getFFmpegPath();
            if (ffmpegPath == null) {
                logger.warn("FFmpeg executable not found");
                return null;
            }
            
            // Create a temporary file for the output image (reuse filename to avoid excessive files)
            java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            tempImageFile = new java.io.File(tempDir, "ffmpeg_rtsp.jpg").getAbsolutePath();
            
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-rtsp_transport", "tcp",
                "-i", streamUrl,
                "-vframes", "1",          // Extract 1 frame only
                "-f", "image2",
                "-q:v", "5",              // Quality 5 (fast encoding)
                "-t", "5",                // Max 5 seconds to capture
                tempImageFile,
                "-hide_banner",
                "-loglevel", "error"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Short timeout - 8 seconds max for fast response
            boolean completed = process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                logger.debug("Frame timeout (stream may be offline)");
                return null;
            }
            
            if (process.exitValue() == 0) {
                java.io.File imageFile = new java.io.File(tempImageFile);
                if (imageFile.exists() && imageFile.length() > 0) {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(imageFile)) {
                        Image image = new Image(fis);
                        if (image != null && !image.isError()) {
                            return image;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("RTSP fetch error: " + e.getMessage());
        } finally {
            // Clean up temporary file (will be reused next time)
            if (tempImageFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempImageFile));
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if FFmpeg is available on the system
     */
    private static boolean isFFmpegAvailable() {
        return getFFmpegPath() != null;
    }
    
    /**
     * Get the full path to the FFmpeg executable
     */
    private static String getFFmpegPath() {
        // Try common Windows installation paths
        String[] possiblePaths = {
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Microsoft\\WinGet\\Links\\ffmpeg.exe",
            "C:\\Program Files\\FFmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files (x86)\\FFmpeg\\bin\\ffmpeg.exe"
        };
        
        for (String path : possiblePaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile()) {
                logger.debug("Found FFmpeg at: " + path);
                return path;
            }
        }
        
        // If not found, try executing "ffmpeg" from PATH as last resort
        try {
            ProcessBuilder testPb = new ProcessBuilder("cmd.exe", "/c", "ffmpeg -version >nul 2>&1 && exit /b 0 || exit /b 1");
            testPb.redirectErrorStream(true);
            Process testProcess = testPb.start();
            boolean completed = testProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            if (completed && testProcess.exitValue() == 0) {
                logger.debug("Found FFmpeg in PATH");
                return "ffmpeg";
            }
            if (!completed) {
                testProcess.destroyForcibly();
            }
        } catch (Exception e) {
            logger.debug("ffmpeg not found: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Convert BufferedImage to JavaFX Image efficiently using SwingFXUtils
     */
    private static Image convertBufferedImageToFXImage(BufferedImage bufferedImage) {
        try {
            if (bufferedImage == null) {
                logger.warn("BufferedImage is null - cannot convert");
                return null;
            }
            
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            
            if (width <= 0 || height <= 0) {
                logger.warn("Invalid BufferedImage dimensions: " + width + "x" + height);
                return null;
            }
            
            logger.debug("Converting BufferedImage to JavaFX Image: " + width + "x" + height);
            
            // Use SwingFXUtils for standard conversion - more efficient and handles edge cases
            Image javafxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            
            if (javafxImage != null) {
                logger.debug("BufferedImage converted to JavaFX Image successfully");
                return javafxImage;
            } else {
                logger.warn("SwingFXUtils conversion returned null");
                return null;
            }
        } catch (Exception e) {
            logger.warn("Error converting BufferedImage to JavaFX Image: " + e.getMessage());
            logger.debug("Conversion error details: ", e);
            return null;
        }
    }

    /**
     * Determine the correct image URL based on the stream URL (HTTP only)
     * Handles IP Webcam and other common formats
     */
    private static String getImageUrl(String streamUrl) {
        // If it's already a direct image URL, return as-is
        if (streamUrl.endsWith(".jpg") || streamUrl.endsWith(".jpeg") || streamUrl.endsWith(".png")) {
            return streamUrl;
        }

        // For IP Webcam video stream URLs
        if (streamUrl.contains("/video")) {
            // Replace /video with /shot.jpg
            return streamUrl.replace("/video", "/shot.jpg");
        }

        // For other stream URLs, try appending /shot.jpg
        if (!streamUrl.endsWith("/")) {
            streamUrl += "/";
        }
        return streamUrl + "shot.jpg";
    }

    /**
     * Get the proper shot URL for IP Webcam
     */
    public static String getShotUrl(String streamUrl) {
        return getImageUrl(streamUrl);
    }
}

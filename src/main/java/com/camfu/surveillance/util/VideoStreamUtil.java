package com.camfu.surveillance.util;

import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HIGH-PERFORMANCE Video Stream Utility
 *
 * Key optimizations vs original:
 * 1. PERSISTENT HTTP connection — one TCP handshake per camera, not one per frame.
 *    MJPEG is a multipart/x-mixed-replace stream; we keep the socket open and
 *    parse boundaries directly, eliminating all reconnect overhead.
 * 2. PERSISTENT FFmpegFrameGrabber — for RTSP, one grabber stays open for the
 *    lifetime of the stream. grabImage() blocks until the next decoded frame
 *    arrives (no file I/O, no ffmpeg process spawning, no temp files).
 * 3. ThreadLocal converters — Java2DFrameConverter is not thread-safe; using a
 *    separate instance per thread avoids locking.
 * 4. Buffered I/O — 64KB read buffer minimises system-call overhead.
 */
public class VideoStreamUtil {
    private static final Logger logger = LoggerFactory.getLogger(VideoStreamUtil.class);

    // ── SSL: accept self-signed certificates (IP Webcam HTTPS mode) ──────────
    static {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
            logger.debug("SSL trust-all installed for camera HTTPS streams");
        } catch (Exception e) {
            logger.warn("Could not install SSL trust-all: {}", e.getMessage());
        }
    }

    // One persistent MJPEG reader per camera URL
    private static final ConcurrentHashMap<String, MJPEGStreamReader> mjpegReaders =
            new ConcurrentHashMap<>();

    // One persistent FFmpegFrameGrabber per RTSP camera URL
    private static final ConcurrentHashMap<String, FFmpegFrameGrabber> rtspGrabbers =
            new ConcurrentHashMap<>();

    // ThreadLocal so every capture thread gets its own converter (no locking)
    private static final ThreadLocal<Java2DFrameConverter> converterLocal =
            ThreadLocal.withInitial(Java2DFrameConverter::new);

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fetch the next available frame from a camera stream.
     * Call this in a tight loop — it blocks until data arrives, so no sleep needed.
     */
    public static Image fetchFrame(String streamUrl) {
        if (streamUrl == null || streamUrl.isBlank()) return null;

        if (streamUrl.toLowerCase().startsWith("rtsp://")) {
            return fetchRTSPFrame(streamUrl);
        }
        return fetchMJPEGFrame(streamUrl);
    }

    /** Backward-compatible alias. */
    public static Image fetchFrameFromMJPEG(String streamUrl) {
        return fetchFrame(streamUrl);
    }

    /**
     * Permanently close a stream and release its resources.
     * Call this when a camera panel is being destroyed.
     */
    public static void closeStream(String streamUrl) {
        if (streamUrl == null) return;

        MJPEGStreamReader reader = mjpegReaders.remove(streamUrl);
        if (reader != null) {
            new Thread(() -> {
                try { reader.close(); } catch (Exception ignored) {}
            }).start();
        }

        FFmpegFrameGrabber grabber = rtspGrabbers.remove(streamUrl);
        if (grabber != null) {
            new Thread(() -> {
                try { grabber.stop(); grabber.close(); } catch (Exception ignored) {}
            }).start();
        }
    }

    /** Close every open stream (call on application shutdown). */
    public static void closeAllStreams() {
        mjpegReaders.forEach((url, r) -> {
            new Thread(() -> { try { r.close(); } catch (Exception ignored) {} }).start();
        });
        mjpegReaders.clear();

        rtspGrabbers.forEach((url, g) -> {
            new Thread(() -> { try { g.stop(); g.close(); } catch (Exception ignored) {} }).start();
        });
        rtspGrabbers.clear();
    }

    // -------------------------------------------------------------------------
    // MJPEG  (HTTP multipart/x-mixed-replace  OR  single JPEG endpoint)
    // -------------------------------------------------------------------------

    private static Image fetchMJPEGFrame(String streamUrl) {
        MJPEGStreamReader reader = mjpegReaders.get(streamUrl);
        if (reader == null) {
            reader = openMJPEGReader(streamUrl);
            if (reader != null) {
                mjpegReaders.put(streamUrl, reader);
            }
        }

        if (reader == null) return null;

        try {
            byte[] jpegBytes = reader.readNextFrame();
            if (jpegBytes != null && jpegBytes.length > 0) {
                return new Image(new ByteArrayInputStream(jpegBytes));
            }
        } catch (IOException e) {
            // Connection dropped — evict and reconnect on next call
            mjpegReaders.remove(streamUrl, reader);
            try { reader.close(); } catch (IOException ignored) {}
            logger.debug("MJPEG connection lost for '{}', will reconnect next frame: {}",
                    streamUrl, e.getMessage());
        }
        return null;
    }

    private static MJPEGStreamReader openMJPEGReader(String streamUrl) {
        try {
            String connectUrl = resolveStreamUrl(streamUrl);
            logger.info("Opening persistent MJPEG connection to: {}", connectUrl);

            URL url = new URL(connectUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(15_000);          // time to wait for the next frame
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "CamFu/1.0");
            conn.setRequestProperty("Accept",
                    "multipart/x-mixed-replace, image/jpeg, image/png, */*");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.connect();

            int code = conn.getResponseCode();
            if (code != 200) {
                logger.warn("HTTP {} opening MJPEG stream at {}", code, connectUrl);
                conn.disconnect();
                return null;
            }

            String contentType = conn.getContentType();
            logger.info("MJPEG stream opened ({}): {}", contentType, connectUrl);
            return new MJPEGStreamReader(conn, conn.getInputStream(), contentType);

        } catch (Exception e) {
            logger.warn("Cannot open MJPEG stream '{}': {}", streamUrl, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // RTSP  (persistent FFmpegFrameGrabber — zero process spawning)
    // -------------------------------------------------------------------------

    private static Image fetchRTSPFrame(String streamUrl) {
        FFmpegFrameGrabber grabber = rtspGrabbers.get(streamUrl);
        if (grabber == null) {
            grabber = openRTSPGrabber(streamUrl);
            if (grabber != null) {
                rtspGrabbers.put(streamUrl, grabber);
            }
        }

        if (grabber == null) return null;

        try {
            Frame frame = grabber.grabImage();   // blocks until next decoded frame
            if (frame != null && frame.image != null) {
                Java2DFrameConverter converter = converterLocal.get();
                BufferedImage bi = converter.convert(frame);
                if (bi != null) {
                    return SwingFXUtils.toFXImage(bi, null);
                }
            }
        } catch (Exception e) {
            // Grabber broken — evict and reconnect on next call
            rtspGrabbers.remove(streamUrl, grabber);
            try { grabber.stop(); grabber.close(); } catch (Exception ignored) {}
            logger.debug("RTSP grabber lost for '{}', will reconnect: {}", streamUrl, e.getMessage());
        }
        return null;
    }

    private static FFmpegFrameGrabber openRTSPGrabber(String streamUrl) {
        try {
            logger.info("Opening persistent RTSP grabber for: {}", streamUrl);

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl);

            // Transport
            grabber.setOption("rtsp_transport", "tcp");

            // Latency-critical FFmpeg options
            grabber.setOption("fflags",              "nobuffer");       // disable read buffer
            grabber.setOption("flags",               "low_delay");      // low-delay decoding
            grabber.setOption("probesize",           "32");             // minimal stream probe
            grabber.setOption("analyzeduration",     "0");              // skip analysis phase
            grabber.setOption("max_delay",           "0");              // no decoder delay
            grabber.setOption("reorder_queue_size",  "0");              // no PTS reordering
            grabber.setOption("avioflags",           "direct");         // bypass buffering in avio

            // Resolution (camera-side resize avoids client-side scaling cost)
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);

            grabber.start();
            logger.info("RTSP grabber connected to: {}", streamUrl);
            return grabber;

        } catch (Exception e) {
            logger.warn("Cannot open RTSP stream '{}': {}", streamUrl, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // URL Resolution
    // -------------------------------------------------------------------------

    /**
     * Resolve a user-supplied URL to the best streaming endpoint.
     * IP Webcam (Android app):  base URL → /video  gives the MJPEG multipart stream.
     * Single JPEG endpoint:     ends with .jpg / .jpeg / .mjpg — use as-is.
     */
    private static String resolveStreamUrl(String streamUrl) {
        if (streamUrl.endsWith(".jpg")
                || streamUrl.endsWith(".jpeg")
                || streamUrl.endsWith(".png")
                || streamUrl.endsWith(".mjpg")) {
            return streamUrl;
        }
        // IP Webcam: /video → multipart MJPEG stream (preferred over /shot.jpg snapshots)
        if (streamUrl.contains("/video")) {
            return streamUrl;       // already pointing at the stream
        }
        // Bare base URL — append /video for IP Webcam compatibility
        String base = streamUrl.endsWith("/") ? streamUrl : streamUrl + "/";
        return base + "video";
    }

    /** Returns the single-frame snapshot URL (used only if stream mode fails). */
    public static String getShotUrl(String streamUrl) {
        if (streamUrl.contains("/video")) {
            return streamUrl.replace("/video", "/shot.jpg");
        }
        String base = streamUrl.endsWith("/") ? streamUrl : streamUrl + "/";
        return base + "shot.jpg";
    }

    // -------------------------------------------------------------------------
    // MJPEG Stream Parser
    // -------------------------------------------------------------------------

    /**
     * Reads frames from a persistent HTTP MJPEG (multipart/x-mixed-replace) connection.
     *
     * Protocol format:
     *   --<boundary>\r\n
     *   Content-Type: image/jpeg\r\n
     *   Content-Length: <N>\r\n
     *   \r\n
     *   <N bytes of JPEG data>
     *   \r\n
     *   --<boundary>...
     *
     * If Content-Length is absent, we scan for the JPEG EOI marker (0xFF 0xD9).
     * For single-JPEG endpoints (non-multipart), we read the entire response body.
     */
    public static class MJPEGStreamReader implements Closeable {

        private static final int BUFFER_SIZE = 65_536; // 64 KB read buffer

        private final HttpURLConnection conn;
        private final InputStream      raw;
        private final BufferedInputStream buf;
        private final boolean          isMultipart;

        public MJPEGStreamReader(HttpURLConnection conn, InputStream stream, String contentType) {
            this.conn        = conn;
            this.raw         = stream;
            this.buf         = new BufferedInputStream(stream, BUFFER_SIZE);
            this.isMultipart = contentType != null
                    && contentType.toLowerCase().contains("multipart");
        }

        /**
         * Block until the next complete JPEG frame is available.
         * Returns the raw JPEG bytes, or throws IOException if the stream is broken.
         */
        public byte[] readNextFrame() throws IOException {
            return isMultipart ? readMultipartFrame() : readFullBody();
        }

        // -- Multipart parser -------------------------------------------------

        private byte[] readMultipartFrame() throws IOException {
            int contentLength = -1;
            boolean insideHeaders = false;
            String line;

            // Read MIME part headers
            while ((line = readLine()) != null) {
                if (line.isEmpty()) {
                    if (insideHeaders) {
                        // Blank line = end of headers, body follows
                        if (contentLength > 0) {
                            return readExactBytes(contentLength);
                        } else {
                            // No Content-Length — scan for JPEG EOI
                            return readUntilJpegEOI();
                        }
                    }
                    // Skip empty lines between frames
                    continue;
                }

                insideHeaders = true;
                String lower = line.toLowerCase();
                
                if (lower.startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(
                                line.substring(line.indexOf(':') + 1).trim());
                    } catch (NumberFormatException ignored) {}
                }
                // Skip boundary lines and other headers (Content-Type, etc.)
            }
            throw new IOException("Stream ended unexpectedly");
        }

        /** Read exactly {@code n} bytes (handles partial reads robustly). */
        private byte[] readExactBytes(int n) throws IOException {
            byte[] data = new byte[n];
            int offset = 0;
            while (offset < n) {
                int read = buf.read(data, offset, n - offset);
                if (read < 0) throw new IOException("Stream ended after " + offset + "/" + n + " bytes");
                offset += read;
            }
            return data;
        }

        /**
         * Scan for JPEG EOI (0xFF 0xD9) when Content-Length is unavailable.
         * Accumulates bytes until the end-of-image marker is found.
         */
        private byte[] readUntilJpegEOI() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32_768);
            int prev = -1, curr;
            while ((curr = buf.read()) != -1) {
                baos.write(curr);
                if (prev == 0xFF && curr == 0xD9) {
                    return baos.toByteArray();   // found EOI
                }
                prev = curr;
            }
            throw new IOException("Stream ended before JPEG EOI marker");
        }

        // -- Single-JPEG (non-multipart) reader --------------------------------

        /** Drain entire response body (for plain image/jpeg endpoints). */
        private byte[] readFullBody() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(65_536);
            byte[] tmp = new byte[8_192];
            int n;
            while ((n = buf.read(tmp)) != -1) {
                baos.write(tmp, 0, n);
            }
            return baos.toByteArray();
        }

        // -- Line reader -------------------------------------------------------

        /**
         * Read one CRLF-terminated (or LF-terminated) line from the stream.
         * Returns {@code null} if the stream is exhausted.
         */
        private String readLine() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            int b, prev = -1;
            while ((b = buf.read()) != -1) {
                if (b == '\n') {
                    // Strip trailing \r if present
                    byte[] bytes = baos.toByteArray();
                    int len = bytes.length;
                    if (len > 0 && bytes[len - 1] == '\r') len--;
                    return new String(bytes, 0, len);
                }
                baos.write(b);
                prev = b;
            }
            return null; // stream exhausted
        }

        @Override
        public void close() throws IOException {
            raw.close();
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

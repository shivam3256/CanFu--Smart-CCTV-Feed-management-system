package com.camfu.surveillance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service for managing the Python AI Engine
 * Handles starting, stopping, and communicating with the Python backend
 */
public class AIEngineService {
    private static final Logger logger = LoggerFactory.getLogger(AIEngineService.class);
    
    private static final String AI_ENGINE_HOST = "localhost";
    private static final int AI_ENGINE_PORT = 5000;
    private static final String PYTHON_EXECUTABLE = "python";
    private static final String PYTHON_SCRIPT_PATH = "./python/src/main.py";

    private Process engineProcess;
    private boolean isRunning = false;

    /**
     * Start the Python AI Engine
     */
    public void startEngine() {
        try {
            logger.info("Starting Python AI Engine...");
            
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXECUTABLE, PYTHON_SCRIPT_PATH);
            pb.redirectErrorStream(true);
            
            engineProcess = pb.start();
            isRunning = true;

            // Log output from Python process
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[AI Engine] " + line);
                    }
                } catch (Exception e) {
                    logger.error("Error reading AI engine output", e);
                }
            }).start();

            // Wait for engine to be ready
            waitForEngineReady();
            
            logger.info("Python AI Engine started successfully");
        } catch (Exception e) {
            logger.error("Failed to start Python AI Engine", e);
            isRunning = false;
        }
    }

    /**
     * Stop the Python AI Engine
     */
    public void stopEngine() {
        try {
            if (engineProcess != null) {
                logger.info("Stopping Python AI Engine...");
                engineProcess.destroy();
                engineProcess.waitFor();
                isRunning = false;
                logger.info("Python AI Engine stopped");
            }
        } catch (Exception e) {
            logger.error("Error stopping AI engine", e);
        }
    }

    /**
     * Check if AI engine is running
     */
    public boolean isEngineRunning() {
        if (!isRunning) return false;
        
        try {
            String url = String.format("http://%s:%d/health", AI_ENGINE_HOST, AI_ENGINE_PORT);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for the AI engine to be ready
     */
    private void waitForEngineReady() {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                if (isEngineRunning()) {
                    logger.info("AI Engine is ready");
                    return;
                }
                Thread.sleep(1000);
                attempt++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.warn("AI Engine did not become ready in time");
    }

    /**
     * Send a request to the AI engine
     */
    public String callAIEngine(String endpoint, String data) throws Exception {
        String url = String.format("http://%s:%d%s", AI_ENGINE_HOST, AI_ENGINE_PORT, endpoint);
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
        if (data != null) {
            conn.setDoOutput(true);
            try (var os = conn.getOutputStream()) {
                os.write(data.getBytes());
            }
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("AI Engine returned status code: " + responseCode);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
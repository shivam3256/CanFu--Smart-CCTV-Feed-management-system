package com.camfu.surveillance.model;

import java.time.LocalDateTime;

/**
 * Camera entity representing a surveillance camera
 */
public class Camera {
    private int id;
    private String cameraName;
    private String location;
    private String cameraUrl;
    private String resolution;
    private int fps;
    private String status; // ACTIVE, INACTIVE, MAINTENANCE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Camera() {
        this.fps = 15;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Camera(String cameraName, String location, String cameraUrl, String resolution) {
        this.cameraName = cameraName;
        this.location = location;
        this.cameraUrl = cameraUrl;
        this.resolution = resolution;
        this.fps = 15;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getName() {
        return cameraName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCameraUrl() {
        return cameraUrl;
    }

    public void setCameraUrl(String cameraUrl) {
        this.cameraUrl = cameraUrl;
    }

    public String getStreamUrl() {
        return cameraUrl;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
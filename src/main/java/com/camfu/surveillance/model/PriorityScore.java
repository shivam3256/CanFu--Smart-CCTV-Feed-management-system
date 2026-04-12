package com.camfu.surveillance.model;

import java.time.LocalDateTime;

/**
 * Priority Score entity for real-time feed ranking
 */
public class PriorityScore {
    private int id;
    private int cameraId;
    private String cameraName;
    private double motionScore;
    private double crowdDensityScore;
    private double unusualBehaviorScore;
    private double timeFactor;
    private double overallPriorityScore;
    private LocalDateTime timestamp;

    public PriorityScore() {
        this.timestamp = LocalDateTime.now();
    }

    public PriorityScore(int cameraId, String cameraName, double motionScore, 
                        double crowdDensity, double behaviorScore, double overallScore) {
        this.cameraId = cameraId;
        this.cameraName = cameraName;
        this.motionScore = motionScore;
        this.crowdDensityScore = crowdDensity;
        this.unusualBehaviorScore = behaviorScore;
        this.overallPriorityScore = overallScore;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public double getMotionScore() {
        return motionScore;
    }

    public void setMotionScore(double motionScore) {
        this.motionScore = motionScore;
    }

    public double getCrowdDensityScore() {
        return crowdDensityScore;
    }

    public void setCrowdDensityScore(double crowdDensityScore) {
        this.crowdDensityScore = crowdDensityScore;
    }

    public double getUnusualBehaviorScore() {
        return unusualBehaviorScore;
    }

    public void setUnusualBehaviorScore(double unusualBehaviorScore) {
        this.unusualBehaviorScore = unusualBehaviorScore;
    }

    public double getTimeFactor() {
        return timeFactor;
    }

    public void setTimeFactor(double timeFactor) {
        this.timeFactor = timeFactor;
    }

    public double getOverallPriorityScore() {
        return overallPriorityScore;
    }

    public void setOverallPriorityScore(double overallPriorityScore) {
        this.overallPriorityScore = overallPriorityScore;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
package com.camfu.surveillance.service;

import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.model.PriorityScore;
import com.camfu.surveillance.dao.CameraDAO;
import com.camfu.surveillance.dao.PriorityScoreDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Service for camera management and priority score retrieval
 */
public class CameraService {
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);
    
    private CameraDAO cameraDAO;
    private PriorityScoreDAO priorityScoreDAO;

    public CameraService() {
        this.cameraDAO = new CameraDAO();
        this.priorityScoreDAO = new PriorityScoreDAO();
    }

    /**
     * Get all cameras
     */
    public List<Camera> getAllCameras() throws Exception {
        logger.debug("Fetching all cameras from database");
        return cameraDAO.getAllCameras();
    }

    /**
     * Get only active cameras
     */
    public List<Camera> getActiveCameras() throws Exception {
        logger.debug("Fetching active cameras from database");
        return cameraDAO.getActiveCameras();
    }

    /**
     * Get a specific camera by ID
     */
    public Camera getCameraById(int id) throws Exception {
        logger.debug("Fetching camera with ID: " + id);
        return cameraDAO.getCameraById(id);
    }

    /**
     * Add a new camera
     */
    public void addCamera(Camera camera) throws Exception {
        logger.info("Adding new camera: " + camera.getCameraName());
        cameraDAO.addCamera(camera);
    }

    /**
     * Update an existing camera
     */
    public void updateCamera(Camera camera) throws Exception {
        logger.info("Updating camera: " + camera.getCameraName());
        cameraDAO.updateCamera(camera);
    }

    /**
     * Delete a camera
     */
    public void deleteCamera(int id) throws Exception {
        logger.info("Deleting camera with ID: " + id);
        cameraDAO.deleteCamera(id);
    }

    /**
     * Get priority scores for all cameras (ordered by priority)
     */
    public List<PriorityScore> getPriorityScores() throws Exception {
        return priorityScoreDAO.getLatestPriorityScores();
    }

    /**
     * Get priority scores for a specific camera
     */
    public List<PriorityScore> getPriorityScoresByCameraId(int cameraId) throws Exception {
        return priorityScoreDAO.getPriorityScoresByCameraId(cameraId);
    }

    /**
     * Store priority score
     */
    public void storePriorityScore(PriorityScore score) throws Exception {
        priorityScoreDAO.storePriorityScore(score);
    }
}
package com.camfu.surveillance.dao;

import com.camfu.surveillance.model.Camera;
import com.camfu.surveillance.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Camera entities
 */
public class CameraDAO {
    private static final Logger logger = LoggerFactory.getLogger(CameraDAO.class);

    /**
     * Get all cameras from database
     */
    public List<Camera> getAllCameras() throws Exception {
        List<Camera> cameras = new ArrayList<>();
        String query = "SELECT * FROM cameras ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cameras.add(mapResultSetToCamera(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all cameras", e);
            throw new Exception("Failed to fetch cameras from database", e);
        }

        return cameras;
    }

    /**
     * Get only active cameras
     */
    public List<Camera> getActiveCameras() throws Exception {
        List<Camera> cameras = new ArrayList<>();
        String query = "SELECT * FROM cameras WHERE status = 'ACTIVE' ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cameras.add(mapResultSetToCamera(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching active cameras", e);
            throw new Exception("Failed to fetch active cameras from database", e);
        }

        return cameras;
    }

    /**
     * Get a specific camera by ID
     */
    public Camera getCameraById(int id) throws Exception {
        String query = "SELECT * FROM cameras WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCamera(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching camera with ID: " + id, e);
            throw new Exception("Failed to fetch camera", e);
        }

        return null;
    }

    /**
     * Add a new camera
     */
    public void addCamera(Camera camera) throws Exception {
        String query = "INSERT INTO cameras (camera_name, location, camera_url, resolution, fps, status) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, camera.getCameraName());
            stmt.setString(2, camera.getLocation());
            stmt.setString(3, camera.getCameraUrl());
            stmt.setString(4, camera.getResolution());
            stmt.setInt(5, camera.getFps());
            stmt.setString(6, camera.getStatus());

            stmt.executeUpdate();
            logger.info("Camera added successfully: " + camera.getCameraName());
        } catch (SQLException e) {
            logger.error("Error adding camera", e);
            throw new Exception("Failed to add camera to database", e);
        }
    }

    /**
     * Update an existing camera
     */
    public void updateCamera(Camera camera) throws Exception {
        String query = "UPDATE cameras SET camera_name=?, location=?, camera_url=?, " +
                      "resolution=?, fps=?, status=?, updated_at=NOW() WHERE id=?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, camera.getCameraName());
            stmt.setString(2, camera.getLocation());
            stmt.setString(3, camera.getCameraUrl());
            stmt.setString(4, camera.getResolution());
            stmt.setInt(5, camera.getFps());
            stmt.setString(6, camera.getStatus());
            stmt.setInt(7, camera.getId());

            stmt.executeUpdate();
            logger.info("Camera updated successfully: " + camera.getCameraName());
        } catch (SQLException e) {
            logger.error("Error updating camera", e);
            throw new Exception("Failed to update camera in database", e);
        }
    }

    /**
     * Delete a camera
     */
    public void deleteCamera(int id) throws Exception {
        String query = "DELETE FROM cameras WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("Camera deleted successfully with ID: " + id);
        } catch (SQLException e) {
            logger.error("Error deleting camera with ID: " + id, e);
            throw new Exception("Failed to delete camera from database", e);
        }
    }

    /**
     * Map ResultSet to Camera object
     */
    private Camera mapResultSetToCamera(ResultSet rs) throws SQLException {
        Camera camera = new Camera();
        camera.setId(rs.getInt("id"));
        camera.setCameraName(rs.getString("camera_name"));
        camera.setLocation(rs.getString("location"));
        camera.setCameraUrl(rs.getString("camera_url"));
        camera.setResolution(rs.getString("resolution"));
        camera.setFps(rs.getInt("fps"));
        camera.setStatus(rs.getString("status"));
        
        if (rs.getTimestamp("created_at") != null) {
            camera.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            camera.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        
        return camera;
    }
}
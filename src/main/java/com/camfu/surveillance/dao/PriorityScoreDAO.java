package com.camfu.surveillance.dao;

import com.camfu.surveillance.model.PriorityScore;
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
 * Data Access Object for Priority Score entities
 */
public class PriorityScoreDAO {
    private static final Logger logger = LoggerFactory.getLogger(PriorityScoreDAO.class);

    /**
     * Get the latest priority scores for all cameras (most recent timestamp)
     * Ordered by overall priority score in descending order
     */
    public List<PriorityScore> getLatestPriorityScores() throws Exception {
        List<PriorityScore> scores = new ArrayList<>();
        String query = "SELECT ps.*, c.camera_name FROM priority_scores ps " +
                      "JOIN cameras c ON ps.camera_id = c.id " +
                      "WHERE ps.timestamp = (SELECT MAX(timestamp) FROM priority_scores WHERE camera_id = ps.camera_id) " +
                      "ORDER BY ps.overall_priority_score DESC";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                scores.add(mapResultSetToPriorityScore(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching latest priority scores", e);
            throw new Exception("Failed to fetch priority scores from database", e);
        }

        return scores;
    }

    /**
     * Get priority scores for a specific camera
     */
    public List<PriorityScore> getPriorityScoresByCameraId(int cameraId) throws Exception {
        List<PriorityScore> scores = new ArrayList<>();
        String query = "SELECT ps.*, c.camera_name FROM priority_scores ps " +
                      "JOIN cameras c ON ps.camera_id = c.id " +
                      "WHERE ps.camera_id = ? " +
                      "ORDER BY ps.timestamp DESC LIMIT 100";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, cameraId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    scores.add(mapResultSetToPriorityScore(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching priority scores for camera: " + cameraId, e);
            throw new Exception("Failed to fetch priority scores", e);
        }

        return scores;
    }

    /**
     * Store a priority score
     */
    public void storePriorityScore(PriorityScore score) throws Exception {
        String query = "INSERT INTO priority_scores (camera_id, motion_score, crowd_density_score, " +
                      "unusual_behavior_score, time_factor, overall_priority_score) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, score.getCameraId());
            stmt.setDouble(2, score.getMotionScore());
            stmt.setDouble(3, score.getCrowdDensityScore());
            stmt.setDouble(4, score.getUnusualBehaviorScore());
            stmt.setDouble(5, score.getTimeFactor());
            stmt.setDouble(6, score.getOverallPriorityScore());

            stmt.executeUpdate();
            logger.debug("Priority score stored for camera: " + score.getCameraId());
        } catch (SQLException e) {
            logger.error("Error storing priority score", e);
            throw new Exception("Failed to store priority score in database", e);
        }
    }

    /**
     * Map ResultSet to PriorityScore object
     */
    private PriorityScore mapResultSetToPriorityScore(ResultSet rs) throws SQLException {
        PriorityScore score = new PriorityScore();
        score.setId(rs.getInt("id"));
        score.setCameraId(rs.getInt("camera_id"));
        score.setCameraName(rs.getString("camera_name"));
        score.setMotionScore(rs.getDouble("motion_score"));
        score.setCrowdDensityScore(rs.getDouble("crowd_density_score"));
        score.setUnusualBehaviorScore(rs.getDouble("unusual_behavior_score"));
        score.setTimeFactor(rs.getDouble("time_factor"));
        score.setOverallPriorityScore(rs.getDouble("overall_priority_score"));
        
        if (rs.getTimestamp("timestamp") != null) {
            score.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        }
        
        return score;
    }
}
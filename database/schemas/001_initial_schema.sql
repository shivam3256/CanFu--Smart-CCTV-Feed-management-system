-- CamFu Database Schema
-- Initial schema setup for intelligent surveillance system

-- Create database
CREATE DATABASE IF NOT EXISTS camfu_db;
USE camfu_db;

-- Cameras Table
CREATE TABLE IF NOT EXISTS cameras (
    id INT AUTO_INCREMENT PRIMARY KEY,
    camera_name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255),
    camera_url VARCHAR(1000) NOT NULL,
    resolution VARCHAR(50),
    fps INT DEFAULT 15,
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Priority Scores Table
CREATE TABLE IF NOT EXISTS priority_scores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    camera_id INT NOT NULL,
    motion_score FLOAT DEFAULT 0,
    crowd_density_score FLOAT DEFAULT 0,
    unusual_behavior_score FLOAT DEFAULT 0,
    time_factor FLOAT DEFAULT 0,
    overall_priority_score FLOAT DEFAULT 0,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (camera_id) REFERENCES cameras(id) ON DELETE CASCADE,
    INDEX idx_camera_id (camera_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_priority_score (overall_priority_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Events Table
CREATE TABLE IF NOT EXISTS events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    camera_id INT NOT NULL,
    event_type ENUM('MOTION', 'CROWD', 'UNUSUAL', 'OTHER') NOT NULL,
    event_description TEXT,
    confidence_score FLOAT DEFAULT 0,
    priority_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    clip_start_time DATETIME,
    clip_end_time DATETIME,
    clip_path VARCHAR(1000),
    clip_size_mb FLOAT DEFAULT 0,
    is_stored BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (camera_id) REFERENCES cameras(id) ON DELETE CASCADE,
    INDEX idx_camera_id (camera_id),
    INDEX idx_event_type (event_type),
    INDEX idx_priority_level (priority_level),
    INDEX idx_created_at (created_at),
    INDEX idx_is_stored (is_stored)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Feed Rankings Table
CREATE TABLE IF NOT EXISTS feed_rankings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ranking_position INT NOT NULL,
    camera_id INT NOT NULL,
    priority_score FLOAT NOT NULL,
    display_order INT NOT NULL,
    is_displayed BOOLEAN DEFAULT TRUE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (camera_id) REFERENCES cameras(id) ON DELETE CASCADE,
    INDEX idx_camera_id (camera_id),
    INDEX idx_timestamp (timestamp),
    UNIQUE KEY unique_rank_time (ranking_position, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    role ENUM('ADMIN', 'OPERATOR', 'VIEWER') DEFAULT 'OPERATOR',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit Log Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(255),
    entity_type VARCHAR(100),
    entity_id INT,
    old_value JSON,
    new_value JSON,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_entity_type (entity_type),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- System Configuration Table
CREATE TABLE IF NOT EXISTS system_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value VARCHAR(1000),
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create default indices for performance
CREATE INDEX idx_events_compound ON events(camera_id, created_at, priority_level);
CREATE INDEX idx_priority_scores_compound ON priority_scores(camera_id, timestamp, overall_priority_score);

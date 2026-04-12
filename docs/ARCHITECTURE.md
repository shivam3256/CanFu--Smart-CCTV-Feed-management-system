# CamFu System Architecture

## Overview

CamFu is a three-tier intelligent surveillance system with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│           Frontend (Dashboard & UI)                  │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP/WebSocket
┌──────────────────────▼──────────────────────────────┐
│     Java Layer (Spring Boot)                         │
│  - REST API Server                                  │
│  - WebSocket Handler                                │
│  - Dashboard Logic                                  │
│  - Feed Management                                  │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP REST
┌──────────────────────▼──────────────────────────────┐
│     Python AI Engine (Flask)                         │
│  - Priority Score Calculation                       │
│  - Video Frame Analysis                             │
│  - ML Model Integration                             │
│  - Event Detection                                  │
└──────────────────────┬──────────────────────────────┘
                       │ SQL Queries
┌──────────────────────▼──────────────────────────────┐
│     MySQL Database                                   │
│  - Camera Metadata                                  │
│  - Event Logs                                       │
│  - Priority Scores                                  │
│  - User Management                                  │
└─────────────────────────────────────────────────────┘
```

## Component Details

### 1. Java Layer (Spring Boot)

**Responsibilities:**
- REST API for CRUD operations on cameras and events
- WebSocket connections for real-time feed updates
- User authentication and authorization
- Dashboard data aggregation
- Cache management for priority scores

**Key Classes:**
- `SurveillanceSystemApplication`: Main entry point
- `CameraController`: API endpoints for camera management
- `EventController`: API endpoints for event management
- `FeedRankingController`: API endpoints for priority rankings
- `WebSocketHandler`: Real-time communication handler
- `AIEngineClient`: HTTP client for Python AI engine

**Port:** 8080
**Context Path:** `/api`

### 2. Python AI Engine (Flask)

**Responsibilities:**
- Real-time priority score calculation
- Video frame analysis using OpenCV
- ML model inference (TensorFlow/PyTorch)
- Motion detection and crowd density estimation
- Unusual behavior detection

**Key Modules:**
- `priority_calculator.py`: Main scoring logic
- `video_processor.py`: Frame extraction and preprocessing
- `models.py`: ML model loading and inference
- `event_detector.py`: Event detection algorithms

**Port:** 5000
**API Base:** `/api/v1`

### 3. MySQL Database

**Core Tables:**
- `cameras`: Camera metadata and configuration
- `priority_scores`: Real-time priority scores
- `events`: Detected events and logs
- `feed_rankings`: Current feed display rankings
- `users`: User accounts and permissions
- `audit_logs`: System activity audit trail

## Data Flow

### Feed Processing Pipeline

```
1. CCTV Camera Feed
   ↓
2. Capture Frame (15 FPS)
   ↓
3. Send to Python AI Engine
   ↓
4. Process Frame
   ├─ Motion Detection
   ├─ Crowd Analysis
   ├─ Behavior Analysis
   └─ Confidence Scoring
   ↓
5. Calculate Priority Score
   ↓
6. Store in Database
   ↓
7. Update Feed Rankings
   ↓
8. Broadcast via WebSocket to Dashboard
   ↓
9. Display Top Priority Feeds
```

## Communication Protocols

### Java ↔ Python
- **Method:** HTTP REST API
- **Format:** JSON
- **Authentication:** API Key/Token (TBD)

### Java ↔ Database
- **Method:** JDBC via JPA/Hibernate
- **Connection Pool:** HikariCP

### Java ↔ Frontend
- **Method 1:** HTTP REST API (Synchronous)
- **Method 2:** WebSocket (Real-time updates)

## Scalability Considerations

1. **Horizontal Scaling:**
   - Java instances behind load balancer
   - Python workers with task queue (Celery)
   - Database read replicas

2. **Caching:**
   - Redis for session management
   - In-memory cache for priority scores
   - Browser cache for static assets

3. **Asynchronous Processing:**
   - Event storage in background queue
   - Video clip extraction async
   - Priority score calculation batching

## Security

1. **Authentication:** JWT tokens for API access
2. **Authorization:** Role-based access control (RBAC)
3. **Data Protection:** Encrypted database credentials
4. **API Security:** CORS, rate limiting, input validation
5. **Audit Logging:** All user actions tracked

## Performance Metrics

- Target: Process 64 feeds at 15 FPS each
- Latency: <2 second priority score update
- Storage: Selective recording reduces by ~70%
- CPU Utilization: Optimized frame processing

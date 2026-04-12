# CamFu - Intelligent Priority-Based Surveillance System

An AI-driven intelligent surveillance system that dynamically prioritizes camera feeds based on activity level and calculated risk scores, reducing monitoring fatigue and storage costs.

## Project Overview

CamFu transforms traditional passive surveillance into an intelligent, AI-assisted monitoring process by:
- Real-time priority-based feed ranking
- Displaying only important feeds
- Storing only meaningful event clips
- Reducing monitoring fatigue and storage costs

## Architecture

### Core Components

1. **JavaFX Desktop Application** - Complete UI & Core Logic
   - Real-time feed ranking display
   - Live monitoring dashboard with grid layout
   - Camera management interface
   - Priority table view
   - Settings and preferences
   - Direct database connectivity

2. **Python AI Engine** (Background Service)
   - Flask-based REST API
   - Real-time priority score calculation
   - Video frame analysis using OpenCV
   - ML model inference (TensorFlow/PyTorch)
   - Motion detection and crowd estimation
   - Unusual behavior detection

3. **MySQL Database** - Data Persistence
   - Camera metadata storage
   - Event logs and timestamps
   - Priority score records
   - User and configuration management

## Project Structure

```
CamFu/
├── java/                                    # JavaFX Desktop Application
│   ├── src/main/java/com/camfu/surveillance/
│   │   ├── ui/                              # JavaFX UI Components
│   │   │   ├── MainWindow.java
│   │   │   ├── FeedGridPanel.java
│   │   │   ├── PriorityTablePanel.java
│   │   │   ├── AddCameraDialog.java
│   │   │   ├── ManageCamerasWindow.java
│   │   │   └── PreferencesWindow.java
│   │   ├── service/                         # Business Logic
│   │   │   ├── AIEngineService.java
│   │   │   └── CameraService.java
│   │   ├── model/                           # Data Models
│   │   │   ├── Camera.java
│   │   │   └── PriorityScore.java
│   │   ├── dao/                             # Database Access
│   │   │   ├── CameraDAO.java
│   │   │   └── PriorityScoreDAO.java
│   │   ├── util/                            # Utilities
│   │   │   ├── DatabaseConnectionPool.java
│   │   │   └── ConfigLoader.java
│   │   └── CamFuApplication.java            # Main Entry Point
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── logback.xml
│   │   └── styles/application.css
│   └── src/test/java/
├── python/                                  # Python AI Engine (Background Service)
│   ├── src/
│   │   └── main.py                          # Flask API Entry Point
│   ├── config/
│   │   └── config.yaml
│   └── requirements.txt
├── database/                                # Database Setup
│   ├── schemas/
│   │   └── 001_initial_schema.sql
│   └── migrations/
├── config/                                  # Configuration Files
│   └── application.properties
├── docs/                                    # Documentation
│   └── ARCHITECTURE.md
├── pom.xml                                  # Maven Configuration
└── README.md
```

## Getting Started

### Prerequisites
- Java 11+
- Python 3.8+
- MySQL 5.7+
- Maven 3.6+
- Git

### Setup Instructions

#### 1. Database Setup
```bash
# Create MySQL database
mysql -u root -p
> CREATE DATABASE camfu_db;
> USE camfu_db;
> SOURCE database/schemas/001_initial_schema.sql;
```

#### 2. Python AI Engine Setup
```bash
cd python

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run the AI engine in background
python src/main.py &
```

#### 3. Build JavaFX Desktop Application
```bash
# Navigate to project root
cd ..

# Build with Maven
mvn clean install

# Run the application
mvn javafx:run
```

Or package as executable JAR:
```bash
mvn package
java -jar target/CamFu.jar
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Desktop Application | Java 11, JavaFX 21 |
| UI Framework | JavaFX (Modern Desktop UI) |
| AI Engine | Python 3.8+, Flask |
| AI/ML | TensorFlow, PyTorch, OpenCV, scikit-learn |
| Database | MySQL 8.0, JDBC |
| Build Tool | Maven 3.6+ |
| Database Driver | mysql-connector-java 8.0.33 |
| Logging | SLF4J, Logback |

## Target Market

- Shopping malls
- Airports
- Manufacturing plants
- Warehouses
- Educational campuses

## Competitive Advantages

- **Intelligent Prioritization**: AI-driven dynamic priority scoring
- **Cost Reduction**: Reduced storage costs with selective recording
- **Efficiency**: Minimized human monitoring fatigue
- **Scalability**: Supports monitoring of multiple camera feeds

## Key Features

### Desktop Application
- **Live Feed Grid**: Display up to 12 camera feeds simultaneously
- **Priority Rankings**: Real-time feed ranking based on activity
- **Camera Management**: Add, edit, delete cameras easily
- **Event Logging**: Track all surveillance events
- **Preferences**: Configurable settings for database and AI engine
- **Responsive UI**: Dark theme, multi-window support

### AI Engine Integration
- **Real-Time Processing**: Continuous frame analysis at 15 FPS
- **Intelligent Scoring**: Motion + Crowd + Behavior + Time factors
- **Event Detection**: Unusual activity alerts
- **Selective Recording**: Store only meaningful clips

## Building & Deployment

### Build for Distribution
```bash
mvn clean package -DskipTests
```

### Create Executable (Windows)
```bash
# Package with bundler
mvn clean package -Djavafx.platform=win
```

### Create Docker Image (Optional)
```bash
# Dockerfile to be added
docker build -t camfu:latest .
docker run -p 5000:5000 camfu:latest
```

## Configuration Files

- `config/application.properties`: Database connection, AI engine settings
- `python/config/config.yaml`: Python service configuration
- `java/src/main/resources/application.yml`: JavaFX app settings

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running: `mysql -u root -p`
- Check credentials in `config/application.properties`
- Ensure `camfu_db` database exists

### AI Engine Not Starting
- Verify Python 3.8+ is installed: `python --version`
- Check dependencies: `pip list`
- Look at logs in `logs/ai_engine.log`

### JavaFX Module Issues
- Ensure JavaFX SDK is properly configured in Maven
- Check IDE JavaFX library paths in project settings

## Performance Tips

1. Limit camera feeds to 64 for optimal performance
2. Adjust frame rate based on system specs
3. Enable database connection pooling
4. Use SSD for event clip storage

## Contributing

Guidelines for contributing to the project will be added.

## License

To be determined.

## Contact

For more information, contact the development team.

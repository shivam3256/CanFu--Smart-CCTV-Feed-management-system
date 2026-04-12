@echo off
REM CamFu Low-Latency RTSP Surveillance System - Launcher
REM Properly configured for JavaFX 21 module system

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

REM Set paths
set JAVAFX_PATH=C:\Users\%USERNAME%\.m2\repository\org\openjfx
set JAR_FILE=%SCRIPT_DIR%target\CamFu-lowlatency.jar

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo Please run: mvn clean compile assembly:single -DskipTests
    pause
    exit /b 1
)

REM Check if JavaFX path exists
if not exist "%JAVAFX_PATH%" (
    echo Error: JavaFX modules not found at %JAVAFX_PATH%
    echo Please ensure Maven has downloaded JavaFX 21 dependencies
    pause
    exit /b 1
)

echo.
echo ======================================
echo CamFu - Intelligent Surveillance
echo Low-Latency RTSP Streaming
echo ======================================
echo.
echo Launching with optimizations:
echo  - RTSP timeout: 8 seconds
echo  - Frame encoding: Quality 5 (fast)
echo  - UI refresh: 100ms (10 FPS)
echo.

REM Build classpath with all JavaFX and MySQL JARs
set CLASSPATH=%JAR_FILE%
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-base\21\javafx-base-21.jar
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-controls\21\javafx-controls-21.jar
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-fxml\21\javafx-fxml-21.jar
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-graphics\21\javafx-graphics-21.jar
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-swing\21\javafx-swing-21.jar
set CLASSPATH=!CLASSPATH!;%JAVAFX_PATH%\javafx-web\21\javafx-web-21.jar
set CLASSPATH=!CLASSPATH!;C:\Users\%USERNAME%\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar

REM Launch with classpath approach (more reliable for fat JAR)
java -cp "%CLASSPATH%" com.camfu.surveillance.CamFuApplication

if errorlevel 1 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)

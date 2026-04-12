@echo off
REM ============================================================================
REM   CamFu Launcher - With VLC Integration for Professional Streaming
REM ============================================================================
REM   
REM   This launcher starts CamFu with VLC for professional RTMP/HLS streaming.
REM   
REM   PREREQUISITES:
REM   1. Java 11+ must be installed
REM   2. VLC must be installed from https://www.videolan.org/vlc/
REM   
REM ============================================================================

setlocal enabledelayedexpansion

REM Detect Java
for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr "version"') do set JAVA_VERSION=%%i
if "!JAVA_VERSION!"=="" (
    echo ERROR: Java not found. Please install Java 11 or later.
    echo Download from: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo   CamFu - Professional Streaming Camera Utility
echo ============================================================================
echo.
echo Java Found: !JAVA_VERSION!
echo.

REM Launch CamFu
echo Starting CamFu...
echo.
cd /d "%~dp0"
java -jar target/CamFu.jar

REM If runs in background
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to start CamFu. Error code: %errorlevel%
    pause
    exit /b %errorlevel%
)

pause
exit /b 0

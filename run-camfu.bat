@echo off
REM CamFu - Intelligent Surveillance Desktop Application Launcher
REM This batch file launches the CamFu application with JavaFX modules

setlocal enabledelayedexpansion

REM Get the script directory
set SCRIPT_DIR=%~dp0

REM Set Maven repository path
set M2_REPO=%USERPROFILE%\.m2\repository\org\openjfx

REM Build module path
set MODULE_PATH=%M2_REPO%\javafx-base\21;%M2_REPO%\javafx-controls\21;%M2_REPO%\javafx-fxml\21;%M2_REPO%\javafx-graphics\21;%M2_REPO%\javafx-swing\21

REM Check if JAR exists
if not exist "%SCRIPT_DIR%target\CamFu.jar" (
    echo Error: CamFu.jar not found in %SCRIPT_DIR%target\
    echo Please build the project first using: mvn clean compile assembly:single
    pause
    exit /b 1
)

REM Launch the application
echo Starting CamFu Application...
echo.
cd /d "%SCRIPT_DIR%"
java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -jar target\CamFu.jar

REM If app closes, show the window for a moment
if %errorlevel% neq 0 (
    echo.
    echo Application closed with error code: %errorlevel%
    pause
)

endlocal

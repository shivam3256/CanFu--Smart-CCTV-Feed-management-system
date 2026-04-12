@echo off
setlocal enabledelayedexpansion

REM Get the directory where this batch file is located
set SCRIPT_DIR=%~dp0

REM Set the JAR path
set JAR_PATH=%SCRIPT_DIR%target\CamFu.jar

REM Find the local Maven repository
for /d %%F in ("%USERPROFILE%\.m2\repository\org\openjfx\javafx-*\21") do (
    if exist "%%F\javafx-*.jar" (
        set JAVAFX_REPO=%%F
        goto :found_javafx
    )
)

:found_javafx
if defined JAVAFX_REPO (
    echo Found JavaFX at: !JAVAFX_REPO!
    REM Build module path for JavaFX
    set MODULE_PATH=!JAVAFX_REPO!;!JAVAFX_REPO!\..\javafx-controls\21;!JAVAFX_REPO!\..\javafx-fxml\21;!JAVAFX_REPO!\..\javafx-graphics\21;!JAVAFX_REPO!\..\javafx-swing\21
    
    echo Running CamFu with JavaFX modules...
    java --module-path !MODULE_PATH! --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing -jar "!JAR_PATH!"
) else (
    echo JavaFX not found in Maven repository
    echo Running CamFu without explicit module path (will fail if JavaFX not bundled)...
    java -jar "!JAR_PATH!"
)

pause

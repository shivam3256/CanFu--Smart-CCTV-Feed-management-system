@echo off
setlocal EnableDelayedExpansion

:: ============================================================
::  CamFu - Intelligent Surveillance System
::  Windows Batch Launcher
:: ============================================================

title CamFu Launcher

:: ── Paths ────────────────────────────────────────────────────
set "PROJECT_DIR=%~dp0"
set "JAR=%PROJECT_DIR%target\CamFu.jar"
set "M2=%USERPROFILE%\.m2\repository\org\openjfx"
set "MAIN_CLASS=com.camfu.surveillance.CamFuApplication"

:: ── Console header ───────────────────────────────────────────
echo.
echo  =====================================================
echo   CamFu ^| Intelligent Surveillance System
echo  =====================================================
echo.

:: ── 1. Check Java ────────────────────────────────────────────
java -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Java not found. Please install Java 21+.
    echo          Download: https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%v"
)
echo  [OK]  Java detected: !JAVA_VER!

:: ── 2. Check JAR ─────────────────────────────────────────────
if not exist "%JAR%" (
    echo.
    echo  [WARN] CamFu.jar not found. Building now...
    echo         This may take 2-3 minutes on first build.
    echo.
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo  [ERROR] Maven ^(mvn^) not found in PATH.
        echo          Please run:  mvn package -DskipTests
        echo          Then re-launch this file.
        echo.
        pause
        exit /b 1
    )
    cd /d "%PROJECT_DIR%"
    call mvn package -DskipTests -q
    if errorlevel 1 (
        echo  [ERROR] Build failed. Check Maven output above.
        echo.
        pause
        exit /b 1
    )
    echo  [OK]  Build complete.
)

echo  [OK]  CamFu.jar found.

:: ── 3. Check JavaFX modules ──────────────────────────────────
set "FX_BASE=%M2%\javafx-base\21\javafx-base-21-win.jar"
if not exist "%FX_BASE%" (
    echo.
    echo  [WARN] JavaFX modules not found in local Maven repo.
    echo         Running 'mvn dependency:resolve' to download them...
    cd /d "%PROJECT_DIR%"
    call mvn dependency:resolve -q
    echo  [OK]  Dependencies resolved.
)

:: ── 4. Build JavaFX module path ──────────────────────────────
set "MODULE_PATH=%M2%\javafx-base\21\javafx-base-21-win.jar"
set "MODULE_PATH=%MODULE_PATH%;%M2%\javafx-controls\21\javafx-controls-21-win.jar"
set "MODULE_PATH=%MODULE_PATH%;%M2%\javafx-fxml\21\javafx-fxml-21-win.jar"
set "MODULE_PATH=%MODULE_PATH%;%M2%\javafx-graphics\21\javafx-graphics-21-win.jar"
set "MODULE_PATH=%MODULE_PATH%;%M2%\javafx-swing\21\javafx-swing-21-win.jar"

:: ── 5. Fast compile latest changes ────────────────────────────
echo  [OK]  Compiling latest changes (fast)...
cd /d "%PROJECT_DIR%"
call mvn compiler:compile resources:resources -q

:: ── 6. Launch ────────────────────────────────────────────────
echo  [OK]  Launching CamFu...
echo.

cd /d "%PROJECT_DIR%"

java ^
    -Djava.awt.headless=false ^
    -Dprism.order=d3d ^
    -Dprism.vsync=true ^
    --module-path="%MODULE_PATH%" ^
    --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.graphics,javafx.swing ^
    --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED ^
    --enable-native-access=ALL-UNNAMED,javafx.graphics ^
    -cp "%PROJECT_DIR%target\classes;%JAR%" ^
    %MAIN_CLASS%

:: ── 7. Exit message ──────────────────────────────────────────
echo.
echo  CamFu exited.
echo  Press any key to close this window...
pause >nul
endlocal

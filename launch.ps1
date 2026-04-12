#!/usr/bin/env powershell
# CamFu Low-Latency RTSP Surveillance System - PowerShell Launcher
# Properly configured for JavaFX 21 module system

param(
    [switch]$Verbose = $false
)

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# JavaFX path
$javafxPath = "$env:USERPROFILE\.m2\repository\org\openjfx"
$jarFile = "$scriptDir\target\CamFu-lowlatency.jar"

# Fallback to standard location if above doesn't exist
if (-not (Test-Path $javafxPath)) {
    $jarFile = "$scriptDir\target\CamFu.jar"
}

# Validate JAR exists
if (-not (Test-Path $jarFile)) {
    Write-Host "Error: JAR file not found at $jarFile" -ForegroundColor Red
    Write-Host "Please run: mvn clean compile package -DskipTests" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Validate JavaFX path exists
if (-not (Test-Path $javafxPath)) {
    Write-Host "Error: JavaFX modules not found at $javafxPath" -ForegroundColor Red
    Write-Host "Please ensure Maven has downloaded JavaFX 21 dependencies" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "`n======================================" -ForegroundColor Cyan
Write-Host "CamFu - Intelligent Surveillance" -ForegroundColor Cyan
Write-Host "Low-Latency RTSP Streaming" -ForegroundColor Cyan
Write-Host "======================================`n" -ForegroundColor Cyan
Write-Host "Launching with optimizations:" -ForegroundColor Green
Write-Host "  - RTSP timeout: 8 seconds (was 30s)" -ForegroundColor Gray
Write-Host "  - Frame encoding: Quality 5 (fast JPEG)" -ForegroundColor Gray
Write-Host "  - UI refresh: 100ms (10 FPS, was 2s)" -ForegroundColor Gray
Write-Host "  - Expected latency: <1 second per frame`n" -ForegroundColor Gray

if ($Verbose) {
    Write-Host "JAR: $jarFile" -ForegroundColor DarkGray
    Write-Host "JavaFX Path: $javafxPath`n" -ForegroundColor DarkGray
}

# Build classpath with all JavaFX and MySQL JARs
$bcp = @(
    $jarFile,
    "$javafxPath\javafx-base\21\javafx-base-21.jar",
    "$javafxPath\javafx-controls\21\javafx-controls-21.jar",
    "$javafxPath\javafx-fxml\21\javafx-fxml-21.jar",
    "$javafxPath\javafx-graphics\21\javafx-graphics-21.jar",
    "$javafxPath\javafx-swing\21\javafx-swing-21.jar",
    "$javafxPath\javafx-web\21\javafx-web-21.jar",
    "$env:USERPROFILE\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar"
) -join ";"

Write-Host "Executing: java -cp <classpath> com.camfu.surveillance.CamFuApplication`n" -ForegroundColor DarkGray

& java -cp "$bcp" com.camfu.surveillance.CamFuApplication

$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    Write-Host "`nApplication exited with error code: $exitCode" -ForegroundColor Red
    Read-Host "Press Enter to exit"
}

# CamFu Launcher Script
# Launches CamFu with VLCJ streaming support

$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $projectDir "target\CamFu.jar"
$m2Repo = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"

# Check if JAR exists
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: CamFu.jar not found at $jarPath"
    Write-Host "Please run: mvn package -DskipTests"
    exit 1
}

# Construct module path with JavaFX modules
$modulePath = @(
    "$m2Repo\javafx-base\21\javafx-base-21-win.jar",
    "$m2Repo\javafx-controls\21\javafx-controls-21-win.jar",
    "$m2Repo\javafx-fxml\21\javafx-fxml-21-win.jar",
    "$m2Repo\javafx-graphics\21\javafx-graphics-21-win.jar",
    "$m2Repo\javafx-swing\21\javafx-swing-21-win.jar"
) -join ";"

Write-Host "CamFu - Intelligent Surveillance System"
Write-Host "======================================="
Write-Host "Launching with VLCJ streaming support..."
Write-Host ""

# Launch application with proper JavaFX module configuration
& java `
    "-Djava.awt.headless=false" `
    "-Dprism.order=d3d" `
    "--module-path=$modulePath" `
    "--add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.graphics,javafx.swing" `
    "-cp" `
    $jarPath `
    "com.camfu.surveillance.CamFuApplication"

Write-Host "Application exited"

# CamFu Feed Diagnostic Script
Write-Host "=== CamFu Feed Display Diagnostic ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check if MySQL is running
Write-Host "[1] Checking MySQL Connection..." -ForegroundColor Yellow
try {
    $result = mysql -h localhost -u root -pShivam@9797 -e "SELECT 1" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "    ✓ MySQL is running and accessible" -ForegroundColor Green
    } else {
        Write-Host "    ✗ MySQL connection failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "    ✗ MySQL error: $_" -ForegroundColor Red
    exit 1
}

# 2. Check cameras in database
Write-Host ""
Write-Host "[2] Checking Cameras in Database..." -ForegroundColor Yellow
$cameras = @()
$output = mysql -h localhost -u root -pShivam@9797 camfu_db -N -e "SELECT camera_id, camera_name, camera_url, status FROM cameras;" 2>&1

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrEmpty($output)) {
    Write-Host "    ✗ No cameras found in database or database query failed" -ForegroundColor Red
    Write-Host "    Please add a camera first using the CamFu application" -ForegroundColor Yellow
    exit 1
}

$lines = $output -split "`n" | Where-Object { $_ -and $_.Trim() }
Write-Host "    ✓ Found $($lines.Count) camera(s)" -ForegroundColor Green
Write-Host ""

# Parse camera data
foreach ($line in $lines) {
    $parts = $line -split "`t" | Where-Object { $_ }
    if ($parts.Count -ge 3) {
        $camId = $parts[0]
        $camName = $parts[1]
        $camUrl = $parts[2]
        $status = if ($parts.Count -gt 3) { $parts[3] } else { "UNKNOWN" }
        
        Write-Host "Camera #$camId - $camName" -ForegroundColor Cyan
        Write-Host "    URL: $camUrl"
        Write-Host "    Status: $status"
        
        # Test stream URL accessibility
        Write-Host "    Testing stream URL..." -NoNewline
        try {
            $web = New-Object System.Net.WebClient
            $web.DownloadString($camUrl) | Out-Null
            Write-Host " ✓" -ForegroundColor Green
        } catch {
            Write-Host " ✗ ($($_.Exception.Message))" -ForegroundColor Red
        }
        
        # Test frame endpoint
        $frameUrl = if ($camUrl -match "/video") {
            $camUrl -replace "/video", "/shot.jpg"
        } else {
            if ($camUrl -notmatch "/$") { $camUrl += "/" }
            $camUrl + "shot.jpg"
        }
        
        Write-Host "    Frame endpoint: $frameUrl"
        Write-Host "    Testing frame endpoint..." -NoNewline
        try {
            $web = New-Object System.Net.WebClient
            $response = $web.DownloadData($frameUrl)
            Write-Host " ✓ ($(($response).Count) bytes)" -ForegroundColor Green
        } catch {
            Write-Host " ✗ ($($_.Exception.Message))" -ForegroundColor Red
            Write-Host "    ⚠ Frame endpoint not accessible. VLC might be using a different format." -ForegroundColor Yellow
            Write-Host "    Please verify the exact streaming format your camera supports." -ForegroundColor Yellow
        }
        Write-Host ""
    }
}

Write-Host "=== Diagnostic Complete ===" -ForegroundColor Cyan

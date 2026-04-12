#!/bin/bash
# ============================================================================
#   CamFu Launcher - With VLC Integration for Professional Streaming
# ============================================================================
#   
#   This launcher starts CamFu with VLC for professional RTMP/HLS streaming.
#   
#   PREREQUISITES:
#   1. Java 11+ must be installed
#   2. VLC must be installed:
#      Ubuntu/Debian: sudo apt-get install vlc
#      macOS: brew install vlc
#      Or download from: https://www.videolan.org/vlc/
#   
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check Java
if ! java -version 2>&1 | grep -q "version"; then
    echo "ERROR: Java not found. Please install Java 11 or later."
    echo "Download from: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep version | head -1)

echo ""
echo "============================================================================"
echo "  CamFu - Professional Streaming Camera Utility"
echo "============================================================================"
echo ""
echo "Java Found: $JAVA_VERSION"
echo ""

# Launch CamFu
echo "Starting CamFu..."
echo ""
java -jar target/CamFu.jar

exit $?

# CamFu Quick Start Guide

## Installation & Launch

### Windows Users
1. Double-click: **RUN_CAMFU_WITH_VLC.bat**
   - Requires: Java 11+, VLC installed

### macOS/Linux Users
1. Open terminal and run:
   ```bash
   chmod +x run_camfu_with_vlc.sh
   ./run_camfu_with_vlc.sh
   ```
   - Requires: Java 11+, VLC installed

---

## Minimal 5-Minute Setup

### Step 1: Install Prerequisites
- **Java 11+**: https://www.oracle.com/java/technologies/downloads/
- **VLC**: https://www.videolan.org/vlc/

### Step 2: Launch CamFu
```bash
# Windows (Command Prompt)
RUN_CAMFU_WITH_VLC.bat

# macOS/Linux (Terminal)
./run_camfu_with_vlc.sh
```

### Step 3: Stream with VLC (in new terminal)
```bash
vlc output.mp4 --sout "#http{mux=ts,dst=:8080/stream.m3u8}" --loop
```

### Step 4: Watch in Browser
Open: **http://localhost:8080/stream.m3u8**

---

## Common Commands

### Twitch Streaming
```bash
vlc input.mp4 --sout "#rtmp{dst=live-sjc.twitch.tv/app/YOUR_STREAM_KEY}" --loop
```

### Local Network Camera
```bash
vlc output.mp4 --sout "#http{mux=m3u8,dst=:9000/stream.m3u8}"
```
Access from any computer: `http://<your_ip>:9000/stream.m3u8`

### High-Quality Stream
```bash
vlc input.mp4 --sout ="#transcode{vcodec=h264,vb=8000,acodec=aac,ab=256}:http{mux=ts,dst=:8080/stream.m3u8}" --loop
```

---

## Detailed Documentation
See [VLC_STREAMING_GUIDE.md](VLC_STREAMING_GUIDE.md) for advanced options, troubleshooting, and best practices.

---

## Project Structure
```
CamFu/
├── src/                          # Source code
│   ├── main/java/...             # Java classes
│   └── test/java/...             # Tests
├── target/
│   └── CamFu.jar                 # Compiled JAR (948 MB)
├── pom.xml                       # Maven configuration
├── RUN_CAMFU_WITH_VLC.bat        # Windows launcher
├── run_camfu_with_vlc.sh         # Unix/Linux launcher
├── VLC_STREAMING_GUIDE.md        # Complete guide
└── QUICK_START.md                # This file
```

---

## Troubleshooting

### Java Not Found
```
Windows: Download Java from https://www.oracle.com/java/technologies/downloads/
macOS: brew install java11
Linux: sudo apt-get install openjdk-11-jdk
```

### VLC Not Found
```
Windows: Download from https://www.videolan.org/vlc/
macOS: brew install vlc
Linux: sudo apt-get install vlc
```

### Port Already in Use
Change port in VLC command:
```bash
vlc input.mp4 --sout "#http{mux=ts,dst=:8888/stream.m3u8}"
# Then access: http://localhost:8888/stream.m3u8
```

---

## Support & Resources
- **CamFu Source**: Java/Spring Boot application
- **VLC Docs**: https://www.videolan.org/doc/
- **H.264 Encoding**: https://en.wikipedia.org/wiki/Advanced_Video_Coding
- **HTTP Live Streaming**: https://tools.ietf.org/html/rfc8216

---

**Ready to stream!** 🚀

# CamFu + VLC Professional Streaming Guide

## Overview
CamFu integrates with VLC (VideoLAN Client) to provide professional RTMP/HLS streaming capabilities. This guide covers installation, configuration, and best practices.

---

## Part 1: Prerequisites Installation

### 1.1 Java Installation
CamFu requires Java 11 or later:

**Windows:**
1. Download Java from: https://www.oracle.com/java/technologies/downloads/
2. Run the installer and follow the wizard
3. Verify: Open Command Prompt and type `java -version`

**macOS:**
```bash
brew install java11
# or use:
brew install openjdk@11
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install openjdk-11-jdk
```

### 1.2 VLC Installation

**Windows:**
1. Download VLC from: https://www.videolan.org/vlc/
2. Run the installer with default options
3. VLC will be installed in `Program Files\VideoLAN\VLC`

**macOS:**
```bash
brew install vlc
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install vlc vlc-plugin-base
```

---

## Part 2: VLC Command-Line Streaming

### 2.1 Understanding VLC Streaming

VLC can stream video via command-line, making it perfect for integration with CamFu output:

```bash
vlc <input> --sout="#<profile>" <options>
```

### 2.2 Common VLC Streaming Profiles

#### RTMP (Real-Time Messaging Protocol)
Ideal for platforms like YouTube, Facebook Live, Twitch:

```bash
vlc input.mp4 --sout "#rtp{dst=127.0.0.1,port=5005,mux=ts}" --loop
```

**For Twitch:**
```bash
vlc input.mp4 --sout "#rtmp{dst=live-sjc.twitch.tv/app/<STREAM_KEY>}" --loop
```

#### RTSP (Real-Time Streaming Protocol)
For network cameras and proxies:

```bash
vlc --rtsp-tcp rtsp://camera.ip:554/stream --sout "#display" --fullscreen
```

#### HLS (HTTP Live Streaming)
For web delivery and mobile:

```bash
vlc input.mp4 --sout "#http{mux=ts,dst=:8080/stream.m3u8}" --loop
```

#### UDP/RTP Multicast
For local network streaming:

```bash
vlc input.mp4 --sout "#rtp{dst=239.255.1.1,port=5004,mux=ts}" --loop
```

---

## Part 3: Advanced Streaming Scenarios

### 3.1 Transcoding While Streaming

Stream video at specific bitrate/resolution:

```bash
vlc input.mp4 --sout=#transcode{vcodec=h264,vb=5000,acodec=mpga,ab=192}:http{mux=ts,dst=:8080/stream.m3u8} --loop
```

**Parameters:**
- `vcodec=h264`: Video codec (h264, h265, etc.)
- `vb=5000`: Video bitrate (5 Mbps)
- `acodec=mpga`: Audio codec (mpga, aac, etc.)
- `ab=192`: Audio bitrate (192 kbps)

### 3.2 Multiple Outputs (Streaming + Local Recording)

Stream to server AND save locally:

```bash
vlc input.mp4 --sout="#duplicate{dst=http{mux=ts,dst=:8080/stream.m3u8},dst=file{dst=output.mp4}}" --loop
```

### 3.3 Encoding for Different Quality Tiers

Stream at 720p and 1080p simultaneously:

```bash
vlc input.mp4 --sout "#duplicate{\
  dst=transcode{vcodec=h264,vb=2500,acodec=aac,ab=128}:http{mux=m3u8,dst=:8080/720p.m3u8},\
  dst=transcode{vcodec=h264,vb=5000,acodec=aac,ab=192}:http{mux=m3u8,dst=:8080/1080p.m3u8}\
}"
```

---

## Part 4: CamFu Integration with VLC

### 4.1 Basic Integration

CamFu captures video, VLC streams it:

```bash
# Start CamFu (outputs video file or stream)
java -jar target/CamFu.jar

# In another terminal, stream with VLC
vlc <camfu_output> --sout "#http{mux=ts,dst=:8080/stream.m3u8}"
```

### 4.2 Direct Piping (Unix/Linux/macOS)

Stream output directly without intermediate file:

```bash
java -jar target/CamFu.jar | vlc - --sout "#http{mux=ts,dst=:8080/stream.m3u8}"
```

### 4.3 Windows PowerShell Integration

```powershell
# Start both processes in parallel
$camfu = Start-Process -FilePath "java" -ArgumentList "-jar", "target/CamFu.jar" -PassThru
$vlc = Start-Process -FilePath "vlc" -ArgumentList "output.mp4", "--sout=#http{mux=ts,dst=:8080/stream.m3u8}" -PassThru

# Wait for both
Wait-Process -Id $camfu.Id, $vlc.Id
```

---

## Part 5: Browser Playback

### 5.1 HLS Playback (Recommended)

Create `index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
</head>
<body>
    <video id="video" width="800" height="600" controls></video>
    <script>
        if (Hls.isSupported()) {
            var video = document.getElementById('video');
            var hls = new Hls();
            hls.loadSource('http://localhost:8080/stream.m3u8');
            hls.attachMedia(video);
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = 'http://localhost:8080/stream.m3u8';
        }
    </script>
</body>
</html>
```

Open in browser: `http://localhost:8080/index.html`

### 5.2 RTMP Playback

Use player supporting RTMP (e.g., OBS, FFmpeg):

```bash
ffplay rtmp://localhost:1935/live/stream
```

---

## Part 6: Best Practices

### 6.1 Performance Optimization
- **Bitrate**: Start at 5 Mbps for 1080p, adjust down for bandwidth constraints
- **Resolution**: Scale to 720p or 480p for network streaming
- **FPS**: Maintain 30 FPS for smooth playback
- **Codec**: Use H.264 for compatibility, H.265 for better compression

### 6.2 Network Architecture
```
Camera/Input
    ↓
CamFu (Processing)
    ↓
VLC (Streaming)
    ↓
CDN/Server/Client (Playback)
```

### 6.3 Troubleshooting

**VLC Not Found:**
- Verify VLC is in PATH: `which vlc` or `vlc --version`
- On Windows, add to PATH or use full path: `"C:\Program Files\VideoLAN\VLC\vlc.exe"`

**Streaming Fails:**
- Check port availability: `netstat -tln | grep 8080`
- Verify firewall allows port
- Check VLC logs: Enable with `--verbose 2`

**High Latency:**
- Reduce buffer size: `--network-caching=300`
- Use UDP instead of TCP
- Lower resolution/bitrate

---

## Part 7: Example Use Cases

### Streaming to Twitch

1. Get Twitch stream key from dashboard
2. Run:
```bash
vlc input.mp4 --sout "#rtmp{dst=live-sjc.twitch.tv/app/<YOUR_STREAM_KEY>}" --loop
```

### Security Camera Network Stream

1. Run CamFu (captures camera)
2. Stream locally:
```bash
vlc <output> --sout "#http{mux=m3u8,dst=:9000/stream.m3u8}"
```

3. View from any browser on network:
```
http://<your_ip>:9000/stream.m3u8
```

### Recording + Live Stream

```bash
vlc input.mp4 --sout "#duplicate{\
  dst=http{mux=ts,dst=:8080/stream.m3u8},\
  dst=file{dst=archive_$(date +%s).mp4}\
}"
```

---

## Summary

**Three-Step Streaming:**
1. Install Java 11+ and VLC
2. Run CamFu: `java -jar target/CamFu.jar`
3. Stream with VLC: `vlc <output> --sout "#http{mux=ts,dst=:8080/stream.m3u8}"`
4. Watch in browser: `http://localhost:8080/stream.m3u8`

For more VLC options: `vlc --help`

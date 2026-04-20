import cv2
import requests
import numpy as np
import time

url = "http://192.168.1.46:8080/video"
print(f"Connecting to {url} using requests...")

bg_subtractor = cv2.createBackgroundSubtractorMOG2(history=500, varThreshold=16, detectShadows=True)

try:
    res = requests.get(url, stream=True, timeout=5)
    bytes_data = bytes()
    frames_processed = 0
    for chunk in res.iter_content(chunk_size=4096):
        bytes_data += chunk
        a = bytes_data.find(b'\xff\xd8')
        b = bytes_data.find(b'\xff\xd9')
        if a != -1 and b != -1:
            if a < b:
                jpg = bytes_data[a:b+2]
                bytes_data = bytes_data[b+2:]
                frame = cv2.imdecode(np.frombuffer(jpg, dtype=np.uint8), cv2.IMREAD_COLOR)
                if frame is not None:
                    frames_processed += 1
                    frame = cv2.resize(frame, (320, 240))
                    fg_mask = bg_subtractor.apply(frame)
                    _, fg_mask = cv2.threshold(fg_mask, 254, 255, cv2.THRESH_BINARY)
                    non_zero = cv2.countNonZero(fg_mask)
                    motion = min(1.0, (non_zero / (320*240)) * 10)
                    print(f"Frame {frames_processed}: motion={motion:.4f}, non_zero={non_zero}")
                    if frames_processed >= 15:
                        break
            else:
                bytes_data = bytes_data[b+2:]
except Exception as e:
    print(f"Error: {e}")

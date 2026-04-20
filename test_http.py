import requests
import cv2
import numpy as np

url = "http://192.168.1.46:8080/video"
print(f"Connecting to {url} using requests...")

try:
    res = requests.get(url, stream=True, timeout=5)
    print(f"Status: {res.status_code}")
    bytes_data = bytes()
    frames_read = 0
    for chunk in res.iter_content(chunk_size=1024):
        bytes_data += chunk
        a = bytes_data.find(b'\xff\xd8')
        b = bytes_data.find(b'\xff\xd9')
        if a != -1 and b != -1:
            jpg = bytes_data[a:b+2]
            bytes_data = bytes_data[b+2:]
            frame = cv2.imdecode(np.frombuffer(jpg, dtype=np.uint8), cv2.IMREAD_COLOR)
            if frame is not None:
                frames_read += 1
                if frames_read == 1:
                    print(f"Successfully read first frame via HTTP streaming! Shape: {frame.shape}")
                    break
except Exception as e:
    print(f"Error: {e}")

import cv2
import requests
import numpy as np
import time

# Replace with your actual camera URL if it's different
url = "http://192.168.1.46:8080/video"
print(f"Connecting to {url}...")

# This is the exact same motion detector used in the AI Engine
bg_subtractor = cv2.createBackgroundSubtractorMOG2(history=500, varThreshold=16, detectShadows=True)

try:
    res = requests.get(url, stream=True, timeout=5)
    bytes_data = bytes()
    
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
                    # 1. Resize just like the engine does
                    small_frame = cv2.resize(frame, (320, 240))
                    
                    # 2. Apply the background subtractor
                    fg_mask = bg_subtractor.apply(small_frame)
                    
                    # 3. Filter out gray shadows
                    _, fg_mask = cv2.threshold(fg_mask, 254, 255, cv2.THRESH_BINARY)
                    
                    # 4. Calculate the score
                    non_zero = cv2.countNonZero(fg_mask)
                    total_pixels = small_frame.shape[0] * small_frame.shape[1]
                    motion_ratio = non_zero / total_pixels
                    motion_score = min(1.0, motion_ratio * 10)
                    
                    # --- Visualization ---
                    # Draw the score on the original frame
                    cv2.putText(frame, f"Motion Score: {motion_score:.2f}", (10, 40), 
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                    cv2.putText(frame, f"Moving Pixels: {non_zero}", (10, 80), 
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 255), 2)
                    
                    # Show the original video and the black/white motion mask
                    cv2.imshow("Original Feed + Score", frame)
                    cv2.imshow("AI Motion Mask (White = Movement)", fg_mask)
                    
                    # Press 'q' to quit
                    if cv2.waitKey(1) & 0xFF == ord('q'):
                        break
            else:
                bytes_data = bytes_data[b+2:]

except Exception as e:
    print(f"Error: {e}")

cv2.destroyAllWindows()

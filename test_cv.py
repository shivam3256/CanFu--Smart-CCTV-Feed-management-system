import cv2
import time

url = "http://192.168.1.46:8080/video"
print(f"Connecting to {url}...")
cap = cv2.VideoCapture(url)
print(f"isOpened: {cap.isOpened()}")

if cap.isOpened():
    ret, frame = cap.read()
    print(f"Read success: {ret}")
    if ret:
        print(f"Frame shape: {frame.shape}")
cap.release()

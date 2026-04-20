import cv2
import logging
import threading
import time

logger = logging.getLogger(__name__)

class CameraStreamProcessor:
    def __init__(self, camera_id, camera_info, config):
        self.camera_id = camera_id
        self.camera_info = camera_info
        self.url = camera_info['camera_url']
        self.running = False
        self.thread = None
        
        # MOG2 for simple, robust motion detection
        self.bg_subtractor = cv2.createBackgroundSubtractorMOG2(history=500, varThreshold=16, detectShadows=True)
        
        self.motion_score = 0.0
        self.crowd_density = 0.0
        self.unusual_behavior = 0.0
        self.time_factor = 0.5
        
        # Load weights
        weights = config.get('ai_engine', {}).get('priority_weights', {})
        self.w_motion = weights.get('motion_score', 0.4)
        self.w_crowd = weights.get('crowd_density', 0.3)
        self.w_behavior = weights.get('unusual_behavior', 0.2)
        self.w_time = weights.get('time_of_day', 0.1)
        
        # Setup Video Writer
        self.video_writer = None
        self.recording_filename = ""
        import os
        self.recordings_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', 'recordings'))
        os.makedirs(self.recordings_dir, exist_ok=True)
        
    def start(self):
        if self.running:
            return
        self.running = True
        self.thread = threading.Thread(target=self._process_loop, daemon=True)
        self.thread.start()
        
    def stop(self):
        self.running = False
        if self.thread:
            self.thread.join(timeout=2.0)
            
    def _process_loop(self):
        logger.info(f"Starting frame processor for camera {self.camera_id}: {self.url}")
        
        target_fps = 5
        frame_time = 1.0 / target_fps
        first_frame_read = False
        
        while self.running:
            # For network robustness on IP webcams, we use requests to manually parse the MJPEG boundary
            # because OpenCV's FFMPEG backend often hangs or drops self-signed HTTPS or chunked HTTP
            try:
                import requests
                import numpy as np
                
                # Use a larger timeout for connection, but process chunk by chunk
                res = requests.get(self.url, stream=True, timeout=10)
                if res.status_code != 200:
                    logger.warning(f"Got status {res.status_code} for camera {self.camera_id}, reconnecting...")
                    time.sleep(2)
                    continue
                    
                content_type = res.headers.get('Content-Type', '')
                if 'text/html' in content_type:
                    logger.error(f"Camera {self.camera_id} URL ({self.url}) returned an HTML webpage, not a video stream! Please add /video to the end of your URL.")
                    time.sleep(10) # Sleep longer so we don't spam requests
                    continue
                    
                bytes_data = bytes()
                last_frame_time = 0
                
                for chunk in res.iter_content(chunk_size=4096):
                    if not self.running:
                        break
                        
                    bytes_data += chunk
                    a = bytes_data.find(b'\xff\xd8') # JPEG start
                    b = bytes_data.find(b'\xff\xd9') # JPEG end
                    
                    if a != -1 and b != -1:
                        if a < b:
                            # Extract the frame
                            jpg = bytes_data[a:b+2]
                            bytes_data = bytes_data[b+2:]
                            
                            frame = cv2.imdecode(np.frombuffer(jpg, dtype=np.uint8), cv2.IMREAD_COLOR)
                            if frame is not None:
                                if not first_frame_read:
                                    logger.info(f"Successfully connected and reading frames from camera {self.camera_id}")
                                    first_frame_read = True
                                
                                # ALWAYS record the frame at native FPS
                                self._record_frame(frame)
                                
                                # Throttle AI processing to target FPS
                                current_time = time.time()
                                if current_time - last_frame_time >= frame_time:
                                    last_frame_time = current_time
                                    # Process the frame for AI scores
                                    self._process_frame(frame)
                        else:
                            # If b comes before a, we drop up to b to resync
                            bytes_data = bytes_data[b+2:]
                            
            except Exception as e:
                logger.warning(f"Connection error for camera {self.camera_id} ({self.url}): {e}, attempting reconnect...")
                time.sleep(2)
                
        if self.video_writer is not None:
            self.video_writer.release()
        logger.info(f"Stopped frame processor for camera {self.camera_id}")

    def _record_frame(self, frame):
        import datetime
        import os
        now = datetime.datetime.now()
        timestamp_str = now.strftime('%Y%m%d_%H00')
        cam_name = str(self.camera_info.get('camera_name', f'Cam{self.camera_id}')).replace(' ', '_')
        expected_filename = os.path.join(self.recordings_dir, f"{cam_name}_{timestamp_str}.avi")
        
        if self.video_writer is None or self.recording_filename != expected_filename:
            if self.video_writer is not None:
                self.video_writer.release()
            
            height, width = frame.shape[:2]
            fourcc = cv2.VideoWriter_fourcc(*'XVID')
            # Record at typical IP Webcam 15 FPS
            self.video_writer = cv2.VideoWriter(expected_filename, fourcc, 15.0, (width, height))
            self.recording_filename = expected_filename
            logger.info(f"Started recording to {expected_filename}")

        # Add Watermark
        loc = str(self.camera_info.get('location', 'Unknown'))
        display_text = f"{cam_name} | {loc} | {now.strftime('%Y-%m-%d %H:%M:%S')}"
        
        # Clone frame so we don't draw on the AI processing frame
        display_frame = frame.copy()
        cv2.rectangle(display_frame, (0, 0), (display_frame.shape[1], 30), (0, 0, 0), -1)
        cv2.putText(display_frame, display_text, (10, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
        
        if self.video_writer is not None:
            self.video_writer.write(display_frame)

    def _process_frame(self, frame):
        # Resize frame for faster processing
        frame = cv2.resize(frame, (320, 240))
        
        # --- Motion Detection ---
        fg_mask = self.bg_subtractor.apply(frame)
        
        # Remove shadows (MOG2 marks shadows as 127)
        _, fg_mask = cv2.threshold(fg_mask, 254, 255, cv2.THRESH_BINARY)
        
        # Calculate percentage of moving pixels
        non_zero = cv2.countNonZero(fg_mask)
        total_pixels = frame.shape[0] * frame.shape[1]
        motion_ratio = non_zero / total_pixels
        
        # Normalize to 0-1 (cap at 10% motion = 1.0 score to make it sensitive)
        self.motion_score = min(1.0, motion_ratio * 10)
        
        # Static placeholders for future ML models
        self.crowd_density = 0.1 
        self.unusual_behavior = 0.0

    def get_scores(self):
        overall = (self.motion_score * self.w_motion) + \
                  (self.crowd_density * self.w_crowd) + \
                  (self.unusual_behavior * self.w_behavior) + \
                  (self.time_factor * self.w_time)
                  
        return {
            'motion': self.motion_score,
            'crowd': self.crowd_density,
            'behavior': self.unusual_behavior,
            'time': self.time_factor,
            'overall': overall
        }

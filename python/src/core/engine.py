import logging
import threading
import time
from database.db_manager import DBManager
from core.frame_processor import CameraStreamProcessor

logger = logging.getLogger(__name__)

class CamFuEngine:
    def __init__(self, config):
        self.config = config
        self.db = DBManager(config)
        self.processors = {}  # camera_id -> CameraStreamProcessor
        self.running = False
        self.thread = None
        self.update_interval = config.get('ai_engine', {}).get('update_interval', 2)
        
    def start(self):
        if self.running:
            return
        self.running = True
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()
        logger.info("CamFu AI Engine background processing started.")
        
    def stop(self):
        self.running = False
        for p in self.processors.values():
            p.stop()
        if self.thread:
            self.thread.join(timeout=2.0)
        logger.info("CamFu AI Engine background processing stopped.")
        
    def _run_loop(self):
        while self.running:
            try:
                self._sync_cameras()
                self._update_scores()
            except Exception as e:
                logger.error(f"Error in engine loop: {e}")
            
            time.sleep(self.update_interval)
            
    def _sync_cameras(self):
        # Fetch active cameras from DB
        cameras = self.db.get_active_cameras()
        active_ids = {c['id'] for c in cameras}
        
        # Start new processors
        for c in cameras:
            cid = c['id']
            if cid not in self.processors:
                logger.info(f"New camera detected: {c['camera_name']}")
                p = CameraStreamProcessor(cid, c, self.config)
                p.start()
                self.processors[cid] = p
                
        # Stop removed processors
        removed_ids = set(self.processors.keys()) - active_ids
        for cid in removed_ids:
            logger.info(f"Camera {cid} no longer active, stopping processor.")
            self.processors[cid].stop()
            del self.processors[cid]
            
    def _update_scores(self):
        # Read scores from all processors and write to DB
        for cid, processor in self.processors.items():
            scores = processor.get_scores()
            success = self.db.save_priority_score(
                camera_id=cid,
                motion=scores['motion'],
                crowd=scores['crowd'],
                behavior=scores['behavior'],
                time_factor=scores['time'],
                overall=scores['overall']
            )
            if success:
                logger.info(f"Updated score for camera {cid}: {scores['overall']:.2f} (Motion: {scores['motion']:.2f})")

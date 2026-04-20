import mysql.connector
from mysql.connector import Error
import logging

logger = logging.getLogger(__name__)

class DBManager:
    def __init__(self, config):
        db_config = config.get('database', {})
        self.host = db_config.get('host', 'localhost')
        self.port = db_config.get('port', 3306)
        self.username = db_config.get('username', 'root')
        self.password = db_config.get('password', 'Shivam@9797')
        self.database = db_config.get('database', 'camfu_db')
        
    def get_connection(self):
        try:
            conn = mysql.connector.connect(
                host=self.host,
                port=self.port,
                user=self.username,
                password=self.password,
                database=self.database
            )
            return conn
        except Error as e:
            logger.error(f"Error connecting to MySQL: {e}")
            return None

    def get_active_cameras(self):
        conn = self.get_connection()
        if not conn:
            return []
            
        try:
            cursor = conn.cursor(dictionary=True)
            cursor.execute("SELECT id, camera_name, location, camera_url FROM cameras WHERE status = 'ACTIVE'")
            return cursor.fetchall()
        except Error as e:
            logger.error(f"Error fetching cameras: {e}")
            return []
        finally:
            if conn and conn.is_connected():
                cursor.close()
                conn.close()

    def save_priority_score(self, camera_id, motion, crowd, behavior, time_factor, overall):
        conn = self.get_connection()
        if not conn:
            return False
            
        try:
            cursor = conn.cursor()
            query = """
            INSERT INTO priority_scores 
            (camera_id, motion_score, crowd_density_score, unusual_behavior_score, time_factor, overall_priority_score)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (camera_id, motion, crowd, behavior, time_factor, overall))
            conn.commit()
            return True
        except Error as e:
            logger.error(f"Error saving priority score: {e}")
            return False
        finally:
            if conn and conn.is_connected():
                cursor.close()
                conn.close()

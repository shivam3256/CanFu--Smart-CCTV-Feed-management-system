import mysql.connector
import sys

try:
    conn = mysql.connector.connect(
        host="localhost",
        port=3306,
        user="root",
        password="Shivam@9797",
        database="camfu_db"
    )
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT id, camera_id, motion_score, overall_priority_score, timestamp FROM priority_scores ORDER BY timestamp DESC LIMIT 10")
    rows = cursor.fetchall()
    print(f"Total rows fetched: {len(rows)}")
    for row in rows:
        print(row)
    cursor.close()
    conn.close()
except Exception as e:
    print(f"Error: {e}")

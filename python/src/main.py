"""
CamFu AI Engine - Main Application Entry Point
Intelligent surveillance system with priority-based feed ranking
"""

import os
import sys
import yaml
from pathlib import Path

# Add src directory to path
sys.path.insert(0, str(Path(__file__).parent))

from flask import Flask
from flask_cors import CORS
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')


def load_config():
    """Load configuration from YAML file"""
    config_path = Path(__file__).parent.parent / 'config' / 'config.yaml'
    if config_path.exists():
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    return {}


def create_app():
    """Create and configure Flask application"""
    app = Flask(__name__)
    CORS(app)
    
    # Load configuration
    config = load_config()
    app.config['AI_CONFIG'] = config
    
    # Register blueprints (to be added)
    # from routes import api_bp
    # app.register_blueprint(api_bp)
    
    @app.route('/health', methods=['GET'])
    def health_check():
        """Health check endpoint"""
        return {
            'status': 'healthy',
            'service': 'CamFu AI Engine',
            'version': config.get('app', {}).get('version', '1.0.0')
        }, 200
    from core.engine import CamFuEngine
    
    # Initialize and start the background engine
    ai_engine = CamFuEngine(config)
    ai_engine.start()
    
    return app


def main():
    """Main entry point"""
    app = create_app()
    config = app.config['AI_CONFIG']
    
    server_config = config.get('server', {})
    host = server_config.get('host', '0.0.0.0')
    port = server_config.get('port', 5000)
    debug = server_config.get('debug', False)
    
    print(f"Starting CamFu AI Engine on {host}:{port}")
    app.run(host=host, port=port, debug=debug, use_reloader=False)


if __name__ == '__main__':
    main()

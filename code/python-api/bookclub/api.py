import logging
import os
from logging.config import dictConfig

from flask import Flask

from .database import dbmodels as models

dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
    }},
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'default'
    }},
    'root': {
        'level': 'INFO',
        'handlers': ['wsgi']
    }
})

db = models.db


# Based on http://flask.pocoo.org/docs/1.0/tutorial/factory/#the-application-factory
# Modified to use Flask SQLAlchemy
# https://lovelace.oulu.fi/ohjelmoitava-web/ohjelmoitava-web/flask-api-project-layout/
# Modified for docker MariaDB
def create_app(_=None):
    app = Flask(__name__, instance_relative_config=True)
    app.logger.setLevel(logging.DEBUG)
    app.logger.info("Starting Flask app")
    app.config.from_mapping(
        TESTING=True,
        SQLALCHEMY_ECHO=True,
        SECRET_KEY="dev",
        SQLALCHEMY_DATABASE_URI="mysql+pymysql://root:test@localhost:6969/book_club",
        SQLALCHEMY_TRACK_MODIFICATIONS=False
    )

    try:
        if app.config.from_envvar('BOOK_CLUB_SETTINGS'):
            app.logger.info("Loaded settings")
    except RuntimeError:
        app.logger.warning("Failed to load settings, assuming testing")

    try:
        os.makedirs(app.instance_path)
    except OSError:
        app.logger.warning(f"Failed to make app instance_path at {str(app.instance_path)}")
        pass

    db.init_app(app)

    from .resources.entry import entry_bp, Entry, CustomEntry
    app.register_blueprint(entry_bp)

    from flask_restful import Api
    api = Api(app)
    api.add_resource(CustomEntry, "/hello/<string:message>")
    api.add_resource(Entry, "/hello")

    app.logger.info("Initialized")
    return app


if __name__ == '__main__':
    _app = create_app()
    _app.run()

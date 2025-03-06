import functions_framework
from google.cloud.sql.connector import Connector, IPTypes
import sqlalchemy
import ssl
import pg8000
from sqlalchemy import text


@functions_framework.cloud_event
def hello_pubsub(cloud_event):
   print('Pub/Sub with Python in GCF 2nd gen! Id: ' + cloud_event['id'])
   
   # create SQLAlchemy connection pool
   pool = sqlalchemy.create_engine(
       "postgresql+pg8000://",
        creator=getconn,
   )
   with pool.connect() as db_conn:
        # query database
        result = db_conn.execute(text("SELECT * from users")).fetchall()

        # Do something with the results
        for row in result:
            print(row)

# SQLAlchemy database connection creator function
def getconn():
    connector = Connector()
    conn = connector.connect(
        "nih-nci-cimac-cidc-stage:us-east4:cidc-postgresql-stage2", # Cloud SQL Instance Connection Name
        "pg8000",
        user="cidcuser",
        password="xxxxxxxxx",
        db="cidc-cidc-stage2",
        ip_type=IPTypes.PUBLIC, # IPTypes.PRIVATE for private IP
        enable_iam_auth=True
    )
    return conn

    
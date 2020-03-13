#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=oracle-to-pub-sub
export BUSINESS_INTERFACE=poc

mvn clean package

java -cp target/dev-1.0-SNAPSHOT-shaded.jar \
OracleToPubSubBatchPipeline \
      --project=${PROJECT_ID} \
      --region=${REGION} \
      --serviceAccount=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
      --subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
      --stagingLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/staging \
      --tempLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/temp \
      --templateLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/template \
      --businessInterface=${BUSINESS_INTERFACE} \
      --driver=oracle.jdbc.OracleDriver \
      --url=jdbc:oracle:thin:@//dom12tstdb01.tmw.com:2494/domtest \
      --user=esbuser \
      --password=load4esb \
      --outputPubsubTopic=projects/${PROJECT_ID}/topics/${BUSINESS_INTERFACE} \
      --runner=DataflowRunner
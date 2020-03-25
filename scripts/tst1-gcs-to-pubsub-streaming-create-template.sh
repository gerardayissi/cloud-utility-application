#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=gcs-to-pub-sub
export BUSINESS_INTERFACE=item_full_feed

mvn clean package

java -cp target/cloud-integrations-0.8-shaded.jar \
com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubStreamingPipeline \
      --project=${PROJECT_ID} \
      --region=${REGION} \
      --serviceAccount=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
      --subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
      --stagingLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/staging \
      --tempLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/temp \
      --templateLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/template \
      --inputFilePattern=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/test/item-full-feed-source*.csv \
      --delimiter='|' \
      --user=admin@tmw.com \
      --organization=TMW \
      --businessInterface=${BUSINESS_INTERFACE} \
      --outputPubsubTopic=projects/${PROJECT_ID}/topics/${BUSINESS_INTERFACE} \
      --numWorkers=1 \
      --runner=DataflowRunner
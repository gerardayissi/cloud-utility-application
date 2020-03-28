#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=gcs-to-pub-sub
export BUSINESS_INTERFACE=store_inventory_full_feed
export JOB_NAME=gcs-to-pubsub-$USER-`date +"%Y-%m-%d_%H:%M:%S%z"`

# all params
export USER=admin@tbi.com
export ORG=TMW
export ERROR_THRESHOLD=10
export PROCESSED_BUCKET=gs://store-inventory-full-feed-processed
export FILES_TO_PROCESS_BUCKET=gs://store_inventory_full_feed/*.csv
export OUTPUT_TOPIC=projects/tst1-integration-3ca6/topics/mao-store-inventory-full-feed
export RUNNER=DataflowRunner

export GOOGLE_APPLICATION_CREDENTIALS=/Users/vpeche/apps/files/gcp/tst1-integration-3ca6-0bff2d5561eb.json

#mvn compile exec:java \
#      -Dexec.mainClass=com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubWithSyncPipeline \
#      -Dexec.args=" \
#      --project=${PROJECT_ID} \
#      --region=${REGION} \
#      --stagingLocation=gs://${PROJECT_ID}/dataflow/pipeline/${JOB_NAME}/staging \
#      --gcpTempLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/temp \
#      --templateLocation=${TEMPLATE_DIR}/${TEMPLATE} \
#      --numWorkers=1 \
#      --maxNumWorkers=5 \
#      --workerMachineType=n1-standard-1 \
#      --autoscalingAlgorithm=THROUGHPUT_BASED \
#      --businessInterface=${BUSINESS_INTERFACE} \
#      --user=${USER} \
#      --organization=${ORG} \
#      --errorThreshold=${ERROR_THRESHOLD} \
#      --processedBucket=${PROCESSED_BUCKET} \
#      --inputFilePattern=${FILES_TO_PROCESS_BUCKET} \
#      --durationSeconds=10 \
#      --delimiter=, \
#      --outputPubsubTopic=${OUTPUT_TOPIC} \
#      --runner=${RUNNER}"


mvn compile exec:java \
-Dexec.mainClass=com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubWithSyncPipeline \
-Dexec.args="--project=${PROJECT_ID} \
--region=${REGION} \
--numWorkers=1 \
--maxNumWorkers=5 \
--serviceAccount=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
--subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
--workerMachineType=n1-standard-1 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--businessInterface=${BUSINESS_INTERFACE} \
--user=${USER} \
--organization=${ORG} \
--errorThreshold=${ERROR_THRESHOLD} \
--processedBucket=${PROCESSED_BUCKET} \
--inputFilePattern=${FILES_TO_PROCESS_BUCKET} \
--durationSeconds=10 \
--outputPubsubTopic=${OUTPUT_TOPIC} \
--runner=DataflowRunner"
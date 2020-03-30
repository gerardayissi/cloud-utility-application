#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=gcs-to-pub-sub-with-sync
export BUSINESS_INTERFACE=store_inventory_full_feed
export JOB_NAME=gcs-2-pubsub-with-sync-300-batch-payload

export TEMPLATE_DIR=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}
export TEMPLATE=${JOB_NAME}

# all params
export USER=admin@tbi.com
export ORG=TMW
export DURATION=30
export ERROR_THRESHOLD=10
export BATCH_PAYLOAD=300
export PROCESSED_BUCKET=gs://store-inventory-full-feed-processed
export FILES_TO_PROCESS_BUCKET=gs://store_inventory_full_feed/*.csv
export OUTPUT_TOPIC=projects/tst1-integration-3ca6/topics/mao-store-inventory-full-feed
export RUNNER=DataflowRunner

export GOOGLE_APPLICATION_CREDENTIALS=/Users/vpeche/apps/files/gcp/tst1-integration-3ca6-0bff2d5561eb.json

mvn compile exec:java \
-Dexec.mainClass=com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubWithSyncPipeline \
-Dexec.args="--project=${PROJECT_ID} \
--region=${REGION} \
--numWorkers=3 \
--maxNumWorkers=5 \
--serviceAccount=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
--subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
--workerMachineType=n1-standard-1 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--stagingLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/staging \
--tempLocation=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/temp \
--templateLocation=${TEMPLATE_DIR}/${TEMPLATE} \
--businessInterface=${BUSINESS_INTERFACE} \
--user=${USER} \
--organization=${ORG} \
--errorThreshold=${ERROR_THRESHOLD} \
--batchPayloadSize=${BATCH_PAYLOAD} \
--processedBucket=${PROCESSED_BUCKET} \
--inputFilePattern=${FILES_TO_PROCESS_BUCKET} \
--durationSeconds=${DURATION} \
--outputPubsubTopic=${OUTPUT_TOPIC} \
--runner=DataflowRunner"
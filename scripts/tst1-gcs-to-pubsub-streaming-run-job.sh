#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=gcs-to-pub-sub

gcloud dataflow jobs run 2M-rows-20-files-1-worker-1-dofn \
    --project=${PROJECT_ID} \
    --region=${REGION} \
    --service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
    --gcs-location=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/template
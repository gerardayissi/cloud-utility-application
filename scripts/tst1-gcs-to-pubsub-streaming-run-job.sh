#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=gcs-to-pub-sub

gcloud dataflow jobs run test-tasl-omni-stg-04-ops-connectivity \
    --project=${PROJECT_ID} \
    --region=${REGION} \
    --service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
    --gcs-location=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}/template
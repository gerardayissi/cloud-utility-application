#!/bin/bash

export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6
export PIPELINE_NAME=oracle-to-pub-sub
export BUSINESS_INTERFACE=poc

gcloud dataflow jobs run test-shaded-pipeline \
    --project=tst1-integration-3ca6 \
    --region=us-east1 \
    --service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
    --gcs-location=gs://tst1-integration-3ca6/dataflow/pipeline/oracle-to-pub-sub/template
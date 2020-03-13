#!/usr/bin/env bash

export PROJECT_ID=tst1-integration-3ca6
export REGION=us-east1
export ZONE=us-east1-d

export BUCKET=gs://${PROJECT_ID}
export RUNNER=DataflowRunner
export TEMPLATE=connectivity
export TEMPLATE_DIR=${BUCKET}/pipeline/template

export TEMP_DIR=${BUCKET}/temp
export STAGING_DIR=${BUCKET}/pipeline/staging

export GOOGLE_CLOUD_PROJECT=${PROJECT_ID}
export REGION=us-east1
export PROJECT_ID=tst1-integration-3ca6

export JOB_NAME=gcs-2-pubsub-with-sync-300-batch-payload
export PIPELINE_NAME=gcs-to-pub-sub-with-sync
export TEMPLATE_DIR=gs://${PROJECT_ID}/dataflow/pipeline/${PIPELINE_NAME}
export TEMPLATE=${JOB_NAME}


echo "# RUN TEMPLATE"
gcloud --project=${PROJECT_ID} dataflow jobs run "${JOB_NAME}" \
--gcs-location="${TEMPLATE_DIR}"/"${TEMPLATE}" \
--region=${REGION} \
--service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com
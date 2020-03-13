export PROJECT_ID=tailoredbrandsresearch
export REGION=us-east1
export ZONE=us-east1-d

export BUCKET=gs://${PROJECT_ID}
export TEMPLATE=jms2pubsub_multi_topics_template
export TEMPLATE_DIR=${BUCKET}/pipeline/template
export JOB_NAME=jsm-to-pubsub-$USER-`date +"%Y-%m-%d_%H:%M:%S%z"`

# required params
export JMS_TO_PUBSUB_TYPE=facility
export OUTPUT_TOPICS=projects/tailoredbrandsresearch/topics/facility-location,projects/tailoredbrandsresearch/topics/facility-inventory-location,projects/tailoredbrandsresearch/topics/facility-location-attributes

echo "# RUN TEMPLATE"
gcloud --project=${PROJECT_ID} dataflow jobs run "${JOB_NAME}" \
--gcs-location=${TEMPLATE_DIR}/${TEMPLATE} \
--region=${REGION} \
--zone=${ZONE} \
--parameters=^--^\
jmsToPubsubPipelineType=${JMS_TO_PUBSUB_TYPE}--\
outputTopics=${OUTPUT_TOPICS}
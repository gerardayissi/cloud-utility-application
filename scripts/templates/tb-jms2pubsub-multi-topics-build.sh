#!/bin/bash

export PROJECT_ID=tailoredbrandsresearch
export REGION=us-east1
export BUCKET=gs://${PROJECT_ID}
export TEMPLATE=jms2pubsub_multi_topics_template
export TEMPLATE_DIR=${BUCKET}/pipeline/template
export JOB_NAME=jsm-to-pubsub-$USER-`date +"%Y-%m-%d_%H:%M:%S%z"`

# all params
export JMS_PROVIDER=tibco
export JMS_URL=tcp://35.204.30.82:7222
export JMS_USER=admin
export JMS_PASSWORD=
export JMS_QUEUE=facility
export JMS_TO_PUBSUB_INTERFACE=facility
export OUTPUT_TOPICS=projects/tailoredbrandsresearch/topics/facility-location,projects/tailoredbrandsresearch/topics/facility-inventory-location,projects/tailoredbrandsresearch/topics/facility-location-attributes
export RUNNER=DataflowRunner

mvn compile exec:java \
      -Dexec.mainClass=com.tailoredbrands.pipeline.pattern.jms_to_pub_sub.JmsToPubSubMultiTopicsPipeline \
      -Dexec.args=" \
      --project=${PROJECT_ID} \
      --region=${REGION} \
      --stagingLocation=gs://${PROJECT_ID}/dataflow/pipeline/${JOB_NAME}/staging \
      --tempLocation=gs://${PROJECT_ID}/dataflow/pipeline/${JOB_NAME}/temp \
      --templateLocation=${TEMPLATE_DIR}/${TEMPLATE} \
      --numWorkers=1 \
      --maxNumWorkers=5 \
      --workerMachineType=n1-standard-1 \
      --autoscalingAlgorithm=THROUGHPUT_BASED \
      --jmsProvider=${JMS_PROVIDER} \
      --jmsServerUrl=${JMS_URL} \
      --jmsUser=${JMS_USER} \
      --jmsPassword=${JMS_PASSWORD} \
      --jmsQueue=${JMS_QUEUE} \
      --jmsToPubsubPipelineType=${JMS_TO_PUBSUB_INTERFACE} \
      --outputTopics=${OUTPUT_TOPICS} \
      --runner=${RUNNER}"
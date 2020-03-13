#!/bin/bash

export PROJECT_ID=tailoredbrandsresearch
export PIPELINE_NAME=gcs-to-pubsub
export BUSINESS_INTERFACE=item_full_feed

mvn compile exec:java \
-Dexec.mainClass=GcsToPubSubBatchPipeline \
      -Dexec.args="--project=${PROJECT_ID}} \
      --stagingLocation=gs://tb-dataflow-pipelines/${PIPELINE_NAME}staging \
      --tempLocation=gs://tb-dataflow-pipelines/${PIPELINE_NAME}/temp \
      --user=user@example.com \
      --organization=example.com \
      --businessInterface=${BUSINESS_INTERFACE} \
      --inputFilePattern=gs://tb-dataflow-pipelines/${PIPELINE_NAME}/test/item-full-feed-source.csv \
      --delimiter=| \
      --outputPubsubTopic=projects/tailoredbrandsresearch/topics/${BUSINESS_INTERFACE} \
      --runner=DataflowRunner"
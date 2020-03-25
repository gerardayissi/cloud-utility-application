#!/bin/bash

export PROJECT_ID=np-integration-8656
export PIPELINE_NAME=gcs-to-pub-sub
export BUSINESS_INTERFACE=item_full_feed

mvn compile exec:java \
-Dexec.mainClass=GcsToPubSubStreamingPipeline \
      -Dexec.args="--project=${PROJECT_ID} \
      --serviceAccount=compute@np-integration-8656.iam.gserviceaccount.com \
      --stagingLocation=gs://tb-np-dataflow-pipelines/${PIPELINE_NAME}/staging \
      --tempLocation=gs://tb-np-dataflow-pipelines/${PIPELINE_NAME}/temp \
      --user=user@example.com \
      --organization=example.com \
      --businessInterface=${BUSINESS_INTERFACE} \
      --inputFilePattern=gs://tb-dataflow-pipelines/${PIPELINE_NAME}/test/item-full-feed-source.csv \
      --delimiter=| \
      --outputPubsubTopic=projects/${PROJECT_ID}/topics/${BUSINESS_INTERFACE} \
      --runner=DataflowRunner"
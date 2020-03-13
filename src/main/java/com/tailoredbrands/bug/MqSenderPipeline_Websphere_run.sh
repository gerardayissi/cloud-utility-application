#!/usr/bin/env bash

. ./env.sh

. ./env_webshere.sh

export JOB_NAME=${TEMPLATE}-sender-${PROVIDER}

gsutil cp Messages.txt ${TEMP_DIR}

echo "# RUN"

cd ../../../../../../

mvn -Pdataflow-runner compile exec:java \
-Dexec.mainClass=com.tailoredbrands.bug.MqSenderPipeline \
-Dexec.args="\
--project=${PROJECT_ID} \
--stagingLocation=${STAGING_DIR} \
--tempLocation=${TEMP_DIR} \
--templateLocation=${TEMPLATE_DIR}/${TEMPLATE}-sender \
--runner=${RUNNER} \
--network=shared-vpc \
--numWorkers=1 \
--maxNumWorkers=5 \
--workerMachineType=n1-standard-1 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
--jmsToPubsubPipelineType=${PROVIDER} \
--jmsUser=${JMS_USER}\
--jmsPassword=${JMS_PASSWORD}\
--jmsQueue=${JMS_QUEUE}\
--jmsServerUrl=${JMS_SERVER}\
--input=${TEMP_DIR}/Messages.txt \
--channel=${CHANNEL} \
--port=${PORT} \
--queueManager=${QUEUE_MANAGER}"

#!/usr/bin/env bash

. ./env.sh
cd ../../../../../../
mvn clean
mvn compile
echo "BUILD TEMPLATE"
mvn compile exec:java \
-Dexec.mainClass=com.tailoredbrands.bug.MqListenerPipeline \
-Dexec.cleanupDaemonThreads=false \
-Dexec.includePluginDependencies=true \
-Dexec.args=" \
--project=${PROJECT_ID} \
--stagingLocation=${STAGING_DIR} \
--tempLocation=${TEMP_DIR} \
--templateLocation=${TEMPLATE_DIR}/${TEMPLATE}-listener \
--runner=${RUNNER} \
--network=shared-vpc \
--numWorkers=1 \
--maxNumWorkers=5 \
--workerMachineType=n1-standard-1 \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4"
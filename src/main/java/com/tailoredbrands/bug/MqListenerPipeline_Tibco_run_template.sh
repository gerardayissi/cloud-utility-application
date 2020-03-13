#!/usr/bin/env bash

. ./env.sh

. ./env_tibco.sh

export JOB_NAME=${TEMPLATE}-listener-${PROVIDER}




echo "# RUN TEMPLATE"
gcloud --project=${PROJECT_ID} dataflow jobs run ${JOB_NAME}-$USER-`date +"%Y%m%d-%H%M%S%z"` \
--gcs-location=${TEMPLATE_DIR}/${TEMPLATE}-listener \
--region=${REGION} \
--zone=${ZONE} \
--service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
--parameters=^--^\
jmsUser=${JMS_USER}--\
jmsPassword=${JMS_PASSWORD}--\
jmsQueue=${JMS_QUEUE}--\
jmsServerUrl=${JMS_SERVER}--\
jmsProvider=${PROVIDER}
#!/usr/bin/env bash

. ./env.sh

. ./env_webshere.sh

export JOB_NAME=${TEMPLATE}-sender-${PROVIDER}

gsutil cp Messages.txt ${TEMP_DIR}

echo "# RUN TEMPLATE"
gcloud --project=${PROJECT_ID} dataflow jobs run ${JOB_NAME}-$USER-`date +"%Y%m%d-%H%M%S%z"` \
--gcs-location=${TEMPLATE_DIR}/${TEMPLATE}-sender \
--region=${REGION} \
--zone=${ZONE} \
--service-account-email=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
--parameters=^--^\
jmsUser=${JMS_USER}--\
jmsPassword=${JMS_PASSWORD}--\
jmsQueue=${JMS_QUEUE}--\
jmsServerUrl=${JMS_SERVER}--\
input=${TEMP_DIR}/Messages.txt--\
jmsToPubsubPipelineType=${PROVIDER}--\
channel=${CHANNEL}--\
port=${PORT}--\
queueManager=${QUEUE_MANAGER}


# with factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);

#java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
#java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
#java.lang.Thread.run(Thread.java:748) Caused by: com.ibm.msg.client.jms.DetailedIllegalStateException:
#JMSWMQ0018: Failed to connect to queue manager '' with connection mode 'Client' and host name 'tcp://35.204.30.82:7222(2024)'.
#Check the queue manager is started and if running in client mode, check there is a listener running.
#Please see the linked exception for more information. com.ibm.msg.client.wmq.common.internal.Reason.reasonToException(Reason.java:489)

# withOut factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
#com.ibm.mq.jms.MQConnectionFactory.createConnection(MQConnectionFactory.java:6030)
#org.apache.beam.sdk.io.jms.JmsIO$Write$WriterFn.setup(JmsIO.java:726) Caused by:
#com.ibm.mq.jmqi.local.LocalMQ$2: CC=2;RC=2495;AMQ8598: Failed to load the IBM MQ native JNI library: 'mqjbnd'.
#com.ibm.mq.jmqi.local.LocalMQ.loadLib(LocalMQ.java:1150) com.ibm.mq.jmqi.local.LocalMQ$1.run(LocalMQ.java:301)
#java.security.AccessController.doPrivileged(Native Method) com.ibm.mq
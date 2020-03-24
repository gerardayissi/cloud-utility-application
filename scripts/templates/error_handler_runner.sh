mvn -Pdataflow-runner compile exec:java \
      -Dexec.mainClass=com.tailoredbrands.pipeline.error.ErrorHandlingPipeline \
      -Dexec.args="--project=tst1-integration-3ca6 \
      --region=us-east1 \
      --gcpTempLocation=gs://tst1-integration-3ca6-jms-pubsub-df-temp/temp/ \
      --stagingLocation=gs://tst1-integration-3ca6-jms-pubsub-df-staging/staging/ \
      --serviceAccount=project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com \
      --subnetwork=https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4 \
      --defaultBucket=gs://tst1-integration-3ca6-default-deadletter/ \
      --deadletterPubsubSubscription=projects/tst1-integration-3ca6/subscriptions/deadletter_subscription \
      --patternToBucketMap=create_order_ecom|gs://tst1-integration-3ca6-create-order-ecom-errors/
      --runner=DataflowRunner"
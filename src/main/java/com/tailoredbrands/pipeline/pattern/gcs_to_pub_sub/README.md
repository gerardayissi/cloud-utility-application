## 0.1.1 Item Full Feed

### Pattern: Gcs to PubSub (OUT_GCS_PUBSUB)
### Pipeline: [GcsToPubSubStreamingPipeline](./GcsToPubSubStreamingPipeline.java)
### Mapping: [Item Full Feed](https://wiki.tailoredbrands.com/pages/viewpage.action?spaceKey=EIS&title=Item+Full+Feed)
### Jira: [CIS-30](https://jira.tailoredbrands.com/browse/CIS-30)
### Description:
1.	The streaming pipeline reads data from the GCS bucket using FileIO.  
PCollection<FileIO.Match> matches = pipeline .apply("Read Data",  
     ```FileIO.match() 
           .filepattern(options.getInputFilePattern())  
           .continuously(Duration.standardSeconds(60),   
                         Watch.Growth.<String>never()));```
2.	Transform each CSV row to JSON PubSub message.
3.	Push JSON messages to MAO PubSub topic
4.	Move the file that has been processed to the Processed folder in the same storage bucket.
5.	If there are data errors, raw data will be sent to another GCS path for review and reprocessing. 

### Input Csv
- [ItemFullFeed input](../../../../../../../test/resources/item_full_feed/item_full_feed_source.csv)

### Output Message
- [ItemFullFeed output](../../../../../../../test/resources/item_full_feed/item_full_feed_target.json)

### Build Template
```bash
./scripts/tst1-gcs-to-pubsub-batch-create-template.sh 
```

### Run Template
```bash
./scripts/tst1-gcs-to-pubsub-batch-run-job.sh 
```

### Run Locally
TODO

## Store Inventory Full Feed

### Pattern: Gcs to PubSub with Sync (OUT_GCS_PUBSUB)
### Pipeline: [GcsToPubSubWithSyncPipeline](./GcsToPubSubWithSyncPipeline.java)
### Mapping: [Store_Inventory Full Feed](https://wiki.tailoredbrands.com/display/JBOM/Detailed+Mapping#DetailedMapping-0.4.1.StoreInventorySync&0.4.3DCInventoryFullFeed)
### Jira: [CIS-26](https://jira.tailoredbrands.com/browse/CIS-26)
### Description:
1.	The streaming pipeline reads data from the GCS bucket using FileIO.  
PCollection<FileIO.Match> matches = pipeline .apply("Read Data",  
     ```FileIO.match() 
           .filepattern(options.getInputFilePattern())  
           .continuously(Duration.standardSeconds(60),   
                         Watch.Growth.<String>never()));```
2.	Read all file and validate rows.
3.  Create StartSync, SyncDetail(payload) and EndSync JSON to be published into PubSub MAO topic
4.  Make sure that batch payload has no more records than 300 because of limitation from Manhattan system
5.	Move the file that has been processed to the Processed folder in the same storage bucket.
6.	If there are data errors, raw data will be sent to another GCS path for review and reprocessing.

### Input Csv
- [StoreInventoryFullFeed input](../../../../../../../test/resources/store_inventory_full_feed/TMW_Store_Inventory_10012019.csv)

### Output Message
- [StoreInventoryFullFeed output](../../../../../../resources/json/store_inventory_full_feed/SyncDetail.json)

### Build Template
```bash
./scripts/templates/tst1-store-inventory-full-feed-build.sh
```

### Run Template
```bash
./scripts/tst1-store-inventory-full-feed-run-job.sh 
```
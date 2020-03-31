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

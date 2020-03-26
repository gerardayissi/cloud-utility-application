## 1.1.1 Availability Sync - Network

### Pattern: PubSub to Oracle (IN_PUBSUB_ORACLE)
### Pipeline: [AvailabilitySyncNetworkPipeline](./AvailabilitySyncNetworkPipeline.java)
### Mapping: [DetailedMapping](https://wiki.tailoredbrands.com/display/JBOM/Detailed+Mapping#DetailedMapping-1.1.1AvailabilitySync-Network)
### Description:
1. UC4 Scheduler will start a time based trigger to check if data from Store Inventory Full Feed and DC Inventory Full Feed 
have been processed on the Manhattan side using API calls to check the status. Once status returns successful, 
Manhattan will start publishing data to PubSub Topic which will be consumed by the streaming pipeline.
2. Read records from PubSub using PubSubIO
3. Log input records to Stackdriver, refer to the Logging section for details.
4. Identify the view and output the key as view and value as the record
5. Session Window should wait for certain period to accumulate all records for the view and trigger
   ```java
    PCollection<KV<String, String>> windowed = extracted
        .apply(Window.<KV<String, String>>into(Sessions
            .withGapDuration(Duration.standardSeconds(60L)))
            .withAllowedLateness(Duration.ZERO)
            .triggering(Repeatedly
                .forever(AfterWatermark
                    .pastEndOfWindow()
                    .withEarlyFirings(AfterProcessingTime
                        .pastFirstElementInPane()
                        .plusDelayOf(Duration.standardSeconds(10L)))))
            .accumulatingFiredPanes());
   ```
6. GroupBy each view key
7. ParDo should validate the count of records and contains an end sync [1.1.1](https://wiki.tailoredbrands.com/display/JBOM/Detailed+Mapping#DetailedMapping-1.1.1AvailabilitySync-Network) and [1.1.4](https://wiki.tailoredbrands.com/display/JBOM/Detailed+Mapping#DetailedMapping-1.1.4SkuStoreInventory)
    - If the check passes, then batch insert using JdbcIO write. Truncate stage table 
    - If the check fails, then write records to text error
8. Log output data before writing them to the target. 

### Q&A:

Q: What format of messages will Manhattan publishing?
Mapping mentions about three type of msg: 
- Availability Start Sync.json
- Availability Sync Detail.json
- Availability End Sync.json

But not clear why Start/End messages are needed because this pipeline will use a Window and group messages by NetworkAvailabilityResponse.ViewId

Or will Manhattan publishing just items of "NetworkAvailabilityResponse" section from the "*Detail.json" msg?

Q: Could you please provide target destination details? It is Oracle tables and fields to insert data.



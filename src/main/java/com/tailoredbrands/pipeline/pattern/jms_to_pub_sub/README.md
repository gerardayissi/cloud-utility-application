### Pattern: JMS to PubSub 
### Pipelines: 
[CreateOrder](../../business_interface/create_order/CreateOrderProcessor.java)
[ItemDeltaFeed](../../business_interface/item_delta_feed/ItemDeltaFeedProcessor.java)
[Facility](../../business_interface/facility/FacilityProcessor.java)
### Jira:
[CIS-9](https://jira.tailoredbrands.com/browse/CIS-9) 
[CIS-28](https://jira.tailoredbrands.com/browse/CIS-28)
[CIS-8](https://jira.tailoredbrands.com/browse/CIS-8)
### Description:
Pipeline steps
1. Read messages from JMS using JMSIO and JMS options supplied during pipeline creation.
2. Log and count incoming messages
3. Process message with suitable processor. Processor is determined by business interface name supplied from options.
4. Convert result to pubsub message
5. Partition resulting collection into successful and failed messages
6. For successful: log, count and write to Pubsub.
7. For failed: log, wrap into error message wrapper and write to deadletter Pubsub topic, supplied from options.

### Run Pipeline
Please see 
[CreateOrder](scripts/templates/tb-create-order-runner.sh)
[ItemDeltaFeed](scripts/templates/tb-item-delta-feed-runner.sh)

## Facility

#### Mapping: [facility](https://https://wiki.tailoredbrands.com/display/EIS/Facility)
#### Description:

1. Read messages from JMS using JMSIO and JMS options supplied during pipeline creation.
2. Log and count incoming messages
3. Process message with suitable processor. In this case we create list of PCollection in order to push message in particular topic.
4. Convert result to pubsub message
5. Partition resulting collection into successful and failed messages
6. For successful: log, count and write to Pubsub.
7. For failed: log, wrap into error message wrapper and write to deadletter Pubsub topic, supplied from options.

#### Input XML
- [Facility input](../../../../../../../test/resources/facility/9_Universe_Facility_xml.xml)

#### Output JSON
- [Facility Inventory Location output](../../../../../../resources/json/facility/MAOInventoryLocation.json)
- [Facility Location output](../../../../../../resources/json/facility/MAOLocation.json)
- [Facility LocationAttributes output](../../../../../../resources/json/facility/MAOLocationAttributes.json)

#### Build Template
```bash
./scripts/templates/tb-jms2pubsub-multi-topics-build.sh
```

#### Run Template
```bash
./scripts/tb-jms2pubsub-multi-topics-run.sh
```
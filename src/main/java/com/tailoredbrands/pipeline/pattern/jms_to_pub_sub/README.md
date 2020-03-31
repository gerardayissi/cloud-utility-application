### Pattern: JMS to PubSub 
### Pipelines: 
[CreateOrder](../../business_interface/create_order/CreateOrderProcessor.java)
[ItemDeltaFeed](../../business_interface/item_delta_feed/ItemDeltaFeedProcessor.java)
### Jira:
[CIS-9](https://jira.tailoredbrands.com/browse/CIS-9) 
[CIS-28](https://jira.tailoredbrands.com/browse/CIS-28)
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
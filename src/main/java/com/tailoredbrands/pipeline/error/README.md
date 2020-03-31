### Pattern: Error Handling 
### Pipelines: 
[ErrorHandling](./ErrorHandlingPipeline.java)
### Jira:
[CIS-32](https://jira.tailoredbrands.com/browse/CIS-32)
### Description:
Pipeline steps
1. Read messages from Pubsub 
2. Log and count messages
3. Write raw messages to GCS bucket according to message's pipeline of origin.

### Run Pipeline
Please see 
[ErrorHandling](scripts/templates/error_handler_runner.sh)
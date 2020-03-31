# Cloud Integration Services

### Project Source
Clone the project from repository
```bash
git clone https://github.com/MensWearhouse/cloud-integrations.git
```

### Maven build automation tool
This project is java8 base and use maven build automation tool. 
How to [Installing Apache Maven](http://maven.apache.org/install.html).

It uses this [pom.xml](./pom.xml) to manage all project dependencies.
Local TB Nexus repository will has all project libs.

There are some [libs](lib) has to be added to repository manually because these libs does not existed in [maven central repository](https://mvnrepository.com/).
More details [here](lib/readme.md).

### GCP stack 
This project is implementation of GSP Dataflow pipelines based on required patterns.
These patterns describes transformation between on-prem services and internal infrastructure.
It includes interaction between GCP PubSub, TIBCO JMS, IBM Websphere MQ and Oracle in different directions.

GCP resources in use: 
- GCP Storage
- Dataflow
- PubSub
- Stackdriver
- VPC Network
- Secret Manager

#### GCP local setup
```bash
gcloud config set account <YOUR_TB_ACCOUNT>@tailoredbrands.com
gcloud config set project <GCP_PROJECT_ID>
gcloud auth login <YOUR_TB_ACCOUNT>@tailoredbrands.com
```
To run pipeline template you need to generate key for Project Service Account, 
save the key as JSON file to local storage (LOCAL_PATH_TO_FILE) and set local variables
```bash
GOOGLE_APPLICATION_CREDENTIALS=<LOCAL_PATH_TO_FILE>.json;GOOGLE_CLOUD_PROJECT=<GCP_PROJECT_ID>
```

### CI/CD
CI/CD process integration [here](https://github.com/MensWearhouse/cloud-integrations/blob/poc/README.md)

### Resources
- [Pipelines declaration](https://teams.microsoft.com/l/file/EB58894D-91E6-404B-BE75-B865408B66C0?tenantId=aac81192-f6f6-4e8a-9f5e-a663e2d41612&fileType=xlsx&objectUrl=https%3A%2F%2Ftmw365.sharepoint.com%2Fsites%2Faoprodsupport%2FShared%20Documents%2FGeneral%2F2-%20Discovery-Rqmnts-Design%2FActiveOmni%20Interface%20List.xlsx&baseUrl=https%3A%2F%2Ftmw365.sharepoint.com%2Fsites%2Faoprodsupport&serviceName=teams&threadId=19:b867c41cdeea47a09b362bcd92ad10b7@thread.skype&groupId=cf737fbc-a891-4e85-a5d3-001074ff5b75)
- [Google Architecture vision](https://teams.microsoft.com/l/file/0876B682-2675-4388-B590-8245A517CDB4?tenantId=aac81192-f6f6-4e8a-9f5e-a663e2d41612&fileType=docx&objectUrl=https%3A%2F%2Ftmw365.sharepoint.com%2Fsites%2Faoprodsupport%2FShared%20Documents%2FGeneral%2FGoogle%20PSO%2FTailored%20Brands%20Integration_TDD_Final.docx&baseUrl=https%3A%2F%2Ftmw365.sharepoint.com%2Fsites%2Faoprodsupport&serviceName=teams&threadId=19:b867c41cdeea47a09b362bcd92ad10b7@thread.skype&groupId=cf737fbc-a891-4e85-a5d3-001074ff5b75)
- [Detailed Mapping](https://wiki.tailoredbrands.com/pages/viewpage.action?spaceKey=JBOM&title=Detailed+Mapping)
- [Code Repository](https://github.com/MensWearhouse/cloud-integrations)
- [Jira](https://jira.tailoredbrands.com/projects/CIS/summary)
- [MS Teams channel](https://teams.microsoft.com/l/channel/19%3ab867c41cdeea47a09b362bcd92ad10b7%40thread.skype/General?groupId=cf737fbc-a891-4e85-a5d3-001074ff5b75&tenantId=aac81192-f6f6-4e8a-9f5e-a663e2d41612)

### Pipelines implementation

- [0.4.1 Store Inventory Full Feed](src/main/java/com/tailoredbrands/pipeline/pattern/gcs_to_pub_sub/README.md#store-inventory-full-feed)
- [2.1.1 Create/Save Order (Ecom)](src/main/java/com/tailoredbrands/pipeline/pattern/jms_to_pub_sub/README.md)
- [2.1.2 Create/Save Order (Pre Sale)](src/main/java/com/tailoredbrands/pipeline/pattern/jms_to_pub_sub/README.md)
- [2.1.3 Create/Save Order (Custom Express)](src/main/java/com/tailoredbrands/pipeline/pattern/jms_to_pub_sub/README.md) 
- [0.1.1 Item Full Feed](src/main/java/com/tailoredbrands/pipeline/pattern/gcs_to_pub_sub/README.md#0.1.1 Item Full Feed)
- [0.2.1, 0.2.2, 0.2.3 Facility (aka. Location, Inventory Location, Location Attributes)](src/main/java/com/tailoredbrands/pipeline/pattern/jms_to_pub_sub/README.md#Facility)
- [0.1.2 Item Delta Feed](src/main/java/com/tailoredbrands/pipeline/pattern/jms_to_pub_sub/README.md)
- [1.1.1 Availability Sync - Network](src/main/java/com/tailoredbrands/pipeline/pattern/pub_sub_to_oracle/README.md#1.1.1 Availability Sync Network)

# Libs are required

### Apache beam bug

Apache Beam has a bug in JMS API described [here](https://issues.apache.org/jira/browse/BEAM-7427)
It will be fixed in 2.20 version (current official version is [2.19](https://github.com/apache/beam/tree/v2.19.0))
When the new version will be introduced you need to change [pom.xml](../pom.xml)
```xml
<beam.version>2.19.0</beam.version>
```
change to
```xml
<beam.version>2.20.0</beam.version>
```
and
```xml
<dependency>
    <groupId>org.apache.beam</groupId>
    <artifactId>beam-sdks-java-io-jms</artifactId>
    <version>2.20.0-SNAPSHOT</version>
</dependency>
```
change to
```xml
<dependency>
    <groupId>org.apache.beam</groupId>
    <artifactId>beam-sdks-java-io-jms</artifactId>
    <version>${beam.version}</version>
</dependency>
```
To avoid the issue we use [latest](https://repository.apache.org/content/groups/snapshots/org/apache/beam/beam-sdks-java-io-jms/2.20.0-SNAPSHOT/beam-sdks-java-io-jms-2.20.0-20200228.072934-42.jar) [snapshot](https://repository.apache.org/content/groups/snapshots/org/apache/beam/beam-sdks-java-io-jms/2.20.0-SNAPSHOT/) version

This lib has to be in TB main Nexus repository. In that case [pom.xml](../pom.xml) can resolve dependency.

You can add this libs to your local repository, if needed, just use [script](./install-beam-jms-2.20.sh) or next command:
```bash
mvn install:install-file -Dfile=./beam-jms-2.20/beam-sdks-java-io-jms-2.20.0-SNAPSHOT.jar -DgroupId=org.apache.beam -DartifactId=beam-sdks-java-io-jms -Dversion=2.20.0-SNAPSHOT -Dpackaging=jar
```

Local [Jira](https://jira.tailoredbrands.com/browse/CIS-3) task.

### TIBCO EMS libs are required

Maven repository does not have TIBCO EMS [libs](./tibco/ems/8.5/lib).

This lib has to be in TB main Nexus repository. In that case [pom.xml](../pom.xml) can resolve dependency.

You can add this libs to your local repository, if needed, just use [script](./install-tibco-lib.sh) or next command:
```bash
mvn install:install-file -Dfile=./lib/tibco/ems/8.5/lib/tibjms.jar -DgroupId=com.tibco -DartifactId=tibjms -Dversion=8.5.1 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/tibco/ems/8.5/lib/jms-2.0.jar -DgroupId=com.tibco -DartifactId=jms-2.0 -Dversion=8.5.1 -Dpackaging=jar
```

### ORACLE libs are required
Maven repository does not have ORACLE [libs](./oracle).

This lib has to be in TB main Nexus repository. In that case [pom.xml](../pom.xml) can resolve dependency.

You can add this libs to your local repository, if needed, just use [script](./install-oracle-lib.sh) or next command:
```bash
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=19.3 -Dpackaging=jar -Dfile=./lib/oracle/ojdbc8.jar
mvn install:install-file -DgroupId=com.oracle -DartifactId=ucp -Dversion=19.3 -Dpackaging=jar -Dfile=./lib/oracle/ucp.jar
```



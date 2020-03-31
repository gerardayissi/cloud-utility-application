#!/usr/bin/env bash

mvn install:install-file -Dfile=./tibco/ems/8.5/lib/tibjms.jar -DgroupId=com.tibco -DartifactId=tibjms -Dversion=8.5.1 -Dpackaging=jar
mvn install:install-file -Dfile=./tibco/ems/8.5/lib/jms-2.0.jar -DgroupId=com.tibco -DartifactId=jms-2.0 -Dversion=8.5.1 -Dpackaging=jar
#mvn install:install-file -Dfile=./lib/tibco/ems/8.5/lib/tibemsd.jar -DgroupId=com.tibco -DartifactId=tibemsd -Dversion=8.5.1 -Dpackaging=jar
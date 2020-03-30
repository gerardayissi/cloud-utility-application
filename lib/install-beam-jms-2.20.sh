#!/usr/bin/env bash

mvn install:install-file -Dfile=./beam20/beam-sdks-java-io-jms-2.20.0-SNAPSHOT.jar -DgroupId=org.apache.beam -DartifactId=beam-sdks-java-io-jms -Dversion=2.20.0-SNAPSHOT -Dpackaging=jar

#!/usr/bin/env bash

mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=19.3 -Dpackaging=jar -Dfile=./lib/oracle/ojdbc8.jar
mvn install:install-file -DgroupId=com.oracle -DartifactId=ucp -Dversion=19.3 -Dpackaging=jar -Dfile=./lib/oracle/ucp.jar
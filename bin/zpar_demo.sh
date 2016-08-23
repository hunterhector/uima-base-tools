#!/usr/bin/env bash
export MAVEN_OPTS="-Xmx18g"
mvn exec:java -pl zpar -Dexec.mainClass="edu.cmu.cs.lti.demo.ZParDemo"
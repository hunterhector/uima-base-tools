#!/usr/bin/env bash
export MAVEN_OPTS="-Xmx14g"
mvn exec:java -pl discourse-parser -Dexec.mainClass="edu.cmu.cs.lti.pipeline.DiscourseParserRunner" -Dexec.args=$1" "$2


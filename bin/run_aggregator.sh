#!/usr/bin/env bash
mvn exec:java -pl caevo -Dexec.mainClass="edu.cmu.cs.lti.pipeline.CmuResultAggregator" -Dexec.args="$args"

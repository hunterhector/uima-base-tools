#!/usr/bin/env bash
mvn exec:java -pl misc-annotators -Dexec.mainClass="edu.cmu.cs.lti.pipeline.CmuResultAggregator" -Dexec.args="$args"

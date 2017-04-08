#!/usr/bin/env bash
export JWNL=caevo/src/main/resources/caevo_resources/jwnl_file_properties.xml

args=$@

if (( $# > 0 )); then
    mvn exec:java -pl caevo -Dexec.mainClass="edu.cmu.cs.lti.annotators.CaevoAnnotator" -Dexec.args="$args"
fi

#!/usr/bin/env bash
mvn exec:java -pl corpus-reader -Dexec.mainClass="edu.cmu.cs.lti.collection_reader.AgigaHeadLineMatcher" -Dexec.args=$1" "$2

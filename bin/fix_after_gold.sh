#!/usr/bin/env bash
mvn exec:java -pl corpus-reader -Dexec.mainClass="edu.cmu.cs.lti.collection_reader.EreCoreferenceReplacer" -Dexec.args=$1" "$2" "$3" "$4" "$5

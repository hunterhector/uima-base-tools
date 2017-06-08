#!/usr/bin/env bash
args=${@:3}

if (( $# > 0 )); then
    mvn exec:java -pl $1 -Dexec.mainClass="$2" -Dexec.args="$args"
fi

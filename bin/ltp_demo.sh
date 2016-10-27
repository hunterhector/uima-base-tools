#!/usr/bin/env bash
export MAVEN_OPTS="-Xmx14g"
# Note that you need to follow the documentation to build the dynamic library and put them in the following path.
# https://github.com/HIT-SCIR/ltp4j/blob/master/doc/ltp4j-document-1.0.md
export LD_LIBRARY_PATH="/home/hector/projects/data/resources/ltp/lib"
mvn exec:java -pl ltp-parser -Dexec.mainClass="edu.cmu.cs.lti.demo.LtpParser"


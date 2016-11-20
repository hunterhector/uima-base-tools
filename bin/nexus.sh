#!/usr/bin/env bash
artifacts=(
'corpus-reader'
'discourse-parser'
'fanse-parser'
'ltp-parser'
'mate-tools'
'misc-annotators'
'opennlp'
'semafor-parser'
'stanford-corenlp'
'uima-base'
'uima-util'
'zpar'
)
subdirs=(
'corpus-reader'
'discourse-parser'
'fanse-parser'
'ltp-parser'
'mate-tools'
'misc-annotators'
'opennlp'
'semafor-parser'
'stanford-corenlp'
'uima-base'
'uima-util'
'zpar'
)

version='0.0.5'

mvn deploy:deploy-file -DgroupId=edu.cmu.cs.lti -Dversion=${version} -DartifactId=uima-tools -Dfile=pom.xml -Dpackaging=pom -Durl=http://deftpack.bbn.com:8081/nexus/content/repositories/DEFTLibraryDependencies -DrepositoryId=DEFTLibraryDependencies

for index in ${!artifacts[*]}; do
  artifact=${artifacts[${index}]}
  subdir=${subdirs[${index}]}
  echo "Pushing artifact ${artifact} in ${subdir} of version ${version}"
  mvn deploy:deploy-file -DgroupId=edu.cmu.cs.lti -Dversion=${version} -DartifactId=${artifact} -Dfile=${subdir}/target/${artifact}-${version}.jar -DpomFile=${subdir}/pom.xml -Dpackaging=jar -DgeneratePom=false -Durl=http://deftpack.bbn.com:8081/nexus/content/repositories/DEFTLibraryDependencies -DrepositoryId=DEFTLibraryDependencies
done



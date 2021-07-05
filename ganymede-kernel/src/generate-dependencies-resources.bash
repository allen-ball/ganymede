#!/bin/bash
# generate-dependencies-resources.bash
REPOSITORY="${HOME}/.m2/repository"

for artifact in ${*}; do
    IFS=":"; read -ra GAV <<< "${artifact}"
    G="${GAV[0]}"
    A="${GAV[1]}"
    V="${GAV[2]}"

    mvn -B dependency:get -Dartifact="${G}:${A}:${V}"

    name="${A}-${V}"

    pom="${REPOSITORY}/${G//\.//}/${A}/${V}/${name}.pom"
    outputFile="${PWD}/main/resources/META-INF/${name}.jar.dependencies"

    mvn -B -f "${pom}" dependency:tree \
        -DoutputScope=false -Dscope=runtime -Dtokens=whitespace \
        -DoutputFile="${outputFile}"
done

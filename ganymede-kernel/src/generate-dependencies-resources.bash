#!/bin/bash
# generate-dependencies-resources.bash
REPOSITORY="${HOME}/.m2/repository"
TRANSITIVE=true

for artifact in ${*}; do
    IFS=":"; read -ra GAV <<< "${artifact}"
    G="${GAV[0]}"
    A="${GAV[1]}"
    V="${GAV[${#GAV[@]}-1]]}"

    mvn -B dependency:get -Dtransitive="${TRANSITIVE}" -Dartifact="${G}:${A}:${V}"

    name="${A}-${V}"

    pom="${REPOSITORY}/${G//\.//}/${A}/${V}/${name}.pom"
    outputFile="${PWD}/main/resources/META-INF/${name}.jar.dependencies"

    if [ -f "${pom}" ]; then
        mvn -B -f "${pom}" dependency:tree \
            -Dexcludes=jdk.tools -DoutputScope=false -Dscope=runtime -Dtokens=whitespace \
            -DoutputFile="${outputFile}"
    fi
done

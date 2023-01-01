#!/bin/bash
# generate-dependencies-resources.bash
REPOSITORY="${HOME}/.m2/repository"
TRANSITIVE=true

MAVEN_DEPENDENCY_PLUGIN=dependency:3.5.0

set -u

for artifact in ${*}; do
    IFS=":"; read -ra GAV <<< "${artifact}"
    G="${GAV[0]}"
    A="${GAV[1]}"
    V="${GAV[${#GAV[@]}-1]]}"

    mvn -B "${MAVEN_DEPENDENCY_PLUGIN}:get" -Dtransitive="${TRANSITIVE}" -Dartifact="${G}:${A}:${V}"

    name="${A}-${V}"

    pom="${REPOSITORY}/${G//\.//}/${A}/${V}/${name}.pom"
    outputFile="${PWD}/main/resources/META-INF/${name}.jar.dependencies"

    if [ -f "${pom}" ]; then
        mvn -B -f "${pom}" "${MAVEN_DEPENDENCY_PLUGIN}:tree" \
            -Dexcludes=jdk.tools -DoutputScope=false -Dscope=runtime -Dtokens=whitespace \
            -DoutputFile="${outputFile}"
    fi
done

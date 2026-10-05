#!/usr/bin/env bash

# Builds the addon jar. Needs JDK 17+ and the mod jars in libs/.
# create-fabric-0_5_1-j-build_1631_mc1_20_1.jar
# Steam_Rails-1_6_9_fabric-mc1_20_1.jar
# fabric-api-0.92.7+1.20.1.jar
# fabric-loader-0.16.3.jar

set -euo pipefail

cd "$(dirname "$0")"

SR=$(find libs -maxdepth 1 -name 'Steam_Rails*.jar' -print -quit)
CR=$(find libs -maxdepth 1 -name 'create-fabric*.jar' -print -quit)
FA=$(find libs -maxdepth 1 -name 'fabric-api*.jar' -print -quit)
FL=$(find libs -maxdepth 1 -name 'fabric-loader*.jar' -print -quit)

if [ -z "$SR" ] || [ -z "$CR" ] || [ -z "$FA" ] || [ -z "$FL" ]; then
    echo "put Steam_Rails-*.jar, create-fabric-*.jar, fabric-api*.jar and fabric-loader*.jar into libs/" >&2
    exit 1
fi

VER=$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' \
    src/main/resources/fabric.mod.json | head -1)

rm -rf build
mkdir -p build/nested build/classes

unzip -q -o "$CR" 'META-INF/jars/*.jar' -d build/nested

NESTED_CP=$(find build/nested/META-INF/jars \
    -name '*.jar' -print0 | tr '\0' ':')

CP="$FL:$FA:$SR:$CR:$NESTED_CP"

javac --release 17 \
    -proc:none \
    -Xlint:none \
    -cp "$CP" \
    -sourcepath stubs \
    -d build/classes \
    $(find src/main/java -name '*.java')

OUT="build/railways_curvefix-$VER.jar"

jar --create \
    --file "$OUT" \
    -C build/classes io \
    -C src/main/resources .

echo "built $OUT"

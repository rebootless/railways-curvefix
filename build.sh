#!/usr/bin/env bash

# Builds the addon jar. Needs JDK 17+ and the two mod jars in libs/.
# create-fabric-0_5_1-j-build_1631_mc1_20_1.jar
# Steam_Rails-1_6_9_fabric-mc1_20_1.jar

set -euo pipefail

cd "$(dirname "$0")"
SR=$(ls libs/Steam_Rails*.jar 2>/dev/null | head -1 || true)
CR=$(ls libs/create-fabric*.jar 2>/dev/null | head -1 || true)
[ -n "$SR" ] && [ -n "$CR" ] || { echo "put Steam_Rails-*.jar and create-fabric-*.jar into libs/" >&2; exit 1; }
VER=$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' src/main/resources/fabric.mod.json | head -1)

rm -rf build && mkdir -p build/nested build/classes
unzip -q -o "$CR" 'META-INF/jars/*.jar' -d build/nested
CP="$SR:$CR:$(ls build/nested/META-INF/jars/*.jar | tr '\n' ':')"

javac --release 17 -proc:none -Xlint:none -cp "$CP" -sourcepath stubs -d build/classes \
  $(find src/main/java -name '*.java')

OUT="build/railways_curvefix-$VER.jar"
jar --create --file "$OUT" -C build/classes io -C src/main/resources .
echo "built $OUT"

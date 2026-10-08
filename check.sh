#!/usr/bin/env bash
# Pure-Java checks that need no running game: tier tables + JSON type round-trip.
set -euo pipefail

MC_HOME="/c/Users/Miyazawa i/Desktop/mc/.minecraft"
PACK_DIR="$MC_HOME/versions/AC26.2基础整合包Radical Version"
JAVA_BIN="/c/Users/Miyazawa i/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin"

cd "$(dirname "$0")"
win() { echo "$1" | sed 's|^/c/|C:/|'; }

CP="$(win "$PACK_DIR/AC26.2基础整合包Radical Version.jar")"
CP="$CP;$(win "$MC_HOME/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar")"
CP="$CP;$(win "$MC_HOME/libraries/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar")"
CP="$CP;$(win "$MC_HOME/libraries/net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar")"
for j in libs/fabric-*.jar; do CP="$CP;$(win "$j")"; done

rm -rf gen-test && mkdir -p gen-test
"$JAVA_BIN/javac" -J-Duser.language=en -encoding UTF-8 -d gen-test -cp "gen;$CP" tools/tools/TierTableCheck.java
"$JAVA_BIN/java" -Dstdout.encoding=UTF-8 -cp "gen-test;gen;$CP" tools.TierTableCheck

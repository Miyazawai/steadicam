#!/usr/bin/env bash
# Test-only build: same sources plus Probe.java, which opens the config screen on join.
# The shipped jar (build.sh) never contains this.
set -euo pipefail

MC_HOME="/c/Users/Miyazawa i/Desktop/mc/.minecraft"
PACK_DIR="$MC_HOME/versions/AC26.2基础整合包Radical Version"
JAVA_BIN="/c/Users/Miyazawa i/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin"

cd "$(dirname "$0")"
ROOT="$PWD"
win() { echo "$1" | sed 's|^/c/|C:/|'; }

CP_ENTRIES=("$(win "$PACK_DIR/AC26.2基础整合包Radical Version.jar")"
  "$(win "$MC_HOME/libraries/net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar")"
  "$(win "$MC_HOME/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar")"
  "$(win "$MC_HOME/libraries/org/joml/joml/1.10.8/joml-1.10.8.jar")"
  "$(win "$MC_HOME/libraries/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar")"
  "$(win "$MC_HOME/libraries/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar")"
  "$(win "$MC_HOME/libraries/org/lwjgl/lwjgl-glfw/3.4.1/lwjgl-glfw-3.4.1.jar")"
  "$(win "$PACK_DIR/mods/【模组菜单】ModMenu-20.0.1.jar")")
for j in libs/fabric-*.jar; do CP_ENTRIES+=("$(win "$j")"); done
CP=$(IFS=';'; echo "${CP_ENTRIES[*]}")

rm -rf gen-probe out/probe && mkdir -p gen-probe out/probe
"$JAVA_BIN/javac" -J-Duser.language=en -encoding UTF-8 -proc:none -Xlint:-unchecked -d gen-probe -cp "$CP" \
  $(find src probe -name '*.java')
cp -r resources/. out/probe/
python - <<'PY'
import json
p = r'C:\Users\Miyazawa i\Desktop\program\mod\steadicam\out\probe\fabric.mod.json'
d = json.load(open(p, encoding='utf-8'))
d['entrypoints']['client'].append('dev.mz.steadicam.Probe')
d['name'] = '稳镜 Steadicam [PROBE]'
json.dump(d, open(p, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('probe entrypoints:', d['entrypoints'])
PY
"$JAVA_BIN/jar" --create --file out/steadicam-probe.jar -C out/probe . -C gen-probe .
"$JAVA_BIN/jar" --list --file out/steadicam-probe.jar | grep -E "Probe|fabric.mod.json"

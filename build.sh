#!/usr/bin/env bash
# Build 稳镜 Steadicam against the jars already on this machine. No downloads.
set -euo pipefail

MC_HOME="/c/Users/Miyazawa i/Desktop/mc/.minecraft"
PACK_DIR="$MC_HOME/versions/AC26.2基础整合包Radical Version"
JAVA_BIN="/c/Users/Miyazawa i/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin"
MOD_ID="steadicam"
VERSION="1.0.1+26.2"

cd "$(dirname "$0")"
ROOT="$PWD"
GEN="$ROOT/gen"
STAGE="$ROOT/out/build"
OUT="$ROOT/out"
LIBS="$ROOT/libs"

win() { echo "$1" | sed 's|^/c/|C:/|'; }

rm -rf "$GEN" "$STAGE"
mkdir -p "$GEN" "$STAGE" "$LIBS"

echo "==> [1/5] 从 Fabric-API fat jar 抽模块 jar"
python - "$PACK_DIR/mods/Fabric-API-0.155.2+26.2.jar" "$LIBS" <<'PY'
import os, sys, zipfile
src, out = sys.argv[1], sys.argv[2]
want = ('fabric-api-base-2.0.4', 'fabric-rendering-v1-25.3.1',
        'fabric-lifecycle-events-v1-4.1.3', 'fabric-key-mapping-api-v1-2.0.5')
z = zipfile.ZipFile(src)
for n in z.namelist():
    base = os.path.basename(n)
    if n.startswith('META-INF/jars/') and any(base.startswith(w) for w in want):
        open(os.path.join(out, base), 'wb').write(z.read(n))
        print('   ', base)
PY

echo "==> [2/5] 组装 classpath"
CP_ENTRIES=(
  "$(win "$PACK_DIR/AC26.2基础整合包Radical Version.jar")"
  "$(win "$MC_HOME/libraries/net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar")"
  "$(win "$MC_HOME/libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar")"
  "$(win "$MC_HOME/libraries/org/joml/joml/1.10.8/joml-1.10.8.jar")"
  "$(win "$MC_HOME/libraries/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar")"
  "$(win "$MC_HOME/libraries/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar")"
  "$(win "$MC_HOME/libraries/org/lwjgl/lwjgl-glfw/3.4.1/lwjgl-glfw-3.4.1.jar")"
  "$(win "$PACK_DIR/mods/【模组菜单】ModMenu-20.0.1.jar")"
)
for j in "$LIBS"/fabric-*.jar; do CP_ENTRIES+=("$(win "$j")"); done
CP=$(IFS=';'; echo "${CP_ENTRIES[*]}")

echo "==> [3/5] javac"
"$JAVA_BIN/javac" -J-Duser.language=en -encoding UTF-8 -proc:none -Xlint:-unchecked -d "$GEN" -cp "$CP" \
  $(find src -name '*.java')

echo "==> [4/5] 打包"
cp -r resources/. "$STAGE/"
JAR="$OUT/$MOD_ID-$VERSION.jar"
rm -f "$JAR"
"$JAVA_BIN/jar" --create --file "$JAR" -C "$STAGE" . -C "$GEN" .

echo "==> [5/5] 静态校验"
LIST=$("$JAVA_BIN/jar" --list --file "$JAR")
echo "$LIST" | grep -qx 'fabric.mod.json' || { echo 'FAIL: fabric.mod.json 不在根目录'; exit 1; }
for FQCN in $(python -c "
import json;d=json.load(open('resources/fabric.mod.json',encoding='utf-8'))
print(' '.join(c for v in d['entrypoints'].values() for c in (v if isinstance(v,list) else [v])))
"); do
  PATH_="${FQCN//.//}.class"
  echo "$LIST" | grep -qx "$PATH_" || { echo "FAIL: entrypoint $FQCN 缺 $PATH_"; exit 1; }
done
python -c "
import json
d=json.load(open('resources/fabric.mod.json',encoding='utf-8'))
assert 'mixins' not in d, '本 mod 必须零 mixin'
print('    depends:', d['depends'])
print('    entrypoints:', d['entrypoints'])
"
echo "$LIST" | grep -c '^assets/steadicam/lang/' | xargs echo '    lang 文件数:'
ls -l "$JAR" | awk '{print "    产物:", $5" bytes", $9}'
echo "OK"

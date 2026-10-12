#!/usr/bin/env bash
# Runs the Minecraft client locally the same way the mc-runtime-test action does,
# to reproduce issues before they hit CI.
#
# Usage: run-client-gametest.sh <modloader> <mc> <mod.jar>...
#   modloader: fabric, neoforge or forge
#
# Environment:
#   WORK_DIR            working directory (default: build/run-client/<modloader>-<mc>)
#   JAVA_HOME           Java used for the game (default: java on PATH)
#   HMC_JAVA            Java used for HeadlessMC (needs 21+, default: java on PATH)
#   HMC_VERSION         HeadlessMC version (default: 3.0.0-RC5, same as mc-runtime-test@main)
#   MC_DIR              .minecraft directory to use (default: $WORK_DIR/.minecraft)
#   FABRIC_API          fabric-api version to download, e.g. 0.116.7
#   FABRIC_GAMETEST_API fabric-gametest-api-v1 version to download, e.g. 2.0.9+50ed0faf7a
#   TIMEOUT             seconds before the game is considered hung (default: 300)
#   JVM_ARGS            additional jvm args (default: -DMcRuntimeGameTestMinExpectedGameTests=1)
set -euo pipefail

if [ "$#" -lt 3 ]; then
  sed -n '2,18p' "$0"
  exit 2
fi

modloader="$1"
mc="$2"
shift 2

root="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
work="${WORK_DIR:-$root/build/run-client/$modloader-$mc}"
hmc_version="${HMC_VERSION:-3.0.0-RC5}"
hmc_java="${HMC_JAVA:-java}"
java_home="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}"

mkdir -p "$work/HeadlessMC" "$work/run/mods"
rm -f "$work/run/mods/"*.jar "$work/run/logs/latest.log"
cp "$@" "$work/run/mods/"

if [ ! -f "$work/headlessmc-$hmc_version.jar" ]; then
  curl -sSfLo "$work/headlessmc-$hmc_version.jar" \
    "https://github.com/headlesshq/headlessmc/releases/download/$hmc_version/headlessmc.jar"
fi

cat > "$work/HeadlessMC/config.properties" <<EOF
hmc.java.versions=$java_home
hmc.files.game=$work/run
hmc.files.game-for-each-version=false
hmc.files.mc=${MC_DIR:-$work/.minecraft}
hmc.jline.enabled=false
hmc.launcher.crash-report-watcher=true
hmc.assets.dummy=true
EOF

if [ -n "${FABRIC_API:-}" ]; then
  curl -sSfLo "$work/run/mods/fabric-api-$FABRIC_API+$mc.jar" \
    "https://github.com/FabricMC/fabric-api/releases/download/$FABRIC_API%2B$mc/fabric-api-$FABRIC_API+$mc.jar"
fi

if [ -n "${FABRIC_GAMETEST_API:-}" ]; then
  curl -sSfLo "$work/run/mods/fabric-gametest-api-v1-$FABRIC_GAMETEST_API.jar" \
    "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-gametest-api-v1/$FABRIC_GAMETEST_API/fabric-gametest-api-v1-$FABRIC_GAMETEST_API.jar"
fi

cat > "$work/run/options.txt" <<EOF
onboardAccessibility:false
pauseOnLostFocus:false
EOF

export HMC_FILES_LOCATION="$work/HeadlessMC"
cd "$work"
# Installing can fail spuriously for fabric, launching still works.
"$hmc_java" -jar "headlessmc-$hmc_version.jar" version install "$modloader" "$mc" || true

status=0
timeout --foreground -k 10 "${TIMEOUT:-300}" \
  xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" \
  "$hmc_java" -Dhmc.xvfb.check=true -jar "headlessmc-$hmc_version.jar" launch --offline \
  --jvm "-Djava.awt.headless=true ${JVM_ARGS:--DMcRuntimeGameTestMinExpectedGameTests=1}" \
  "$modloader" "$mc" || status=$?

if [ "$status" -eq 124 ]; then
  echo "Game timed out after ${TIMEOUT:-300}s, it probably hung."
  # the game jvm is started by HeadlessMC and survives the timeout, its classpath contains $work
  pkill -f -- "$work/HeadlessMC" || true
fi
echo "Game exited with $status"

"$root/.github/scripts/check-gametest-log.sh" "$work/run/logs/latest.log" && [ "$status" -eq 0 ]

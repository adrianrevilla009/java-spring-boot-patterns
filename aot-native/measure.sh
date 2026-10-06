#!/usr/bin/env bash
# Usage: ./measure.sh jvm|aot|native   (build first, see README). Prints startup time and RSS.
set -euo pipefail
cd "$(dirname "$0")"
mode="${1:?mode: jvm|aot|native}"
log="$(mktemp)"
case "$mode" in
  jvm)    java -jar target/aot-native-1.0.0.jar >"$log" 2>&1 & ;;
  aot)    java -Dspring.aot.enabled=true -jar target/aot-native-1.0.0.jar >"$log" 2>&1 & ;;
  native) [ -x target/aot-native ] || { echo "no native binary: needs GraalVM, see README"; exit 2; }
          ./target/aot-native >"$log" 2>&1 & ;;
  *) echo "unknown mode $mode"; exit 1 ;;
esac
pid=$!
for _ in $(seq 1 60); do grep -q "Started App" "$log" && break; sleep 0.5; done
started="$(grep -o 'Started App in [0-9.]* seconds' "$log" | head -1)"
rss_kb="$(awk '/VmRSS/ {print $2}' "/proc/$pid/status")"
kill "$pid"; wait "$pid" 2>/dev/null || true
echo "RESULT $mode: ${started:-no start line} | RSS $((rss_kb / 1024)) MB"
rm -f "$log"

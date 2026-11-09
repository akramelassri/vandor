#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

case "${1:-}" in
  "") options=(--replace) ;;
  --check) options=(--dry-run --set-exit-if-changed) ;;
  *) echo "Usage: $0 [--check]" >&2; exit 2 ;;
esac

formatter="${GOOGLE_JAVA_FORMAT_JAR:-target/tools/google-java-format-2.0.0.jar}"
if [[ ! -f "$formatter" ]]; then
  mkdir -p "$(dirname "$formatter")"
  curl --fail --location --silent --show-error \
    https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/2.0.0/google-java-format-2.0.0-all-deps.jar \
    -o "$formatter.tmp"
  mv "$formatter.tmp" "$formatter"
fi

mapfile -d '' files < <(find src -name '*.java' -print0 | sort -z)
java -jar "$formatter" "${options[@]}" "${files[@]}"

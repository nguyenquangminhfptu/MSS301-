#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && "$(uname)" == Darwin ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
mvn -B -f pom.xml clean verify

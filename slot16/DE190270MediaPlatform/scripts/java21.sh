#!/usr/bin/env bash
set -euo pipefail
if [[ "$(uname -s)" == Darwin ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

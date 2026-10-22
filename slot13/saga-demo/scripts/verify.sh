#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
# Integration tests require healthy MySQL, SQL Server and MongoDB containers.
# Without this variable Maven only runs the orchestrator unit tests.
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi
mvn -B verify "$@"

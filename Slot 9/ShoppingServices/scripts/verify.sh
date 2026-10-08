#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 21)
  export JAVA_HOME
fi
if [ -S "$HOME/.docker/run/docker.sock" ] && [ -z "${DOCKER_HOST:-}" ]; then
  export DOCKER_HOST="unix://$HOME/.docker/run/docker.sock"
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
fi
for service in product-service inventory-service order-service api-gateway; do
  (cd "$service" && sh ./mvnw -B -ntp clean verify)
done

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java21.sh
mkdir -p .run
for service in UserService PostService MediaGateway; do
  name="DE190270$service"
  case "$service" in
    UserService) artifact=userservice;;
    PostService) artifact=postservice;;
    MediaGateway) artifact=mediagateway;;
  esac
  jar="$name/target/$artifact-1.0.0.jar"
  if [[ ! -f "$jar" ]]; then
    echo "Missing $jar. Run ./scripts/build.sh first." >&2
    exit 1
  fi
  if [[ -f ".run/$name.pid" ]] && kill -0 "$(cat ".run/$name.pid")" 2>/dev/null; then
    echo "$name is already running." >&2
    exit 1
  fi
done
for service in UserService PostService MediaGateway; do
  name="DE190270$service"
  case "$service" in
    UserService) artifact=userservice;;
    PostService) artifact=postservice;;
    MediaGateway) artifact=mediagateway;;
  esac
  nohup "$JAVA_CMD" -jar "$name/target/$artifact-1.0.0.jar" > ".run/$name.log" 2>&1 &
  echo "$!" > ".run/$name.pid"
done
for port in 8081 8082 8080; do
  ready=false
  for attempt in {1..60}; do
    if curl --silent --fail "http://localhost:$port/v3/api-docs" >/dev/null; then
      ready=true
      break
    fi
    sleep 1
  done
  if [[ "$ready" != true ]]; then
    echo "Port $port did not become ready. See .run/*.log and run ./scripts/stop.sh." >&2
    exit 1
  fi
done
echo "Ready: http://localhost:8080/swagger-ui.html"
if [[ "${1:-}" == --foreground ]]; then
  trap './scripts/stop.sh' INT TERM EXIT
  wait
fi

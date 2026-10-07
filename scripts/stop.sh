#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for service in customer-service movie-service booking-service api-gateway; do
  if [[ -f ".run/$service.pid" ]]; then
    pid="$(cat ".run/$service.pid")"
    if kill -0 "$pid" 2>/dev/null && ps -p "$pid" -o args= | grep -F "$service-0.0.1-SNAPSHOT.jar" >/dev/null; then
      kill "$pid"
    fi
    rm -f ".run/$service.pid"
  fi
done
echo "Assignment Java services stopped; database volumes retained"

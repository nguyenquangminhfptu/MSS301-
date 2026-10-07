#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && "$(uname)" == Darwin ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p .run
services=(customer-service movie-service booking-service api-gateway)
ports=(8081 8082 8083 9000)
for index in 0 1 2 3; do
  service="${services[$index]}"
  if [[ -f ".run/$service.pid" ]] && kill -0 "$(cat ".run/$service.pid")" 2>/dev/null; then
    echo "$service is already running"
    continue
  fi
  jar="$service/target/$service-0.0.1-SNAPSHOT.jar"
  [[ -f "$jar" ]] || { echo "Run scripts/build.sh first"; exit 1; }
  if command -v lsof >/dev/null && lsof -iTCP:"${ports[$index]}" -sTCP:LISTEN >/dev/null; then
    echo "Port ${ports[$index]} is occupied; no process was stopped"; exit 1
  fi
  nohup "$java_cmd" -Duser.timezone=Asia/Ho_Chi_Minh -jar "$jar" --logging.level.root=INFO --logging.level.org.springframework.web=INFO > ".run/$service.log" 2>&1 &
  echo "$!" > ".run/$service.pid"
done
for attempt in $(seq 1 180); do
  if curl -fsS http://localhost:9000/actuator/health >/dev/null &&
     curl -fsS http://localhost:9000/api/genres >/dev/null &&
     curl -fsS http://localhost:8081/api/auth/login -H 'Content-Type: application/json'        -d '{"email":"admin@fucinema.com","password":"@@abc123@@"}' >/dev/null &&
     curl -fsS http://localhost:9000/api/bookings/showtimes/66f300000000000000000001/seats >/dev/null; then
    echo "All four services ready. Gateway: http://localhost:9000"
    if [[ "${1:-}" == "--foreground" ]]; then wait; fi
    exit 0
  fi
  for service in "${services[@]}"; do
    kill -0 "$(cat ".run/$service.pid")" 2>/dev/null || {
      tail -n 40 ".run/$service.log"; echo "$service exited"; exit 1;
    }
  done
  sleep 1
done
echo "Startup timed out. Inspect .run/*.log"; exit 1

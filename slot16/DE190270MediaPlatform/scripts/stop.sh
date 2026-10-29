#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for file in .run/DE190270*.pid; do
  [[ -f "$file" ]] || continue
  process_id="$(cat "$file")"
  command_line="$(ps -p "$process_id" -o command= 2>/dev/null || true)"
  if [[ "$command_line" == *DE190270* && "$command_line" == *-1.0.0.jar* ]]; then
    kill "$process_id" 2>/dev/null || true
  fi
  rm -f "$file"
done

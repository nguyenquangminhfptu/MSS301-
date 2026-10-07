#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p evidence .run
newman run postman/FUCinemaBookingSystem.postman_collection.json   -e postman/FUCinema-Local.postman_environment.json   --color off --reporters cli,json --reporter-json-export .run/newman-raw.json   --export-environment postman/run-environment.json
python3 scripts/summarize-postman.py

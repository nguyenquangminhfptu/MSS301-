#!/usr/bin/env bash
set -euo pipefail
sqlcmd=/opt/mssql-tools18/bin/sqlcmd
# A new container starts with the strong bootstrap password. Subsequent runs use the exam password.
if "$sqlcmd" -S sqlserver -U sa -P sa -C -Q 'SELECT 1' >/dev/null 2>&1; then
  "$sqlcmd" -S sqlserver -U sa -P sa -C -b -i /sql/init.sql
else
  "$sqlcmd" -S sqlserver -U sa -P 'Slot16@2026' -C -b -i /sql/init.sql
  "$sqlcmd" -S sqlserver -U sa -P 'Slot16@2026' -C -b -i /sql/docker-exam-login.sql
fi

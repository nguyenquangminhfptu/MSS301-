#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java21.sh
mvn -B -ntp clean
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root = Path.cwd()
output = root.parent / 'DE190270_Slot16_Submission.zip'
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob('*')):
        relative = path.relative_to(root)
        if path.is_file() and not any(part in {'target', '.run', '.idea', '.git', '__pycache__'} for part in relative.parts) and path.name != '.DS_Store':
            archive.write(path, Path(root.name) / relative)
print(output)
PY

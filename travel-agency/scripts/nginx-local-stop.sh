#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

nginx -p "$ROOT_DIR" -c "$ROOT_DIR/nginx.local.conf" -s stop

echo "Nginx local detenido"

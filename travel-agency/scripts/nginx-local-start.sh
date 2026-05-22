#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

mkdir -p \
  "$ROOT_DIR/.nginx/logs" \
  "$ROOT_DIR/.nginx/client_body_temp" \
  "$ROOT_DIR/.nginx/proxy_temp" \
  "$ROOT_DIR/.nginx/fastcgi_temp" \
  "$ROOT_DIR/.nginx/uwsgi_temp" \
  "$ROOT_DIR/.nginx/scgi_temp"

if [ ! -d "$ROOT_DIR/dist" ]; then
  echo "No existe dist/. Ejecuta primero: npm run build"
  exit 1
fi

nginx -p "$ROOT_DIR" -c "$ROOT_DIR/nginx.local.conf"

echo "Nginx levantado en http://localhost:8081"

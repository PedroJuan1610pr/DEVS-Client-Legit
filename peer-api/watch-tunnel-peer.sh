#!/usr/bin/env bash
# Lee el log de cloudflared y actualiza el Gist cuando cambia la URL trycloudflare.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="${1:-/opt/devs-peer-api/tunnel.log}"
STATE_FILE="/opt/devs-peer-api/last-tunnel-url.txt"

mkdir -p /opt/devs-peer-api
touch "$LOG_FILE"
echo "$(date '+%F %T') Vigilando $LOG_FILE"

tail -n0 -F "$LOG_FILE" 2>/dev/null | grep --line-buffered -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' | \
while read -r URL; do
  LAST=""
  [ -f "$STATE_FILE" ] && LAST="$(cat "$STATE_FILE")"
  if [ "$URL" != "$LAST" ]; then
    echo "$(date '+%F %T') Nueva URL: $URL"
    if "$SCRIPT_DIR/update-gist-peer.sh" "$URL"; then
      echo "$URL" > "$STATE_FILE"
    fi
  fi
done

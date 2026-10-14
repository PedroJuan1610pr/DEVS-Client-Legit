#!/usr/bin/env bash
# Publica la URL del túnel peer-api en el Gist (mismo patrón que Licencias).
set -euo pipefail

GIST_ID="6cff64c9557c02e22ab743c7a912ff96"
GIST_FILENAME="peer-api.json"
TOKEN_FILE="${TOKEN_FILE:-$HOME/.config/licencias/github-gist-token}"

if [ -z "${1:-}" ]; then
  echo "Uso: $0 <url-base-del-tunel>" >&2
  exit 1
fi
BASE_URL="${1%/}"

if [ ! -f "$TOKEN_FILE" ]; then
  echo "ERROR: no hay token en $TOKEN_FILE" >&2
  exit 1
fi
GITHUB_TOKEN="$(cat "$TOKEN_FILE")"

PAYLOAD=$(python3 - "$GIST_FILENAME" "$BASE_URL" <<'PY'
import json, sys
filename, url = sys.argv[1], sys.argv[2]
file_content = json.dumps({"url": url})
print(json.dumps({"files": {filename: {"content": file_content}}}))
PY
)

HTTP_CODE=$(curl -s -o /tmp/gist-peer-response.json -w "%{http_code}" \
  -X PATCH \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  -H "User-Agent: devs-peer-tunnel-updater" \
  "https://api.github.com/gists/$GIST_ID" \
  -d "$PAYLOAD")

if [ "$HTTP_CODE" = "200" ]; then
  echo "$(date '+%F %T') OK - Gist peer-api → $BASE_URL"
else
  echo "$(date '+%F %T') ERROR gist HTTP $HTTP_CODE" >&2
  cat /tmp/gist-peer-response.json >&2
  exit 1
fi

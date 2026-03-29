#!/usr/bin/env bash
# Démarre un seed via le conteneur NGINX microservices, puis lance k6 sur :8082 et :8090
# et écrit docs/metrics/k6-summary-8082.json et k6-summary-8090.json.
# Prérequis : stack UP (docker compose -f docker-compose.lb.yml up -d), k6 installé sur l'hôte.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! docker ps --format '{{.Names}}' | grep -q '^canbankx_nginx$'; then
  echo "Erreur : conteneur canbankx_nginx introuvable. Lance : docker compose -f docker-compose.lb.yml up -d"
  exit 1
fi

docker cp scripts/seed-and-test.sh canbankx_nginx:/tmp/seed-report.sh
OUT="$(docker exec canbankx_nginx bash /tmp/seed-report.sh 2>&1)" || true
echo "$OUT"
LINE=$(echo "$OUT" | grep 'CID_A=' | tail -1)
CID=$(echo "$LINE" | grep -oE 'CID_A=[a-f0-9-]+' | head -1 | cut -d= -f2)
SRC=$(echo "$LINE" | grep -oE 'AID_SRC=[a-f0-9-]+' | head -1 | cut -d= -f2)
DST=$(echo "$LINE" | grep -oE 'AID_DST=[a-f0-9-]+' | head -1 | cut -d= -f2)
if [[ -z "$CID" || -z "$SRC" || -z "$DST" ]]; then
  echo "Impossible d'extraire CID_A / AID_SRC / AID_DST du seed."
  exit 1
fi

mkdir -p docs/metrics
{
  echo "# Régénéré $(date -Iseconds 2>/dev/null || date)"
  echo "CID_A=$CID"
  echo "AID_SRC=$SRC"
  echo "AID_DST=$DST"
} > docs/metrics/SEED_IDS.txt

echo "=== k6 → NGINX :8082 ==="
k6 run --summary-export=docs/metrics/k6-summary-8082.json \
  -e BASE_URL=http://localhost:8082 \
  -e CID_A="$CID" -e AID_SRC="$SRC" -e AID_DST="$DST" \
  loadtests/canbankx.js

echo "=== k6 → KrakenD :8090 ==="
k6 run --summary-export=docs/metrics/k6-summary-8090.json \
  -e BASE_URL=http://localhost:8090 \
  -e CID_A="$CID" -e AID_SRC="$SRC" -e AID_DST="$DST" \
  loadtests/canbankx.js

echo "Terminé. Mets à jour les tableaux dans docs/RAPPORT_COMPARATIFS.md avec les résumés affichés ci-dessus."

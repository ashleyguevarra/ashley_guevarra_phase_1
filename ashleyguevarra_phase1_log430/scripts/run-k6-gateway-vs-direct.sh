#!/usr/bin/env bash
# Compare deux entrées sous la même charge k6 : NGINX direct vs KrakenD.
# Usage: ./scripts/run-k6-gateway-vs-direct.sh CID_A AID_SRC AID_DST
set -euo pipefail
CID_A="${1:?CID_A}"
AID_SRC="${2:?AID_SRC}"
AID_DST="${3:?AID_DST}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "========== 1) NGINX direct :8082 =========="
BASE_URL=http://localhost:8082 ./scripts/run-k6.sh "$CID_A" "$AID_SRC" "$AID_DST" || true

echo ""
echo "========== 2) KrakenD gateway :8090 =========="
BASE_URL=http://localhost:8090 ./scripts/run-k6.sh "$CID_A" "$AID_SRC" "$AID_DST" || true

echo ""
echo "Renseigner les résumés k6 dans docs/RAPPORT_COMPARATIFS.md (section 2)."

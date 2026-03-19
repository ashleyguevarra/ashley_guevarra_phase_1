#!/bin/bash
# Run k6 load tests
# Usage: ./scripts/run-k6.sh CID_A AID_SRC AID_DST [BASE_URL]
#        BASE_URL=http://localhost:8090 ./scripts/run-k6.sh CID_A AID_SRC AID_DST
# Example: ./scripts/run-k6.sh abc-123 src-uuid dst-uuid
#          BASE_URL=http://localhost:8091 ./scripts/run-k6.sh abc-123 src-uuid dst-uuid  # monolith

BASE="${BASE_URL:-http://localhost:8090}"
CID_A="${1:?Usage: $0 CID_A AID_SRC AID_DST}"
AID_SRC="${2:?Usage: $0 CID_A AID_SRC AID_DST}"
AID_DST="${3:?Usage: $0 CID_A AID_SRC AID_DST}"

cd "$(dirname "$0")/.."
k6 run \
  -e BASE_URL="$BASE" \
  -e CID_A="$CID_A" \
  -e AID_SRC="$AID_SRC" \
  -e AID_DST="$AID_DST" \
  loadtests/canbankx.js

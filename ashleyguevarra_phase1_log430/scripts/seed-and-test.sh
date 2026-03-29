#!/bin/bash
# Seed data and run tests (ledger, transfer, k6)
# Run via: docker exec -i canbankx_nginx bash < scripts/seed-and-test.sh
#   (from project root) - or copy/paste into: docker exec -it canbankx_nginx bash

set -e
# From host against monolith stack (KrakenD) : SEED_GATEWAY_URL=http://localhost:8091 bash scripts/seed-and-test.sh
# Inside Docker (same network as monolith): MONOLITH=1 … ACCT/TRANS → http://monolith:8080
# Micro stack via nginx exec: ACCT/TRANS → http://localhost
if [[ -n "$SEED_GATEWAY_URL" ]]; then
  ACCT="$SEED_GATEWAY_URL"
  TRANS="$SEED_GATEWAY_URL"
elif [[ -n "$MONOLITH" ]]; then
  ACCT="http://monolith:8080"
  TRANS="http://monolith:8080"
else
  ACCT="http://localhost"
  TRANS="http://localhost"
fi

echo "=== 1. Create customer, approve KYC, open 2 accounts ==="
CR=$(curl -s -u admin:admin -X POST "$ACCT/api/v1/customers" -H "Content-Type: application/json" -d "{\"email\":\"load-$(date +%s)@test.com\",\"fullName\":\"LoadTest\"}")
CID=$(echo "$CR" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
[[ -z "$CID" ]] && { echo "Failed: $CR"; exit 1; }
echo "CID=$CID"

curl -s -u admin:admin -X PATCH "$ACCT/api/v1/customers/$CID/kyc/approve" -H "Content-Type: application/json" -d '{}'
ACCS=$(curl -s -u admin:admin -X POST "$ACCT/api/v1/customers/$CID/accounts" -H "Content-Type: application/json" -d '{"type":"CHECKING","currency":"CAD"}')
AID_SRC=$(echo "$ACCS" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
ACCD=$(curl -s -u admin:admin -X POST "$ACCT/api/v1/customers/$CID/accounts" -H "Content-Type: application/json" -d '{"type":"CHECKING","currency":"CAD"}')
AID_DST=$(echo "$ACCD" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "AID_SRC=$AID_SRC AID_DST=$AID_DST"

echo "=== Fund source account (10000 cents = 100 CAD) ==="
curl -s -X POST "$ACCT/internal/seed/fund" -H "Content-Type: application/json" -d "{\"accountId\":\"$AID_SRC\",\"amountCents\":10000}"

echo ""
echo "=== 2. Transfer 1 cent (creates ledger entries) ==="
curl -s -u admin:admin -X POST "$TRANS/api/v1/transfers" \
  -H "Content-Type: application/json" -H "X-Customer-Id: $CID" -H "Idempotency-Key: seed-$(date +%s)" \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"$AID_DST\",\"amountCents\":1}"

echo ""
echo "=== 3. Ledger GET /api/v1/accounts/{id}/ledger ==="
echo "--- AID_SRC (debit entries) ---"
curl -s -u admin:admin -H "X-Customer-Id: $CID" "$ACCT/api/v1/accounts/$AID_SRC/ledger?page=0&size=10"
echo ""
echo "--- AID_DST (credit entries) ---"
curl -s -u admin:admin -H "X-Customer-Id: $CID" "$ACCT/api/v1/accounts/$AID_DST/ledger?page=0&size=10"

echo ""
echo "=== 4. k6 - export and run ==="
echo "CID_A=$CID AID_SRC=$AID_SRC AID_DST=$AID_DST"
if [[ -n "$SEED_GATEWAY_URL" ]]; then
  echo "k6 run --summary-export=docs/metrics/k6-summary-8091.json -e BASE_URL=$SEED_GATEWAY_URL -e CID_A=$CID -e AID_SRC=$AID_SRC -e AID_DST=$AID_DST loadtests/canbankx.js"
else
  echo "BASE_URL=http://host.docker.internal:8082 ./scripts/run-k6.sh $CID $AID_SRC $AID_DST"
fi

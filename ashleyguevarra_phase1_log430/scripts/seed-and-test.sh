#!/bin/bash
# Seed data and run tests (ledger, transfer, k6)
# Run via: docker exec -i canbankx_nginx bash < scripts/seed-and-test.sh
#   (from project root) - or copy/paste into: docker exec -it canbankx_nginx bash

set -e
# For monolith: MONOLITH=1 docker exec canbankx_nginx_monolith bash /tmp/seed-and-test.sh
# Use localhost so nginx proxies to backends (avoids connection refused if services still starting)
if [[ -n "$MONOLITH" ]]; then
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
echo "BASE_URL=http://host.docker.internal:8082 ./scripts/run-k6.sh $CID $AID_SRC $AID_DST"

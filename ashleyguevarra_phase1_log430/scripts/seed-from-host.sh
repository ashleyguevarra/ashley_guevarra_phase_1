#!/bin/bash
# Seed from host via BASE_URL (e.g. http://localhost:8082 for microservices, http://localhost:8091 for monolith)
# Usage: BASE_URL=http://localhost:8082 ./scripts/seed-from-host.sh

set -e
BASE="${BASE_URL:-http://localhost:8082}"
ACCT="$BASE"
TRANS="$BASE"

echo "Waiting for services to be ready (account-service takes ~80s to start)..."
for i in {1..60}; do
  if curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health" 2>/dev/null | grep -q 200; then
    echo "Services ready."
    break
  fi
  [[ $i -eq 60 ]] && { echo "Timeout: services not ready after 120s"; exit 1; }
  sleep 2
done

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
echo "=== 3. k6 - export and run ==="
echo "export CID_A=$CID AID_SRC=$AID_SRC AID_DST=$AID_DST"
echo "BASE_URL=$BASE ./scripts/run-k6.sh \$CID_A \$AID_SRC \$AID_DST"

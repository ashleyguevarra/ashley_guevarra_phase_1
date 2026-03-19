#!/bin/bash
# Test GET /api/v1/accounts/{id}/ledger
# Usage: ./scripts/test-ledger.sh <CUSTOMER_ID> <ACCOUNT_ID> [BASE_URL]
# Example: ./scripts/test-ledger.sh abc-123 4b55cf10-93cd-49a1-84e5-97018d1f1a6f http://localhost:8090

CID="${1:?Usage: $0 CUSTOMER_ID ACCOUNT_ID [BASE_URL]}"
AID="${2:?Usage: $0 CUSTOMER_ID ACCOUNT_ID [BASE_URL]}"
BASE="${3:-http://localhost:8090}"

echo "GET $BASE/api/v1/accounts/$AID/ledger"
curl -s -w "\nHTTP %{http_code}\n" \
  -u admin:admin \
  -H "X-Customer-Id: $CID" \
  "$BASE/api/v1/accounts/$AID/ledger?page=0&size=20" | jq .

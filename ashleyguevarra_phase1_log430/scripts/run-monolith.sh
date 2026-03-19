#!/bin/bash
# Lancer le monolithe (account + transfer dans un seul conteneur)
# Port gateway: 8091, nginx: 8083
# Usage: ./scripts/run-monolith.sh

cd "$(dirname "$0")/.."
docker compose -f docker-compose.monolith.yml up -d --build
echo ""
echo "Monolith running:"
echo "  Gateway: http://localhost:8091"
echo "  Nginx:   http://localhost:8083"
echo ""
echo "Run full flow to seed data, then k6:"
echo "  BASE_URL=http://localhost:8091 CID_A=... AID_SRC=... AID_DST=... ./scripts/run-k6.sh"

# Tests – Ledger, k6, Comparatif Monolithe

## 1. Ledger – GET /api/v1/accounts/{id}/ledger

Après un transfert, les écritures apparaissent sur les comptes source (DEBIT) et destination (CREDIT).

```bash
# Depuis le host (si 8082/8090 fonctionnent) :
curl -s -u admin:admin -H "X-Customer-Id: <CID>" \
  "http://localhost:8090/api/v1/accounts/<AID>/ledger?page=0&size=10"

# Depuis le conteneur nginx (toujours fiable) :
docker exec canbankx_nginx curl -s -u admin:admin -H "X-Customer-Id: <CID>" \
  "http://account-service:8080/api/v1/accounts/<AID>/ledger?page=0&size=10"
```

## 2. k6 – Tests de charge

Utiliser les bons `CID_A`, `AID_SRC`, `AID_DST` obtenus après le seed.

```bash
# Seed data (depuis le projet) :
docker cp scripts/seed-and-test.sh canbankx_nginx:/tmp/seed-and-test.sh
docker exec canbankx_nginx bash /tmp/seed-and-test.sh

# Exporter les IDs et lancer k6 :
export CID_A=<valeur> AID_SRC=<valeur> AID_DST=<valeur>
BASE_URL=http://localhost:8082 ./scripts/run-k6.sh $CID_A $AID_SRC $AID_DST

# Ou en une ligne :
./scripts/run-k6.sh <CID_A> <AID_SRC> <AID_DST)
```

**Note** : k6 utilise `BASE_URL` (défaut `http://host.docker.internal:8082`). Si k6 tourne sur le host, utiliser `http://localhost:8082` ou `http://localhost:8090`.

## 3. Comparatif monolithe

Lancer le monolithe (account + transfer dans un seul conteneur) :

```bash
docker compose -f docker-compose.monolith.yml up -d --build
```

- **Gateway monolithe** : http://localhost:8091  
- **Nginx monolithe** : http://localhost:8083  

Seed puis k6 :

```bash
# Seed (depuis nginx monolithe)
docker cp scripts/seed-and-test.sh canbankx_nginx_monolith:/tmp/
docker exec -e MONOLITH=1 canbankx_nginx_monolith bash /tmp/seed-and-test.sh

# k6 vers le monolithe
BASE_URL=http://localhost:8091 ./scripts/run-k6.sh $CID_A $AID_SRC $AID_DST
```

Comparer les métriques k6 (p95, taux d’erreur) entre microservices (8090/8082) et monolithe (8091).

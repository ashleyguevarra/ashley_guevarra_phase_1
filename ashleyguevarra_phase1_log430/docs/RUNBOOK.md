# Runbook CanBankX (LOG430)

## Démarrage rapide — microservices + LB + gateway

**Astuce :** j’utilise **`-p canbankx_lb`** pour le micro et **`-p canbankx_mono`** pour le monolithe. Sinon les deux `docker-compose.*` dans le même dossier se marchent dessus (même noms `redis`, `nginx`, `gateway` → conteneurs en conflit).

```bash
docker compose -p canbankx_lb -f docker-compose.lb.yml up --build -d
# Avec Prometheus + Grafana (même réseau Docker, cibles scrape OK) :
docker compose -p canbankx_lb -f docker-compose.lb.yml --profile monitoring up --build -d
```

| Service | URL / port |
|---------|----------------|
| NGINX (API) | `http://localhost:8082` |
| KrakenD (Gateway) | `http://localhost:8090` |
| Prometheus (profil monitoring) | `http://localhost:19090` |
| Grafana | `http://localhost:3001` (admin / admin) |
| PostgreSQL account | `localhost:5435` |
| PostgreSQL transfer | `localhost:5436` |
| Redis (hôte, compose LB) | `localhost:16379` → `redis:6379` dans le réseau Docker |

## Seed + identifiants pour k6

```bash
docker cp scripts/seed-and-test.sh canbankx_nginx:/tmp/seed-and-test.sh
docker exec canbankx_nginx bash /tmp/seed-and-test.sh
# Noter CID_A, AID_SRC, AID_DST affichés
```

Auth API : **Basic** `admin` / `admin`. Swagger (hors Docker) : port app locale selon `application.yml`.

## Tests de charge

```bash
BASE_URL=http://localhost:8082 ./scripts/run-k6.sh <CID_A> <AID_SRC> <AID_DST>
./scripts/run-k6-gateway-vs-direct.sh <CID_A> <AID_SRC> <AID_DST>
# Regénérer seed + exports JSON pour le rapport :
./scripts/collect-k6-for-report.sh
```

## Tolérance aux pannes (manuel)

1. Lancer k6 contre `8082` ou `8090`.
2. Dans un autre terminal : `docker stop canbankx_account_b` (ou `canbankx_transfer_b`).
3. Vérifier que les requêtes continuent (répartition sur l’instance restante).
4. `docker start canbankx_account_b`.

## Observabilité

- Dashboard importé automatiquement : **CanBankX — 4 Golden Signals (aperçu)**.
- Si Prometheus tourne **hors** du compose LB, les noms `account-service-a` ne se résolvent pas : utiliser le profil `monitoring` sur le même `docker-compose.lb.yml`.

## Monolithe + métriques

Pour cibler une seule JVM, utiliser `monitoring/prometheus.monolith.yml` (cible `monolith:8080`) sur le réseau du compose monolithe — voir commentaire en tête du fichier.

## CI / build local

```bash
./mvnw -B clean verify
```

Rollback : repasser à une ancienne image / tag, ou `docker compose -p canbankx_lb -f docker-compose.lb.yml down` (idem `canbankx_mono` pour l’autre stack) puis checkout d’une version plus vieille du code.

# Runbook CanBankX (LOG430)

## Démarrage rapide — microservices + LB + gateway

**Important :** utiliser **`-p canbankx_lb`** pour cette stack. Si tu lances aussi `docker-compose.monolith.yml` depuis le même dossier, utilise **`-p canbankx_mono`** pour le monolithe — sinon Compose fusionne les deux fichiers (mêmes noms de services `redis` / `nginx` / `gateway`) et provoque des conflits de noms de conteneurs.

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

Rollback déploiement : rétablir une image ou un tag précédent du conteneur, ou `docker compose -p canbankx_lb -f docker-compose.lb.yml down` (ou `-p canbankx_mono` …) puis monter une version antérieure du dépôt.

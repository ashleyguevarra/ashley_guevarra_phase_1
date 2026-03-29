# CanBankX — LOG430 Phase 1

API bancaire (**Spring Boot**), microservices derrière **NGINX** et **KrakenD**, observabilité **Prometheus / Grafana**, tests de charge **k6**.

## Démarrage

```bash
docker compose -p canbankx_lb -f docker-compose.lb.yml up --build -d
docker compose -p canbankx_lb -f docker-compose.lb.yml --profile monitoring up --build -d   # + Prometheus :19090 + Grafana
```

Monolithe (autre projet Compose) : `docker compose -p canbankx_mono -f docker-compose.monolith.yml up -d`

- **API** : `http://localhost:8082` (NGINX) ou `http://localhost:8090` (gateway)
- **Grafana** : `http://localhost:3001` (admin / admin) — dashboard *4 Golden Signals*
- **Prometheus** : `http://localhost:19090`
- **Runbook** : [`docs/RUNBOOK.md`](docs/RUNBOOK.md)
- **Rapport comparatif (gabarit)** : [`docs/RAPPORT_COMPARATIFS.md`](docs/RAPPORT_COMPARATIFS.md)
- **Documentation longue** : [`docs/README.md`](docs/README.md), **Arc42** : [`docs/arc42.md`](docs/arc42.md)

## Build & tests

```bash
./mvnw -B clean verify
```

## CI

[![CanBankX CI/CD](https://github.com/OWNER/REPO/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci-cd.yml)

Remplace `OWNER/REPO` par ton dépôt GitHub une fois publié.

## CORS

La configuration CORS permet les appels depuis un client local (ex. `localhost:3000`) tout en conservant l’authentification sur les endpoints protégés.

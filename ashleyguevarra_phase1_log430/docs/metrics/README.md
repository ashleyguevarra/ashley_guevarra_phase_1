# Métriques exportées (k6)

- `k6-summary-8082.json` / `k6-summary-8090.json` : sortie de `k6 run --summary-export=...`
- Régénérer : `./scripts/collect-k6-for-report.sh` (depuis la racine du projet) après `docker compose -p canbankx_lb -f docker-compose.lb.yml up -d`

Les UUID dans `SEED_IDS.txt` ne sont valides que tant que les volumes PostgreSQL correspondants existent ; après `docker compose down -v`, relancer un seed et mettre à jour le rapport.

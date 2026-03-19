# CanBankX Frontend

Simple web UI for the CanBankX API.

## Run

1. Start the API (microservices or monolith):
   ```bash
   docker compose -f docker-compose.lb.yml up -d
   ```

2. Serve the frontend on port 3000 (required for CORS):
   ```bash
   npx serve -p 3000 frontend
   ```
   Or from project root:
   ```bash
   cd frontend && npx serve -p 3000 .
   ```

3. Open http://localhost:3000

4. Set API URL to `http://localhost:8082` (nginx) or `http://localhost:8090` (gateway)

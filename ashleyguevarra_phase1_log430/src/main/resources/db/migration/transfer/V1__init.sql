-- Transfer service database: transfers only (no FK to accounts - database per service)

CREATE TABLE transfers (
  id              UUID PRIMARY KEY,
  from_account_id UUID NOT NULL,
  to_account_id   UUID NOT NULL,
  amount_cents    BIGINT NOT NULL,
  status          VARCHAR(30) NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

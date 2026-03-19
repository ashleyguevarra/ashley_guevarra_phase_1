CREATE TABLE customers (
  id            UUID PRIMARY KEY,
  full_name     VARCHAR(200) NOT NULL,
  email         VARCHAR(255) NOT NULL UNIQUE,
  kyc_status    VARCHAR(30)  NOT NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
  id            UUID PRIMARY KEY,
  customer_id   UUID NOT NULL REFERENCES customers(id),
  type          VARCHAR(30) NOT NULL,
  currency      VARCHAR(10) NOT NULL,
  balance_cents BIGINT NOT NULL DEFAULT 0,
  status        VARCHAR(30) NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transfers (
  id              UUID PRIMARY KEY,
  from_account_id UUID NOT NULL REFERENCES accounts(id),
  to_account_id   UUID NOT NULL REFERENCES accounts(id),
  amount_cents    BIGINT NOT NULL,
  status          VARCHAR(30) NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
  id          UUID PRIMARY KEY,
  action      VARCHAR(80) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id   UUID NOT NULL,
  payload     TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);

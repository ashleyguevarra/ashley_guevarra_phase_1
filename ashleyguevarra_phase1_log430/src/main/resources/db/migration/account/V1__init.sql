-- Account service database: customers, accounts, ledger, audit, saga steps

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

CREATE TABLE audit_log (
  id          UUID PRIMARY KEY,
  action      VARCHAR(80) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id   UUID NOT NULL,
  payload     TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ledger_entries (
  id           UUID PRIMARY KEY,
  account_id   UUID NOT NULL REFERENCES accounts(id),
  direction    VARCHAR(10) NOT NULL,
  amount_cents BIGINT NOT NULL,
  description  VARCHAR(255),
  transfer_id  UUID,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE saga_steps (
  id         UUID PRIMARY KEY,
  transfer_id UUID NOT NULL,
  step_type   VARCHAR(80) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (transfer_id, step_type)
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);
CREATE UNIQUE INDEX uq_ledger_transfer_direction ON ledger_entries (transfer_id, direction) WHERE transfer_id IS NOT NULL;

-- UC-04: Consultation du solde et de l'historique
CREATE TABLE IF NOT EXISTS ledger_entries (
  id           UUID PRIMARY KEY,
  account_id   UUID NOT NULL REFERENCES accounts(id),
  direction    VARCHAR(10) NOT NULL,     -- CREDIT / DEBIT
  amount_cents BIGINT NOT NULL,
  description  VARCHAR(255),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ledger_account_created
ON ledger_entries(account_id, created_at DESC);

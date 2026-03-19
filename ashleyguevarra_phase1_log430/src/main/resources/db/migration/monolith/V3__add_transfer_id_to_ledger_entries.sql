-- Add transfer_id to ledger entries so ledger writes become effectively-once
-- (idempotent retries protected by a unique constraint).

ALTER TABLE ledger_entries
ADD COLUMN IF NOT EXISTS transfer_id UUID;

-- Enforce idempotency per (transfer_id, direction) when transfer_id is present.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_transfer_direction
ON ledger_entries (transfer_id, direction)
WHERE transfer_id IS NOT NULL;

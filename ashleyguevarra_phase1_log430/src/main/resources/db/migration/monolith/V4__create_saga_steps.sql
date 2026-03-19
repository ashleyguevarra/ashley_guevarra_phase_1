-- Idempotency for saga steps across services.
-- Ensures the same step for a given transfer can be applied only once.

CREATE TABLE IF NOT EXISTS saga_steps (
  id UUID PRIMARY KEY,
  transfer_id UUID NOT NULL REFERENCES transfers(id),
  step_type VARCHAR(80) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (transfer_id, step_type)
);

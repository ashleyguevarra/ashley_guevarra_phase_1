-- V5's unique constraint on (action, entity_type, entity_id) can fail if
-- duplicate audit entries exist (e.g. from retries or legacy data).
-- Drop it to avoid insert failures; idempotency is still enforced via
-- transfer idempotency key and ledger constraints.
DROP INDEX IF EXISTS uq_audit_log_action_entity;

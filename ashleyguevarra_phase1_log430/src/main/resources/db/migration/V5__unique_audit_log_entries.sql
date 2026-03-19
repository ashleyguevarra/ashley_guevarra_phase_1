-- Make audit log writes effectively-once per (action, entity_type, entity_id)
-- so retries don't duplicate compliance logs.

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_log_action_entity
ON audit_log (action, entity_type, entity_id);


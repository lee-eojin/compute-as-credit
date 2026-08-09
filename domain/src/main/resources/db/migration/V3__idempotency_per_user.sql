ALTER TABLE idempotency_keys ADD COLUMN user_id BIGINT NULL AFTER scope;

ALTER TABLE idempotency_keys DROP INDEX idem_key;

ALTER TABLE idempotency_keys ADD UNIQUE KEY uk_idempotency_scope_user (idem_key, scope, user_id);

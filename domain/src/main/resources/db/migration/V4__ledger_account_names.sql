-- Without a name, balance and hold resolved to the same (user_id, type) row, so every hold
-- debited and credited one account and reserved nothing.
ALTER TABLE ledger_accounts ADD COLUMN name VARCHAR(40) NULL AFTER type;

UPDATE ledger_accounts SET name = 'balance' WHERE name IS NULL AND type = 'LIABILITY';
UPDATE ledger_accounts SET name = 'revenue' WHERE name IS NULL AND type = 'REVENUE';

ALTER TABLE ledger_accounts ADD UNIQUE KEY uk_ledger_accounts_user_type_name (user_id, type, name);

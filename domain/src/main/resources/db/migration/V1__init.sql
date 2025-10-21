CREATE TABLE IF NOT EXISTS jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NULL,
  agent_spec JSON,
  resource_hint JSON,
  max_budget DECIMAL(18,6),
  status ENUM('SUBMITTED','QUEUED','PROVISIONING','RUNNING','SUCCEEDED','FAILED','CANCELLED') NOT NULL,
  created_at DATETIME,
  started_at DATETIME,
  ended_at DATETIME
);

CREATE TABLE IF NOT EXISTS providers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80),
  region VARCHAR(60),
  status VARCHAR(20),
  pricing_json JSON,
  metrics_json JSON,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS ledger_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  type ENUM('ASSET','LIABILITY','REVENUE','EXPENSE'),
  currency CHAR(3) NOT NULL DEFAULT 'USD'
);
CREATE TABLE IF NOT EXISTS ledger_entries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT NULL,
  kind ENUM('HOLD','DEBIT','REFUND','CHARGE') NOT NULL,
  created_at DATETIME NOT NULL,
  idempotency_key VARCHAR(64) UNIQUE
);
CREATE TABLE IF NOT EXISTS ledger_postings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  entry_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  side ENUM('DEBIT','CREDIT') NOT NULL,
  amount DECIMAL(18,6) NOT NULL
);


CREATE TABLE IF NOT EXISTS outbox_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type VARCHAR(80) NOT NULL,
  aggregate_type VARCHAR(80),
  aggregate_id BIGINT,
  correlation_id VARCHAR(64),
  payload JSON,
  created_at DATETIME NOT NULL,
  processed_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  idem_key VARCHAR(64) UNIQUE NOT NULL,
  scope VARCHAR(64) NOT NULL,
  job_id BIGINT NULL,
  created_at DATETIME NOT NULL
);

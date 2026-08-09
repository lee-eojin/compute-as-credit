ALTER TABLE providers ADD UNIQUE KEY uk_providers_name (name);

INSERT IGNORE INTO providers (name, region, status) VALUES
  ('FakeProviderClient', NULL, 'ACTIVE'),
  ('RunPodClient', NULL, 'ACTIVE');

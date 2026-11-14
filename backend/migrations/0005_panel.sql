-- Web identity extends existing users. Existing rows remain unclaimed.
ALTER TABLE users
  ADD COLUMN display_name TEXT NOT NULL DEFAULT '',
  ADD COLUMN panel_role TEXT NOT NULL DEFAULT 'user'
    CHECK (panel_role IN ('user', 'support', 'admin', 'owner')),
  ADD COLUMN panel_active BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN panel_primary BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN panel_managed BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN panel_password BYTEA,
  ADD CONSTRAINT panel_managed_credentials CHECK (panel_password IS NULL OR panel_managed),
  ADD CONSTRAINT panel_primary_active_owner
    CHECK (NOT panel_primary OR (panel_active AND panel_role = 'owner'));

CREATE UNIQUE INDEX panel_single_primary ON users (panel_primary) WHERE panel_primary;
CREATE INDEX panel_properties_owner ON properties (owner_user_id, created_at DESC);

CREATE TABLE panel_access_links (
  token_hash BYTEA PRIMARY KEY,
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE panel_sessions (
  token_hash BYTEA PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX panel_sessions_user ON panel_sessions (user_id);
CREATE INDEX panel_sessions_expiry ON panel_sessions (expires_at);

CREATE TABLE panel_login_limits (
  key_hash BYTEA PRIMARY KEY,
  attempts INTEGER NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX panel_login_limits_expiry ON panel_login_limits (expires_at);

CREATE TABLE panel_role_permissions (
  role TEXT PRIMARY KEY CHECK (role IN ('admin', 'support')),
  permissions TEXT[] NOT NULL,
  CHECK (permissions <@ ARRAY['accounts.read','accounts.write','reports.read','audit.read']::TEXT[]),
  CHECK (role <> 'support' OR NOT ('accounts.write' = ANY(permissions))),
  CHECK (NOT ('accounts.write' = ANY(permissions)) OR ('accounts.read' = ANY(permissions)))
);
INSERT INTO panel_role_permissions (role, permissions) VALUES
  ('admin', ARRAY['accounts.read','accounts.write','reports.read','audit.read']),
  ('support', ARRAY['accounts.read','reports.read']);

CREATE TABLE panel_report_access (
  report_id UUID PRIMARY KEY REFERENCES reports(id) ON DELETE CASCADE,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE panel_audit (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor_id UUID NOT NULL REFERENCES users(id),
  event TEXT NOT NULL,
  subject_id UUID,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

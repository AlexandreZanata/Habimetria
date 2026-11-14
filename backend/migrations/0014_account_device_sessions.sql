CREATE TABLE account_device_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  device_id UUID NOT NULL,
  platform TEXT NOT NULL CHECK (platform IN ('android','ios')),
  label TEXT NOT NULL CHECK (char_length(label) BETWEEN 1 AND 80),
  created_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
  last_used_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp()+interval '30 days',
  generation INTEGER NOT NULL DEFAULT 0 CHECK (generation BETWEEN 0 AND 8192),
  revoked_at TIMESTAMPTZ,
  revoke_reason TEXT CHECK (revoke_reason IN ('logout','replaced','identity_changed','password_reset','refresh_reuse','expired','rotation_limit')),
  UNIQUE(id,user_id),
  CHECK ((revoked_at IS NULL)=(revoke_reason IS NULL)),
  CHECK (expires_at>created_at AND expires_at<=created_at+interval '30 days')
);
CREATE UNIQUE INDEX account_device_live ON account_device_sessions(user_id,device_id) WHERE revoked_at IS NULL;
CREATE INDEX account_device_expiry ON account_device_sessions(expires_at,id);

CREATE TABLE account_refresh_tokens (
  token_hash BYTEA PRIMARY KEY CHECK (octet_length(token_hash)=32),
  session_id UUID NOT NULL REFERENCES account_device_sessions(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
  used_at TIMESTAMPTZ
);
CREATE INDEX account_refresh_family ON account_refresh_tokens(session_id);
CREATE UNIQUE INDEX account_refresh_current ON account_refresh_tokens(session_id) WHERE used_at IS NULL;
ALTER TABLE account_access_sessions ADD COLUMN family_id UUID,
  ADD CONSTRAINT account_access_family FOREIGN KEY(family_id,user_id)
    REFERENCES account_device_sessions(id,user_id) ON DELETE CASCADE;
CREATE INDEX account_access_family_index ON account_access_sessions(family_id) WHERE family_id IS NOT NULL;

CREATE FUNCTION account_device_revoked() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM account_access_sessions WHERE family_id=NEW.id;
  RETURN NEW;
END $$;
CREATE TRIGGER account_device_revoke_access AFTER UPDATE OF revoked_at ON account_device_sessions
  FOR EACH ROW WHEN (NEW.revoked_at IS NOT NULL) EXECUTE FUNCTION account_device_revoked();

CREATE FUNCTION account_revoke_devices() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  UPDATE account_device_sessions SET revoked_at=clock_timestamp(),revoke_reason='identity_changed'
    WHERE user_id=NEW.id AND revoked_at IS NULL;
  RETURN NEW;
END $$;
CREATE TRIGGER account_device_identity_changed AFTER UPDATE OF panel_active,panel_role,panel_password,email ON users
  FOR EACH ROW WHEN (OLD.panel_active IS DISTINCT FROM NEW.panel_active
    OR OLD.panel_role IS DISTINCT FROM NEW.panel_role OR OLD.panel_password IS DISTINCT FROM NEW.panel_password
    OR OLD.email IS DISTINCT FROM NEW.email) EXECUTE FUNCTION account_revoke_devices();

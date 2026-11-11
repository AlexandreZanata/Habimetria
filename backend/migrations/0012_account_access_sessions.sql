-- C10 short-lived API authority; device refresh families are introduced in C12.
CREATE TABLE account_access_sessions (
  token_hash BYTEA PRIMARY KEY CHECK (octet_length(token_hash)=32),
  user_id UUID NOT NULL REFERENCES users(id),
  audience TEXT NOT NULL DEFAULT 'account_api' CHECK (audience='account_api'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT statement_timestamp()+interval '15 minutes',
  CHECK (expires_at>created_at AND expires_at<=created_at+interval '15 minutes')
);
CREATE INDEX account_access_sessions_user ON account_access_sessions(user_id);
CREATE INDEX account_access_sessions_expiry ON account_access_sessions(expires_at);

CREATE FUNCTION account_revoke_access() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM account_access_sessions WHERE user_id=NEW.id;
  RETURN NEW;
END $$;
CREATE TRIGGER account_revoke_changed_identity AFTER UPDATE OF panel_active,panel_role,panel_password ON users
  FOR EACH ROW WHEN (OLD.panel_active IS DISTINCT FROM NEW.panel_active
    OR OLD.panel_role IS DISTINCT FROM NEW.panel_role
    OR OLD.panel_password IS DISTINCT FROM NEW.panel_password)
  EXECUTE FUNCTION account_revoke_access();

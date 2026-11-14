CREATE TABLE account_password_recoveries (
  user_id UUID PRIMARY KEY REFERENCES users(id),
  token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash)=32),
  purpose TEXT NOT NULL DEFAULT 'password_recovery' CHECK (purpose='password_recovery'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT now()+interval '30 minutes',
  CHECK (expires_at>created_at AND expires_at<=created_at+interval '30 minutes')
);

ALTER TABLE account_mail_outbox
  ALTER COLUMN token_hash DROP NOT NULL,
  DROP CONSTRAINT account_mail_outbox_template_check,
  DROP CONSTRAINT account_mail_payload_state,
  ADD COLUMN recipient_digest BYTEA,
  ADD CONSTRAINT account_mail_template CHECK (template IN ('email_verification','password_recovery','password_changed')),
  ADD CONSTRAINT account_mail_capability CHECK (
    (template='password_changed' AND token_hash IS NULL AND recipient_digest IS NOT NULL AND octet_length(recipient_digest)=32)
    OR (template IN ('email_verification','password_recovery') AND token_hash IS NOT NULL AND recipient_digest IS NULL)),
  ADD CONSTRAINT account_mail_payload_state CHECK (
    (state IN ('pending','sending') AND completed_at IS NULL AND
      ((template='password_changed' AND proof_ciphertext IS NULL) OR
       (template<>'password_changed' AND proof_ciphertext IS NOT NULL AND octet_length(proof_ciphertext)=61)))
    OR (state IN ('sent','failed','cancelled') AND proof_ciphertext IS NULL AND completed_at IS NOT NULL));

CREATE FUNCTION account_invalidate_recovery() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM account_password_recoveries WHERE user_id=NEW.id;
  UPDATE account_mail_outbox SET state='cancelled',proof_ciphertext=NULL,
    completed_at=clock_timestamp(),failure_code='ineligible',lease_id=NULL,lease_until=NULL
    WHERE user_id=NEW.id AND template='password_recovery' AND state IN ('pending','sending');
  RETURN NEW;
END $$;
CREATE TRIGGER account_recovery_identity_changed AFTER UPDATE OF panel_active,panel_role,panel_password,email ON users
  FOR EACH ROW WHEN (OLD.panel_active IS DISTINCT FROM NEW.panel_active
    OR OLD.panel_role IS DISTINCT FROM NEW.panel_role
    OR OLD.panel_password IS DISTINCT FROM NEW.panel_password OR OLD.email IS DISTINCT FROM NEW.email)
  EXECUTE FUNCTION account_invalidate_recovery();

ALTER TABLE panel_audit DROP CONSTRAINT panel_audit_known_event,
  ADD CONSTRAINT panel_audit_known_event CHECK (event IN (
    'account_created','account_updated','access_issued','permissions_updated',
    'signed_in','profile_updated','legacy_property_assigned','email_verified','password_reset')) NOT VALID;

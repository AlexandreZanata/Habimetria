ALTER TABLE users
  ADD COLUMN registration_started_at TIMESTAMPTZ,
  ADD COLUMN email_verified_at TIMESTAMPTZ,
  ADD CONSTRAINT account_pending_identity CHECK (registration_started_at IS NULL OR email_verified_at IS NOT NULL
    OR (panel_role='user' AND NOT panel_active AND panel_managed AND panel_password IS NULL AND NOT panel_primary)),
  ADD CONSTRAINT account_verification_order CHECK (registration_started_at IS NULL OR email_verified_at IS NULL
    OR email_verified_at >= registration_started_at);

CREATE TABLE account_verifications (
  user_id UUID PRIMARY KEY REFERENCES users(id),
  token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash)=32),
  purpose TEXT NOT NULL DEFAULT 'email_verification' CHECK (purpose='email_verification'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT now()+interval '30 minutes',
  CHECK (expires_at>created_at AND expires_at<=created_at+interval '30 minutes')
);

CREATE TABLE account_mail_outbox (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash)=32),
  template TEXT NOT NULL DEFAULT 'email_verification' CHECK (template='email_verification'),
  proof_ciphertext BYTEA,
  state TEXT NOT NULL DEFAULT 'pending' CHECK (state IN ('pending','sent','failed','cancelled')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ,
  failure_code TEXT CHECK (failure_code IN ('transport','rejected','tls','invalid_proof','expired','replaced','ineligible')),
  CHECK ((state='pending' AND octet_length(proof_ciphertext)=61 AND proof_ciphertext IS NOT NULL AND completed_at IS NULL)
    OR (state<>'pending' AND proof_ciphertext IS NULL AND completed_at IS NOT NULL))
);
CREATE INDEX account_mail_due ON account_mail_outbox(next_attempt_at,id) WHERE state='pending';

CREATE TABLE account_registration_limits (
  key_hash BYTEA PRIMARY KEY CHECK (octet_length(key_hash)=32),
  attempts INTEGER NOT NULL CHECK (attempts>0 AND attempts<=10),
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX account_registration_limits_expiry ON account_registration_limits(expires_at);

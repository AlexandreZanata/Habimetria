ALTER TABLE account_mail_outbox
  DROP CONSTRAINT account_mail_outbox_state_check,
  DROP CONSTRAINT account_mail_outbox_check,
  DROP CONSTRAINT account_mail_outbox_failure_code_check,
  ADD COLUMN lease_id UUID,
  ADD COLUMN lease_until TIMESTAMPTZ,
  ADD CONSTRAINT account_mail_state CHECK (state IN ('pending','sending','sent','failed','cancelled')),
  ADD CONSTRAINT account_mail_failure_code CHECK (failure_code IN
    ('transport','rejected','tls','invalid_proof','expired','replaced','ineligible','attempt_limit')),
  ADD CONSTRAINT account_mail_payload_state CHECK (
    (state IN ('pending','sending') AND proof_ciphertext IS NOT NULL AND octet_length(proof_ciphertext)=61 AND completed_at IS NULL)
    OR (state IN ('sent','failed','cancelled') AND proof_ciphertext IS NULL AND completed_at IS NOT NULL)),
  ADD CONSTRAINT account_mail_lease_state CHECK (
    (state='sending' AND lease_id IS NOT NULL AND lease_until IS NOT NULL)
    OR (state<>'sending' AND lease_id IS NULL AND lease_until IS NULL));
CREATE INDEX account_mail_reclaim ON account_mail_outbox(lease_until,id) WHERE state='sending';

-- Add only a minimal self-owned fact; no proof, email or credential in audit.
ALTER TABLE panel_audit DROP CONSTRAINT panel_audit_known_event,
  ADD CONSTRAINT panel_audit_known_event CHECK (event IN (
    'account_created','account_updated','access_issued','permissions_updated',
    'signed_in','profile_updated','legacy_property_assigned','email_verified')) NOT VALID;

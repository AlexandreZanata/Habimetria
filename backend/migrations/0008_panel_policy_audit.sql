-- Preserve old events without invented reasons, operation identities or states.
ALTER TABLE panel_audit
  ADD COLUMN operation_id UUID UNIQUE,
  ADD COLUMN reason_code TEXT,
  ADD COLUMN before_role TEXT,
  ADD COLUMN after_role TEXT,
  ADD COLUMN before_active BOOLEAN,
  ADD COLUMN after_active BOOLEAN,
  ADD COLUMN before_permissions TEXT[],
  ADD COLUMN after_permissions TEXT[];

CREATE FUNCTION panel_audit_permissions_valid(role TEXT, permissions TEXT[]) RETURNS BOOLEAN
LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
  SELECT role IN ('admin','support') AND permissions <@ ARRAY[
    'accounts.read','accounts.write','reports.read','audit.read','commerce.read',
    'catalog.write','pricing.review','pricing.publish','grants.write','finance.read',
    'refunds.request','events.replay','support.write','privacy.process','operations.write'
  ]::TEXT[] AND panel_distinct_permissions(permissions)
  AND (NOT ('accounts.write'=ANY(permissions)) OR 'accounts.read'=ANY(permissions))
  AND (role <> 'support' OR permissions <@ ARRAY[
    'accounts.read','reports.read','audit.read','commerce.read','support.write'
  ]::TEXT[])
$$;

ALTER TABLE panel_audit
  ADD CONSTRAINT panel_audit_known_event CHECK (event IN (
    'account_created','account_updated','access_issued','permissions_updated',
    'signed_in','profile_updated','legacy_property_assigned')) NOT VALID,
  ADD CONSTRAINT panel_audit_closed_reason CHECK (reason_code IS NULL OR reason_code IN (
    'access_review','security_response','account_request','account_onboarding','activation_delivery')),
  ADD CONSTRAINT panel_audit_before_role CHECK (before_role IS NULL OR before_role IN ('owner','admin','support','user')),
  ADD CONSTRAINT panel_audit_after_role CHECK (after_role IS NULL OR after_role IN ('owner','admin','support','user')),
  ADD CONSTRAINT panel_audit_before_permissions CHECK (before_permissions IS NULL OR
    (before_role IS NOT NULL AND panel_audit_permissions_valid(before_role,before_permissions))),
  ADD CONSTRAINT panel_audit_after_permissions CHECK (after_permissions IS NULL OR
    (after_role IS NOT NULL AND panel_audit_permissions_valid(after_role,after_permissions))),
  ADD CONSTRAINT panel_audit_policy_details CHECK (event NOT IN (
    'account_created','account_updated','access_issued','permissions_updated') OR (
    operation_id IS NOT NULL AND reason_code IS NOT NULL AND after_role IS NOT NULL
    AND CASE event
      WHEN 'permissions_updated' THEN subject_id IS NULL AND before_role=after_role
        AND before_role IS NOT NULL AND before_permissions IS NOT NULL AND after_permissions IS NOT NULL
        AND before_active IS NULL AND after_active IS NULL
        AND reason_code IN ('access_review','security_response','account_request')
      WHEN 'account_created' THEN subject_id IS NOT NULL AND before_role IS NULL
        AND before_active IS NULL AND after_active IS NOT NULL AND after_active
        AND before_permissions IS NULL AND after_permissions IS NULL AND reason_code='account_onboarding'
      ELSE subject_id IS NOT NULL AND before_role IS NOT NULL AND before_active IS NOT NULL
        AND after_active IS NOT NULL AND before_permissions IS NULL AND after_permissions IS NULL
        AND ((event='access_issued' AND reason_code='activation_delivery') OR
             (event='account_updated' AND reason_code IN ('access_review','security_response','account_request')))
    END)) NOT VALID;

CREATE FUNCTION panel_audit_immutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'audit rows are immutable';
END $$;
CREATE TRIGGER panel_audit_no_rewrite BEFORE UPDATE OR DELETE ON panel_audit
  FOR EACH ROW EXECUTE FUNCTION panel_audit_immutable();
CREATE TRIGGER panel_audit_no_truncate BEFORE TRUNCATE ON panel_audit
  FOR EACH STATEMENT EXECUTE FUNCTION panel_audit_immutable();

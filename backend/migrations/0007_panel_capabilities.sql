-- Keep existing grants. Newly registered powers require explicit owner action.
ALTER TABLE panel_role_permissions
  DROP CONSTRAINT panel_role_permissions_permissions_check,
  DROP CONSTRAINT panel_role_permissions_check,
  ADD CONSTRAINT panel_known_capabilities CHECK (permissions <@ ARRAY[
    'accounts.read','accounts.write','reports.read','audit.read','commerce.read',
    'catalog.write','pricing.review','pricing.publish','grants.write','finance.read',
    'refunds.request','events.replay','support.write','privacy.process','operations.write'
  ]::TEXT[]),
  ADD CONSTRAINT panel_support_capabilities CHECK (role <> 'support' OR permissions <@ ARRAY[
    'accounts.read','reports.read','audit.read','commerce.read','support.write'
  ]::TEXT[]);

CREATE FUNCTION panel_distinct_permissions(permissions TEXT[]) RETURNS BOOLEAN
LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS $$
  SELECT cardinality(permissions) = count(DISTINCT permission)
    FROM unnest(permissions) AS permission
$$;
ALTER TABLE panel_role_permissions
  ADD CONSTRAINT panel_distinct_capabilities CHECK (panel_distinct_permissions(permissions));

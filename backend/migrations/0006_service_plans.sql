-- No commercial amounts or provider products are seeded.
CREATE FUNCTION service_plan_allowances_v1(value JSONB) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE item JSONB; seen TEXT[] := '{}'; capability TEXT; unit_name TEXT; window_name TEXT; amount NUMERIC;
BEGIN
  IF jsonb_typeof(value) <> 'array' OR octet_length(value::text) > 8192 THEN RETURN false; END IF;
  IF jsonb_array_length(value) NOT BETWEEN 1 AND 4 THEN RETURN false; END IF;
  FOR item IN SELECT * FROM jsonb_array_elements(value) LOOP
    IF jsonb_typeof(item) <> 'object' OR NOT item ?& ARRAY['capability','unit','window','limit']
      OR item - ARRAY['capability','unit','window','limit'] <> '{}'::jsonb THEN RETURN false; END IF;
    IF jsonb_typeof(item->'capability') <> 'string' OR jsonb_typeof(item->'unit') <> 'string'
      OR jsonb_typeof(item->'window') <> 'string' OR jsonb_typeof(item->'limit') <> 'number' THEN RETURN false; END IF;
    capability := item->>'capability'; unit_name := item->>'unit'; window_name := item->>'window';
    IF capability = ANY(seen) THEN RETURN false; END IF;
    seen := array_append(seen, capability);
    IF NOT ((capability='cloud_backup' AND unit_name='bytes' AND window_name='retained')
      OR (capability IN ('cross_device_sync','hosted_share') AND unit_name='bytes' AND window_name='subscription_month')
      OR (capability='web_library' AND unit_name='requests' AND window_name='subscription_month')) THEN RETURN false; END IF;
    IF item->>'limit' !~ '^[1-9][0-9]{0,18}$' THEN RETURN false; END IF;
    amount := (item->>'limit')::numeric;
    IF amount <= 0 OR amount > 9223372036854775807 OR amount <> trunc(amount) THEN RETURN false; END IF;
  END LOOP;
  RETURN true;
END $$;

CREATE TABLE service_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT NOT NULL UNIQUE CHECK (code ~ '^[a-z][a-z0-9_-]{2,63}$'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  retired_at TIMESTAMPTZ
);

CREATE TABLE service_plan_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  plan_id UUID NOT NULL REFERENCES service_plans(id) ON DELETE RESTRICT,
  version_number INTEGER NOT NULL CHECK (version_number > 0),
  name TEXT NOT NULL CHECK (name=btrim(name) AND char_length(name) BETWEEN 3 AND 120 AND name !~ '[[:cntrl:]]'),
  billing_period TEXT NOT NULL CHECK (billing_period='monthly'),
  subject_scope TEXT NOT NULL CHECK (subject_scope='individual'),
  format_version SMALLINT NOT NULL CHECK (format_version=1),
  allowances JSONB NOT NULL CHECK (service_plan_allowances_v1(allowances)),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(plan_id,version_number)
);

CREATE FUNCTION guard_service_plan_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP='INSERT' THEN
    PERFORM 1 FROM service_plans WHERE id=NEW.plan_id AND retired_at IS NULL FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'active service plan required' USING ERRCODE='23514'; END IF;
    RETURN NEW;
  END IF;
  RAISE EXCEPTION 'service plan versions are immutable' USING ERRCODE='55000';
END $$;
CREATE TRIGGER service_plan_version_rows BEFORE INSERT OR UPDATE OR DELETE ON service_plan_versions
FOR EACH ROW EXECUTE FUNCTION guard_service_plan_version();
CREATE TRIGGER service_plan_version_truncate BEFORE TRUNCATE ON service_plan_versions
FOR EACH STATEMENT EXECUTE FUNCTION guard_service_plan_version();

CREATE FUNCTION guard_service_plan_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP='UPDATE' THEN
    IF NEW.id=OLD.id AND NEW.code=OLD.code AND NEW.created_at=OLD.created_at
      AND OLD.retired_at IS NULL AND NEW.retired_at IS NOT NULL THEN
      NEW.retired_at := clock_timestamp();
      RETURN NEW;
    END IF;
  END IF;
  RAISE EXCEPTION 'service plan identity and retirement are immutable' USING ERRCODE='55000';
END $$;
CREATE TRIGGER service_plan_identity_rows BEFORE UPDATE OR DELETE ON service_plans
FOR EACH ROW EXECUTE FUNCTION guard_service_plan_identity();
CREATE TRIGGER service_plan_identity_truncate BEFORE TRUNCATE ON service_plans
FOR EACH STATEMENT EXECUTE FUNCTION guard_service_plan_identity();

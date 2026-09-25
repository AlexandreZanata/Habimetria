-- 0001_init: MVP 0.1 core (entry-ready, exit-ready, occurrences-ready)
-- Postgres 18.6-alpine. One version per file.

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email CITEXT UNIQUE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE properties (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID REFERENCES users(id),
  address TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenancies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
  type TEXT NOT NULL DEFAULT 'entry' CHECK (type IN ('entry','exit')),
  cap_photos INT NOT NULL DEFAULT 300,
  cap_videos INT NOT NULL DEFAULT 30,
  cap_bytes BIGINT NOT NULL DEFAULT 2147483648,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inspections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenancy_id UUID NOT NULL REFERENCES tenancies(id) ON DELETE CASCADE,
  kind TEXT NOT NULL DEFAULT 'entry' CHECK (kind IN ('entry','exit')),
  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SEALED')),
  sealed_at TIMESTAMPTZ,
  manifest_sha256 TEXT,
  report_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT sealed_needs_fields CHECK (
    status = 'DRAFT' OR (sealed_at IS NOT NULL AND manifest_sha256 IS NOT NULL)
  )
);

CREATE TABLE rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
  key TEXT NOT NULL,
  label TEXT NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  UNIQUE (inspection_id, key)
);

CREATE TABLE evidences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
  room_id UUID REFERENCES rooms(id) ON DELETE SET NULL,
  type TEXT NOT NULL CHECK (type IN ('photo','video','text','doc')),
  mime TEXT NOT NULL,
  size BIGINT NOT NULL CHECK (size > 0),
  storage_key TEXT NOT NULL,
  sha256 TEXT NOT NULL,
  captured_at_device TIMESTAMPTZ,
  received_at_server TIMESTAMPTZ NOT NULL DEFAULT now(),
  sequence INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (inspection_id, sequence)
);

CREATE TABLE findings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  evidence_id UUID NOT NULL REFERENCES evidences(id) ON DELETE CASCADE,
  preset TEXT NOT NULL CHECK (preset IN ('stain','scratch','crack','broken','moisture','hole','other')),
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  inspection_id UUID NOT NULL UNIQUE REFERENCES inspections(id) ON DELETE CASCADE,
  verify_code TEXT NOT NULL UNIQUE,
  report_sha256 TEXT NOT NULL,
  pdf_key TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE occurrences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  body TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE occurrence_evidences (
  occurrence_id UUID NOT NULL REFERENCES occurrences(id) ON DELETE CASCADE,
  evidence_id UUID NOT NULL REFERENCES evidences(id) ON DELETE CASCADE,
  PRIMARY KEY (occurrence_id, evidence_id)
);

CREATE TABLE shares (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
  channel TEXT NOT NULL CHECK (channel IN ('whatsapp','email','link')),
  token TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE access_logs (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  share_id UUID NOT NULL REFERENCES shares(id) ON DELETE CASCADE,
  event TEXT NOT NULL CHECK (event IN ('created','accessed','downloaded')),
  ip_hash TEXT,
  at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidences_inspection ON evidences(inspection_id);
CREATE INDEX idx_rooms_inspection ON rooms(inspection_id);

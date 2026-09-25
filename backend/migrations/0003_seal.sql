-- 0003_seal: manifest vault + nullable report artifact columns.
-- The manifest is the trust anchor at seal time. The final PDF hash
-- (report_sha256) and pdf_key are filled by the two-pass app upload (S7).

CREATE TABLE manifests (
  inspection_id UUID PRIMARY KEY REFERENCES inspections(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  sha256 TEXT NOT NULL,
  signature TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE reports ALTER COLUMN report_sha256 DROP NOT NULL;
ALTER TABLE reports ALTER COLUMN pdf_key DROP NOT NULL;

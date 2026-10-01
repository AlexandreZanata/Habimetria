-- 0004_comparisons: user-confirmed entry-vs-exit tags (V1.3 sync).
-- Tags describe visual state only, never liability. One tag per exit
-- position: retags overwrite the row, history stays on device.

CREATE TABLE comparisons (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenancy_id UUID NOT NULL REFERENCES tenancies(id) ON DELETE CASCADE,
  entry_room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  exit_room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  shot_category TEXT NOT NULL,
  entry_evidence_id UUID REFERENCES evidences(id) ON DELETE SET NULL,
  exit_evidence_id UUID REFERENCES evidences(id) ON DELETE SET NULL,
  tag TEXT NOT NULL CHECK (tag IN ('no_change','changed','wear')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (exit_room_id, shot_category)
);

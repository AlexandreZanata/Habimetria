-- 0002_geo: device-reported geolocation claims on evidences.
-- Additive and safe. Server never vouches for these values;
-- see docs/03-architecture/client-processing.md.

ALTER TABLE evidences
  ADD COLUMN latitude DOUBLE PRECISION
    CHECK (latitude IS NULL OR (latitude BETWEEN -90 AND 90)),
  ADD COLUMN longitude DOUBLE PRECISION
    CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180));

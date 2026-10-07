#!/bin/sh
# Audit pack: reruns the code gates and stamps versions into a timestamped
# evidence bundle for controladoria. Fails closed: any gate failure aborts
# the pack. Packs are build artifacts, never committed (see .gitignore).
set -eu
cd "$(dirname "$0")"
sha=$(git rev-parse --short HEAD)
stamp=$(date -u +%Y%m%dT%H%M%SZ)
out="audit-pack-${sha}-${stamp}.txt"
{
  echo "commit: $(git rev-parse HEAD)"
  echo "branch: $(git branch --show-current)"
  echo "date-utc: $stamp"
  echo "--- make verify ---"
  make verify
  echo "--- make cover ---"
  # Local note: if the default toolchain lacks covdata, rerun this step as
  # GOTOOLCHAIN=go1.25.13 ./check_coverage.sh inside backend/.
  (cd backend && ./check_coverage.sh)
  echo "--- make secrets ---"
  make secrets
  echo "--- fixture verifier ---"
  (cd backend && go run ./cmd/verify --format json testdata/fixture-package)
  echo "--- migrations ---"
  ls backend/migrations/*.sql
  echo "--- mobile: run make verify-mobile separately (Gradle, ~8 min) ---"
} > "$out" 2>&1
echo "wrote $out"

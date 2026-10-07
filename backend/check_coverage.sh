#!/bin/sh
# Coverage ratchet: fails on test failure or on any floored package below
# its floor in coverage_floor.txt. Local note: the auto-downloaded Go 1.25.0
# toolchain in this workspace lacks the covdata tool, so run with
# GOTOOLCHAIN=go1.25.13 (cached) when that happens; CI uses setup-go and
# is unaffected.
set -eu
out=$(go test -cover ./... 2>&1) || { printf '%s\n' "$out"; exit 1; }
printf '%s\n' "$out" | grep -E '^FAIL' && exit 1 || true
fail=0
while read -r pkg floor; do
  case "$pkg" in ''|'#'*) continue;; esac
  line=$(printf '%s\n' "$out" | grep -E "[[:space:]]$pkg[[:space:]]" || true)
  if [ -z "$line" ]; then echo "missing coverage for $pkg"; fail=1; continue; fi
  pct=$(printf '%s' "$line" | sed -E 's/.*coverage: ([0-9]+)\.[0-9]+%.*/\1/')
  if [ "$pct" -lt "$floor" ]; then echo "$pkg coverage ${pct}% below floor ${floor}%"; fail=1; fi
done < coverage_floor.txt
exit $fail

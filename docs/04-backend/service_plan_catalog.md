# ServicePlan Catalog — C05 Contract

Implements the catalog part of [subscription storage](subscription_storage.md)
under [free local / monthly hosted](../02-product/service_monetization.md).
No plan, allowance quantity, retail price or store product is seeded.
StoreProduct bindings/readback remain C24; entitlements/metering remain C18/C21.
No catalog HTTP/admin exposure, purchase activation or local paywall is added.
Catalog codes/names are registered as system configuration in the privacy
inventory; they must not contain customer identity. No retention period is set.

## Immutable identity and versions

`service_plans`: UUID, unique stable code (lowercase ASCII slug, 3–64 chars),
server creation instant and nullable server retirement instant. Code/identity
cannot change; retirement is terminal and does not erase purchased history.
`service_plan_versions`: UUID, plan FK, positive number unique within the plan,
display name (3–120 trimmed characters), `monthly` period, `individual` subject,
format 1, bounded explicit allowance array and server creation instant.

Each inserted version is immutable, including before store publication. Change
terms by appending a version; do not update/delete/truncate old versions.
Later editable administrative drafts are separate from committed versions.
Retirement stops appending/new offers; historical reads remain available.
This does not terminate an existing paid period or certify store availability.

## Format 1 hosted allowances

Each allowance has exactly `capability`, `unit`, `window` and `limit`.
Limit is a positive signed 64-bit integer, never null, fractional or unlimited.
No duplicate capability, unknown field, local capability or account control.
At least one allowance is required; at most the four registered capabilities
fit format 1. The entire JSON array is bounded to 8 KiB for storage safety.

- `cloud_backup`: unit `bytes`, window `retained`; maximum hosted retained-byte
  inventory at any instant, including retained versions covered by that plan.
- `cross_device_sync`: unit `bytes`, window `subscription_month`; hosted outbound
  transfer bytes attributable to sync/restore in the verified monthly period.
- `web_library`: unit `requests`, window `subscription_month`; completed hosted
  library operations, deduplicated by operation identity when C21 implements it.
- `hosted_share`: unit `bytes`, window `subscription_month`; publisher-attributed
  outbound hosted Share transfer bytes in that period.

`subscription_month` is the verified provider-effective monthly interval in UTC,
not a guessed 30-day device window. `retained` does not reset on renewal.
Bytes are integer bytes, not ambiguous GB/GiB. No reset/meter enforcement is
claimed by C05. Further hosted meters/providers need a reviewed format extension;
unsupported services are not silently unbounded. All device equivalents remain
free, along with mandatory identity/billing/privacy controls.

## Storage and concurrency

Migration [0006_service_plans.sql](../../backend/migrations/0006_service_plans.sql)
enforces shape, registration, units, finite amounts, period/scope and immutability
even on direct SQL writes. The insert guard serializes with plan retirement.
Services append under a plan row lock, allocate the next version and preserve
unique history under concurrency. No device time or billing data enters seals.
The internal service validates codes, canonical UUIDs, version numbers and
keyset cursors. History pages contain at most 100 versions, ordered by number;
clients continue after the last returned number. Unknown plans return not-found,
storage failures remain errors, and unsupported terms never return partial rights.
No mutation transport is wired: current staff permission/audit exposure belongs
to C07/C41, and store-qualified offer availability remains a later boundary.

Expand-only deployment: retain the new tables/functions/triggers on binary
rollback. There are no customers/plans to backfill and no automatic sale enable.
Do not drop catalog versions once future subscriptions or StoreProducts refer
to them. A destructive rollback requires separate reviewed export/dependency
analysis; it cannot rewrite purchased rights or sealed artifacts.

## Verification sources

- [Database fixture](../../backend/internal/testutil/database.go): disposable
  per-test database selected only by TEST_DATABASE_URL.
- [Fixture index](../../backend/internal/testutil/README.md).
- [Schema regressions](../../backend/internal/store/postgres/service_plan_schema_test.go):
  migration replay/populated upgrade, constraints, immutable history/retirement.
- [Catalog service and persistence](../../backend/internal/service/commerce/catalog/README.md):
  typed terms, validation, transactional append/retirement and bounded queries.

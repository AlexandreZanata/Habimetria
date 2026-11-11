# Phase 5 — Paid Hosted Collection with Free Local Authority

Depends on C01-C32. Read [cloud contract](../../03-architecture/cloud_sync_contract.md),
[privacy](../../03-architecture/commercial_privacy.md) and [runbook](agent_runbook.md).
Each slice requires real DB/storage and relevant mobile checks, commit/push/CI.

## C33 — Enroll selected collections under one account

Explicit account/data/transfer consent, enrollment receipt, local/remote ID
mapping and current entitlement/quota guard. Stop scoped workers on account
switch; do not auto-claim unowned rows. Test two accounts and enrollment retry.
Commit: `feat(sync): enroll authorized local collections`.

## C34 — Preserve complete server snapshot and Report versions

Incremental schema/service parity for all immutable Report identities,
Manifest versions, receipts, public keys and declarations. Preserve legacy
Report IDs and server-seal semantics. Test old/new packages and prohibit
overwriting final PDFs while keeping migration backward compatible.
Commit: `db(vault): preserve immutable cloud snapshot versions`.

## C35 — Transfer originals with durable quota reservations

Owner-scoped bounded destinations, resume/checkpoints, byte/hash validation,
atomic completion/metering, orphan reconciliation and explicit server ACK.
Test corrupted bytes, wrong owner, upload expiry, process death, disk/storage
outage and quota races. No original replacement or local deletion on failure.
Commit: `feat(backup): resume verified original uploads`.

## C36 — Finalize and list complete backups

Inventory and finalize immutable snapshots only after every reference/file
passes. Include Findings/Occurrences/comparison/declaration metadata, all
Reports/receipts and public verification material. Exclude private keys.
Test missing item, unsupported schema, duplicate completion and exact hashes.
Commit: `feat(backup): finalize complete immutable collection snapshots`.

## C37 — Synchronize changes and resolve conflicts

Versioned change cursor, DRAFT compare-and-set edits, tombstones and explicit
two-device conflict recovery. Respect enrollment/plan/source state. Test
offline rejoin, delete/rename races, duplicate operations and terminal seal.
Commit: `feat(sync): reconcile revisioned collection changes`.

## C38 — Restore snapshots on a clean device

Authorized manifest/download staging, verified bytes/references, disk bounds
and atomic local import. Support collisions and interrupted restore; preserve
historic public material while new signing uses current installation key.
Test independent verifier and complete multi-Report round trip on both clients.
Commit: `app(restore): recover verified cloud collection snapshots`.

## C39 — Publish controlled hosted shares and verification

Paid publisher entitlement/allowance; recipient authorized access without
subscription, minimized public verification and short-lived private downloads.
Add expiration/revocation, honest access events and contracted retention.
Test expired/revoked link, metadata leak and already-downloaded-copy semantics.
Commit: `feat(share): publish private plan-funded report access`.

## C40 — Complete the hosted user web library

Real own-account library with search/pagination, complete immutable PDF view/
download, version/sync status, subscription/usage/help, explicit Report
availability and recovery/export paths after expiry. Required account/privacy
controls remain accessible. Test actual browser documents and mobile widths.
Commit: `feat(web): complete the subscribed collection library`.

Exit: clean-device restore and independent verification prove full backup;
server-confirmed never precedes durable acknowledgment. Local workflows stay
free and usable when account, plan, network or cloud recovery is unavailable.

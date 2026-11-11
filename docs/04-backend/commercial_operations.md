# Commercial Operations — Planned Production Runbooks

Read [administration](commercial_admin.md), [cost pricing](cost_pricing.md),
[privacy](../03-architecture/commercial_privacy.md) and
[release gates](../06-roadmap/commercial/release_gates.md).

## Environments and deployment

[C06 commercial configuration](commercial_configuration.md) defines the
implemented provider namespace/credential-reference and new-sale controls.

Separate production/sandbox databases, object namespaces, store credentials,
topics/endpoints, product mappings, email identities and analytics.
Use PostgreSQL 18.6-alpine, TLS/public domain, private storage, least-privilege
service identities and secret references with rotation. No secrets in mobile
clients, web templates, log payloads or committed environment files.
Document trusted proxy/rate-limit behavior; the current panel limiter sees
connection addresses, so deployment must not blindly trust forwarded headers.

Deploy expand/migrate/contract with readiness/health checks and versioned
artifacts. Readiness checks distinguish a dependency outage from missing
commercial configuration. A missing valid plan blocks new sales, not local
startup or required account controls. Rollout/kill-switch can stop purchase
exposure or new hosted writes; retain paid delivery/reconciliation/recovery.

## Durable jobs and connector failures

Leased jobs, bounded exponential backoff with jitter, attempt history,
dead-letter review, idempotency and replay permissions. Maintain store event
age/lag, acknowledgments approaching provider deadlines, token verification
errors, outbox backlog and reconciliation drift. No success ACK before event
durability. Reprocessing must not duplicate a purchase or usage record.
Rotate/store credentials with a tested runbook and safe fallback state.

## Support and financial operation

Support protocol includes category, assigned operator, severity, response
target, actions and customer-visible outcome. Handle paid-but-not-delivered,
wrong account, duplicate subscription, refund request, missing snapshot and
privacy requests without asking for passwords, full card data or private keys.
The app never collects raw card details; stores own checkout.

Import official store finance reports idempotently, with currency/period and
matching provenance. Separate gross sale, commission/tax adjustments,
recognized recurring estimate and net payout. Reconcile against verified
ledger, not notification counts. Manual imports are acceptable as a first
complete slice; automated jobs must subsequently exercise the same importer.
Accountant owns fiscal classification/invoice obligations; store receipt is
not evidence that every Brazilian tax obligation has been fulfilled.

## Observability and service objectives

Use PII-minimized structured logs, request/job references and metrics for
purchase delivery, event lag, acknowledgment failure, paid-state divergence,
sync/restore correctness, object errors, email, latency, crash/ANR and costs.
Do not emit raw tokens, certificate secrets, interiors, exact GPS or CPF.
Commercial metrics exclude sandbox and operational grants; define paid-active,
conversion, voluntary/involuntary churn, refunds and cohort cost formulas.

Before beta record target capacity, p95 latency/error budgets, delivery-lag
thresholds, storage headroom, RPO/RTO and incident response targets. These are
measured on declared workloads, not invented here. Existing mobile performance
budgets still apply. Owner approves commitments before marketing an SLA.

## Backup/restore and retention

Back up database plus immutable objects consistently; cover encryption keys,
public verification material and migration version. Define PITR/versioning,
retention/access and restore environment. Run a timed recovery drill and
independent collection integrity check. Replay deletion/holds before reconnecting
restored services. A successful backup command alone is not proof of recovery.
Keep recovery capacity funded after customer plan expiration.

## Incident response

For each alert define operator, diagnostic commands, containment, rollback,
customer communication and escalation. Scenarios: fraudulent event, leaked
token, wrong-owner grant, mass upload loss, store downtime, runaway cost and
PII exposure. Preserve evidence with limited access; legal/privacy operator
determines applicable ANPD/subject notices and timelines.

## Production proof

Run migration replay and failure injection on disposable real PostgreSQL,
S3-compatible storage and mail sandbox. Qualify actual production-equivalent
configuration, backups and alerts separately. Record build, environment,
scenario, expected/observed result and artifact; never fill missing evidence
with assumed pass. Store credentials, production publication, fiscal/privacy
acceptance and user/device gates remain explicitly manual.

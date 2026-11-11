# Commercial Privacy, Retention and Evidence Preservation

Target acceptance contract; legal sign-off is external and unverified.
Read [Brazil applicability](brazil_report_legal_requirements.md),
[account lifecycle](account_lifecycle.md) and
[store sources](../06-roadmap/commercial/store_compliance.md).

## Data minimization and access

Extend processing records with purpose, legal basis, controller/processor,
retention, region/transfers and operators for account, transactions, billing
events, usage/cost, support, device/session and hosted collection data.
Do not equate device permission with a legal basis or assume consent is the
only legal basis. Evaluate sensitive incidental content in interior photos.
Telemetry is minimized; local functionality requires no marketing tracking.

Commercial staff see safe account and purchase context, not collection content.
Keep explicit Report-specific titleholder grants plus current staff capability.
Revocation takes effect on subsequent requests. No support impersonation,
password retrieval, privileged public links or unscoped database exports.

## Required policy configuration before paid release

Define a versioned policy for each class: active hosted originals/Reports,
expired-account recovery copies, deleted-account bytes, staging uploads,
financial/purchase records, sessions/verification links, support, event proofs,
security/audit logs, operational backups, public verification and legal holds.
For each record: purpose, applicable basis, duration/trigger, authorized access,
deletion/anonymization mechanism and backup expiry/restore behavior.

No durations are approved here. The owner/legal/privacy operator supplies
concrete lawful values before enrollment/sale. Missing policy blocks hosted
release, not local operation. Capacity and subscription prices must reserve
cost for the promised recovery/retention period and recipient verification.

## Erasure versus immutability

Immutability means no changes to historical bytes while retained. It is not
permission to retain personal data indefinitely or reject every erasure
request. Decide and document a lawful retention/erasure design before upload.
Deleting the retained object and its authorized references is different from
editing its signed bytes. Where minimization/redaction is needed, create a
new derived identity and preserve honest verification semantics.

Existing Manifests can contain personal declarations; do not claim they are
PII-free or retroactively redact/re-sign historical originals. A future
separated encrypted identity vault must be a versioned format decision with
backward-compatible verification. Legal holds record authority, reason,
scope, expiry/review and restricted access; support cannot invent holds.

## Rights workflow

Track authenticated request, scope/authority validation, export/correction/
deletion decision, affected stores/objects/backups, notices and completion.
Correct current profile without changing finalized historical declarations.
Data exports are authorized, bounded and expire. Do not bill a privacy request
or require an active hosted plan to request it. Juridical exceptions and
identity-verification proportionality require responsible review.

Notify about the distinction between deleting the account, canceling a store
subscription, clearing a local device and recalling already-shared copies.
After backup restore, replay deletion/hold records before resuming access;
otherwise an operational restore could resurrect deleted private data.

## Public pages, providers and incident handling

Keep privacy policy, terms, support and account-deletion web page publicly
reachable. Minimize `/v` verification and authorized `/s` disclosures; a QR
is not permission to disclose all original photos or private identities.
Store Data safety/App Privacy and SDK manifests must match actual behavior,
including optional sync, purchase analytics and remote providers.

List infrastructure/subprocessors and international transfer arrangements.
Remote AI requires explicit data flow/purpose and retention/training terms;
do not send a customer's photos to a provider just because they subscribed.
No automated liability/professional conclusions from a photo suggestion.
Document incident detection, containment, evidence and applicable ANPD/subject
notification decisions/timelines with the responsible operator.

## Proof

Two-account isolation, revoked Report grant, masked logs, minimized public
verification, account deletion, expired subscriptions, legal holds and backup
restoration after deletion are mandatory tests. Counsel/privacy acceptance
and store-form submissions remain manual gates with dated evidence.

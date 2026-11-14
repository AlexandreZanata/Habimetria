# Phase 2 — Connected Account and Security

Depends on C01-C08. Read [identity](../../03-architecture/account_lifecycle.md),
[API](../../04-backend/subscription_api.md) and [runbook](agent_runbook.md).
Each slice requires applicable real-DB/mobile checks and its own commit/push.

## C09 — Add self-registration and transactional mail delivery

Extend existing users/credentials with pending verification and a durable mail
outbox/provider adapter. Neutral responses, rate limits, bounded templates,
delivery/failure visibility; no second identity database or manual login-link
distribution as production signup. Test replay, duplicates and mail outage.
Commit: `feat(account): add verified registration delivery`.

## C10 — Verify email and expose shared authentication

Consume short-lived links atomically; reuse password policy and existing panel
authorization with JSON mobile login and web forms. Tokens are purpose-bound
and digest-stored. Test concurrent consume, expiry, enumeration and role checks.
Commit: `feat(account): unify verified web and mobile login`.

## C11 — Deliver customer-owned password recovery

Self-service reset uses purpose-specific one-time proofs, neutral initiation,
credential/session invalidation and security mail. Staff cannot reset activated
customers to log in as them. Test abuse, expired links and cross-purpose replay.
Commit: `feat(account): add secure self-service recovery`.

## C12 — Add rotating mobile sessions

Introduce device-scoped refresh families, short-lived audience-checked access,
rotation/reuse detection, revoke/list endpoints and protected platform storage.
Test stolen refresh reuse, disabled account, expiry and logout with two devices.
Commit: `feat(auth): add revocable native device sessions`.

## C13 — Implement account screens and device controls

Shared UI for signup/login/verification/recovery/profile/logout/device list;
correct skeleton/errors and brand/insets. Stop owner-scoped queues on logout
without deleting local collection. Test API isolation and free workflow when signed out.
Commit: `app(account): add connected account management`.

## C14 — Require staff MFA and safe recovery

Add enrolled factor, protected secrets, challenge limits, single-use recovery
codes and audited reauthentication for privileged actions. Preserve trusted
primary-owner recovery without customer impersonation. Exercise failure/races.
Commit: `feat(security): require administrative multifactor access`.

## C15 — Add privacy request intake and public deletion path

Authenticated export/deletion request/status plus a discoverable web request
page and in-app entry, independent of paid entitlement. Explain subscription
management separately; worker disposal lands C44 after policy configuration.
Deliver complete intake/status behavior, never pretend data is already deleted.
Test request ownership, identity challenge and expired-plan accessibility.
Commit: `feat(privacy): add account rights request intake`.

## C16 — Secure sensitive profile and account transitions

Reauthentication and notifications for password/email/device changes; finalize
switch-account behavior, local enrollment isolation and account status checks.
Add help for purchase-binding conflict without revealing another customer.
Test multiple customers/devices and recovery after interrupted account change.
Commit: `fix(account): isolate sensitive account transitions`.

Exit: a single real account works on app/web; staff cannot impersonate it.
Do not open paid enrollment before retention/deletion workers and store gates pass.

# Account Lifecycle — Planned Connected-Service Identity

Extend [panel identity](../04-backend/web_panel.md) rather than introducing
a second account system. [Local work](../02-product/service_monetization.md)
never requires registration. Connected account does not imply sync enrollment.

## Registration and recovery

Self-registration verifies email through a short-lived single-use digest-
stored token. Email/password reuses the existing versioned credential policy.
Support neutral responses, address/source throttling, resend limits,
deliverability, failures and safe link/referrer/log handling.
Activated customers control their password recovery; staff never obtain
their login link or act as them. Password/email change needs reauthentication,
notification and explicit session-revocation behavior.

Native clients use short-lived access credentials and rotating device-scoped
refresh secrets protected by Keystore/Keychain. Persist only token digests
where verification permits. Detect refresh reuse and revoke the affected
family. List own sessions and revoke devices; check account active state and
current capabilities at each sensitive server operation.
Browser sessions retain secure HTTP-only cookies and same-origin CSRF checks.
Do not reuse a mobile refresh credential as a browser session.

Staff MFA is required for production administrative access, with challenge
attempt limits, secret protection, one-time recovery codes and audited reset.
Retain primary-owner/last-owner invariants during concurrent role changes.
If third-party primary login is added, review Apple guideline 4.8 for an
equivalent qualifying login option; social login is not required by this plan.

## Purchase binding and account change

Generate opaque account binding on the server. Bind verified store chain
once and require a deliberate audited recovery path for conflicts.
Store restoration does not reveal which other customer owns a chain.
Switching account stops owner-specific workers and clears session-derived
private caches safely. It never deletes local originals or auto-enrolls them
into the next account. Show the selected owner before cloud transfer.

## Deletion

Offer a clear request inside the app and on the public website. Reauthenticate
without unreasonable barriers, explain active subscriptions and supply the
correct store management path. Account deletion and canceling renewal are
separate. Do not require buying or waiting for subscription expiry simply to
exercise deletion rights. Support applicable immediate/scheduled choices
under store and legal rules; record actual completion status.

Revoke sessions/links, stop new sync, revoke public hosted access as applicable,
perform retention/hold assessment and process deletes/export safely.
Retain only justified minimized commercial claims needed for fraud/legal
obligations; never a full secret-rich account dump indefinitely.
Deletion from the service cannot remotely erase copies previously exported
or bytes on an offline device. Offer explicit local cleanup separately.
If Sign in with Apple is later used, revoke its tokens as required.

## Acceptance

Cover expired/replayed tokens, email enumeration, refresh reuse, device
revocation, staff reset abuse, disabled account and primary-owner races.
Run the local journey with no account and after session expiration.
Cross-check deletion with [retention](commercial_privacy.md) and
[store policies](../06-roadmap/commercial/store_compliance.md).

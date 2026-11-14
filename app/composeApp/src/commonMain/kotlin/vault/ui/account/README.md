# Connected account screens

`AccountScreen.kt` coordinates an explicitly entered HTTPS server and account
flows; `AccountForms.kt` renders registration, verification, login and recovery.
The shared account `AccountProof.kt` extracts bounded purpose-specific proofs
from email links.
`AccountDevices.kt` renders the authenticated profile, device revocation and
logout, including the distinction between local and unconfirmed remote logout.
Secrets are held only in transient Compose state and are not saved in navigation.
The account entry is optional; local work remains independent. The native apps
currently run in LOCAL_ONLY mode and have no owner-scoped cloud queue to cancel.

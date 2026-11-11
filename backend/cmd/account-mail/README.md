# Durable account mail worker

`main.go` wires signals; `run.go` opens the database and real private-file/SMTP
runtime, runs bounded batches and prints only aggregate JSON. `run_test.go`
checks status without SMTP secrets and an actual runtime delivery on C08 fixtures.
The API remains independent of this opt-in worker. Apply current migrations
through the normal API deployment before running it. No public signup in C09.

```sh
cd backend
go run ./cmd/account-mail --status
go run ./cmd/account-mail --once --limit 20
go run ./cmd/account-mail
```

`--status` needs only DATABASE_URL; it neither opens mail secrets nor contacts
SMTP. Delivery requires ACCOUNT_MAIL_ENABLED=true and the settings documented
in [registration](../../../docs/04-backend/account_registration.md). The dedicated
proof master is exactly 32 random bytes in a 0400/0600 file. SMTP password files
are private/nonempty/bounded, with no newline; they are not inline env values.
Use the explicit read-only [Compose overlay](../../../infra/docker-compose.account-mail.yml)
and operator-owned mount source. Production needs HTTPS/TLS/authentication;
no production configuration or external email is enabled automatically.

Daemon mode polls every five seconds. Each claim expires after sixty seconds;
SMTP is bounded to fifteen. Batches are 1-100 (default 20). Exit zero means
batch results persisted; inspect Sent/Retried/Failed/Lost and --status. Retry
delays/limits, expiration/cancellation and at-least-once behavior are specified
in the contract. No worker output is proof of inbox receipt/email verification.
SIGINT/SIGTERM stop work; an interrupted claim stays reclaimable. No raw SMTP
reply, recipient, password, proof or body is printed.

`infra/verify_account_mail_config.py` (run by verify-commerce-config) checks
rendered opt-in/environment parity and the non-creating read-only worker mount.

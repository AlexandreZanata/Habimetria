# Transactional verification mail

- `settings.go`: bounded addresses/origin and mandatory SMTP security modes.
- `template.go`: one fixed Portuguese verification message with stable operation
  Message-ID and a fragment proof; no caller-provided template/body/Host.
- `smtp.go`: real SMTP, mandatory TLS for remote hosts, deadlines/cancellation.
- `failure.go`: closed retry classification and redacted errors/sender values.
- `smtp_fixture_test.go` and `smtp_test.go`: actual local protocol/TLS fixture,
  no downgrade, certificate/auth checks, cancellation and safe failures.
- `template_test.go`: input/header bounds and fixed message identity/content.
- `mailpit_integration_test.go`: real C08 SMTP capture and decoded message proof.
- `account_message.go` and `account_message_test.go`: closed recovery/security
  templates, purpose-specific Message-ID and redacted message formatting.
  Security notices accept no token/password. `smtp.go` uses the same real secure
  transport for all templates; verification callers retain their adapter.

SMTP DATA acceptance is not inbox delivery. Public proof consumption is C10;
durable leasing/retry belongs to account delivery. No application mail is sent
to a real recipient by tests. [Contract](../../../docs/04-backend/account_registration.md).
Transport API follows [Go SMTP](https://pkg.go.dev/net/smtp).

# Native authentication inputs

`transport.go` rejects ambient browser/mixed credentials and resolves only the
actual connection peer for throttling. `transport_test.go` checks malformed
headers, cookies, origins, IPv6 and forged proxy inputs. It grants no authority:
the account service validates token digest, audience, expiry and current User.
See [authentication](../../../../docs/04-backend/account_authentication.md).

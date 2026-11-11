package mail

import (
	"crypto/tls"
	"errors"
	"fmt"
	"io"
	"net/textproto"
)

type deliveryError struct {
	code  string
	retry bool
	cause error
}

func (e *deliveryError) Error() string                   { return "account mail " + e.code + " failure" }
func (e *deliveryError) Unwrap() error                   { return e.cause }
func (e *deliveryError) Format(state fmt.State, _ rune)  { _, _ = io.WriteString(state, e.Error()) }
func failure(code string, retry bool, cause error) error { return &deliveryError{code, retry, cause} }
func protocolFailure(err error) error {
	var reply *textproto.Error
	if errors.As(err, &reply) && reply.Code >= 500 && reply.Code <= 599 {
		return failure("rejected", false, err)
	}
	return failure("transport", true, err)
}

func secureFailure(err error) error {
	var verification *tls.CertificateVerificationError
	if errors.As(err, &verification) {
		return failure("tls", false, err)
	}
	return protocolFailure(err)
}
func Failure(err error) (code string, retry bool) {
	var safe *deliveryError
	if errors.As(err, &safe) {
		return safe.code, safe.retry
	}
	return "transport", true
}

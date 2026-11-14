package panelweb

import (
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/storage"
)

type AccountServices struct {
	Registration  *account.Registration
	Verification  *account.Verification
	Recovery      *account.RecoveryRequest
	PasswordReset *account.PasswordReset
}

func NewConnected(service *panel.Service, reader storage.Reader, origin string, accounts AccountServices) (*Web, error) {
	p, err := New(service, reader, origin)
	if err != nil {
		return nil, err
	}
	p.registration, p.verification = accounts.Registration, accounts.Verification
	p.recovery, p.passwordReset = accounts.Recovery, accounts.PasswordReset
	return p, nil
}

package account

import (
	"context"
	"net/mail"
	"net/netip"
	"strings"
	"time"
)

func requestAddress(email, source string) (string, string, error) {
	email = strings.ToLower(strings.TrimSpace(email))
	a, err := mail.ParseAddress(email)
	ip, ipErr := netip.ParseAddr(source)
	if err != nil || a.Address != email || len(email) > 254 || ipErr != nil || strings.ContainsAny(email, "\r\n") {
		return "", "", ErrInput
	}
	for _, r := range email {
		if r > 127 {
			return "", "", ErrInput
		}
	}
	return email, ip.Unmap().String(), nil
}

// A normal-path lower bound is shared by all eligible/ineligible initiations.
func responseFloor(ctx context.Context, started time.Time) {
	timer := time.NewTimer(max(0, 250*time.Millisecond-time.Since(started)))
	defer timer.Stop()
	select {
	case <-timer.C:
	case <-ctx.Done():
	}
}

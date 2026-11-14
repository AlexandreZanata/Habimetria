package account

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"io"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"

	"vault/internal/accountproof"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

type Device = postgres.Device
type DevicePair struct {
	Access  string    `json:"access_token"`
	Refresh string    `json:"refresh_token"`
	ID      string    `json:"session_id"`
	User    string    `json:"user_id"`
	Kind    string    `json:"token_type"`
	Seconds int       `json:"expires_in"`
	Expires time.Time `json:"refresh_expires_at"`
}

func (p DevicePair) Format(state fmt.State, _ rune) {
	_, _ = io.WriteString(state, "[device credentials redacted]")
}

type deviceTokens struct {
	access, refresh         string
	accessHash, refreshHash []byte
}

func newDeviceTokens() deviceTokens {
	var a, r [32]byte
	rand.Read(a[:])
	rand.Read(r[:])
	access := base64.RawURLEncoding.EncodeToString(a[:])
	refresh := "hr1_" + base64.RawURLEncoding.EncodeToString(r[:])
	ah, _ := accountproof.Digest(access)
	rh, _ := refreshDigest(refresh)
	return deviceTokens{access, refresh, ah, rh}
}
func refreshDigest(token string) ([]byte, error) {
	if len(token) != 47 || !strings.HasPrefix(token, "hr1_") {
		return nil, postgres.ErrNotFound
	}
	raw, err := base64.RawURLEncoding.Strict().DecodeString(token[4:])
	if err != nil || len(raw) != 32 {
		return nil, postgres.ErrNotFound
	}
	digest := sha256.Sum256(append([]byte("habimetria/account/refresh/v1:"), raw...))
	return digest[:], nil
}
func canonicalDeviceID(id string) bool {
	parsed, err := uuid.Parse(id)
	return err == nil && parsed != uuid.Nil && parsed.String() == id
}
func validateDevice(d Device) (Device, error) {
	d.Label = strings.TrimSpace(d.Label)
	if !canonicalDeviceID(d.ID) || (d.Platform != "android" && d.Platform != "ios") || !utf8.ValidString(d.Label) || len(d.Label) > 320 || utf8.RuneCountInString(d.Label) < 1 || utf8.RuneCountInString(d.Label) > 80 {
		return d, ErrInput
	}
	for _, r := range d.Label {
		if unicode.IsControl(r) {
			return d, ErrInput
		}
	}
	return d, nil
}
func (t deviceTokens) pair(g postgres.DeviceGrant) DevicePair {
	return DevicePair{t.access, t.refresh, g.ID, g.User, "Bearer", g.Seconds, g.Expires}
}

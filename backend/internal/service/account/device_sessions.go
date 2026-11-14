package account

import (
	"context"

	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

func (s *Authentication) LoginDevice(ctx context.Context, email, password string, device Device) (DevicePair, error) {
	device, err := validateDevice(device)
	if err != nil {
		return DevicePair{}, err
	}
	user, encoded, err := s.credentials.Authenticate(ctx, email, password)
	if err != nil {
		return DevicePair{}, err
	}
	tokens := newDeviceTokens()
	grant, err := s.store.StartDevice(ctx, user, uuid.NewString(), encoded, tokens.accessHash, tokens.refreshHash, device)
	if err != nil {
		return DevicePair{}, err
	}
	return tokens.pair(grant), nil
}
func (s *Authentication) Refresh(ctx context.Context, secret, device string) (DevicePair, error) {
	if !canonicalDeviceID(device) {
		return DevicePair{}, postgres.ErrNotFound
	}
	digest, err := refreshDigest(secret)
	if err != nil {
		return DevicePair{}, err
	}
	tokens := newDeviceTokens()
	grant, err := s.store.RotateDevice(ctx, digest, device, tokens.accessHash, tokens.refreshHash)
	if err != nil {
		return DevicePair{}, err
	}
	return tokens.pair(grant), nil
}
func (s *Authentication) Devices(ctx context.Context, user string) ([]postgres.DeviceSession, error) {
	return s.store.ListDevices(ctx, user)
}
func (s *Authentication) RevokeDevice(ctx context.Context, user, id string) error {
	if !canonicalDeviceID(id) {
		return postgres.ErrNotFound
	}
	return s.store.RevokeDevice(ctx, user, id)
}

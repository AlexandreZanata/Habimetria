package service

import (
	"errors"
	"fmt"

	"github.com/google/uuid"
)

// ErrInvalid marks a 400 validation failure. Wrap it with field context.
var ErrInvalid = errors.New("invalid input")

func checkUUID(id, field string) error {
	if _, err := uuid.Parse(id); err != nil {
		return fmt.Errorf("%s: %w", field, ErrInvalid)
	}
	return nil
}

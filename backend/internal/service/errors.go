package service

import "errors"

// ErrInvalid marks a 400 validation failure. Wrap it with field context.
var ErrInvalid = errors.New("invalid input")

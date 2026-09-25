package postgres

import (
	"errors"
	"time"
)

// ErrNotFound is returned when a parent row (property, tenancy,
// inspection) does not exist. Handlers map it to HTTP 404.
var ErrNotFound = errors.New("not found")

// ErrRoomExists is returned on duplicate room key per inspection.
// Handlers map it to HTTP 409.
var ErrRoomExists = errors.New("room already exists")

// Property is a rental address owned by a tenant user.
type Property struct {
	ID        string    `json:"id"`
	Address   string    `json:"address"`
	CreatedAt time.Time `json:"created_at"`
}

// Tenancy groups inspections for one rental period.
type Tenancy struct {
	ID         string    `json:"id"`
	PropertyID string    `json:"property_id"`
	Type       string    `json:"type"`
	CreatedAt  time.Time `json:"created_at"`
}

// Inspection is a DRAFT or SEALED evidence collection.
type Inspection struct {
	ID        string    `json:"id"`
	TenancyID string    `json:"tenancy_id"`
	Kind      string    `json:"kind"`
	Status    string    `json:"status"`
	CreatedAt time.Time `json:"created_at"`
}

// Room is a subdivision of an inspection (kitchen, bedroom, ...).
type Room struct {
	ID           string `json:"id"`
	InspectionID string `json:"inspection_id"`
	Key          string `json:"key"`
	Label        string `json:"label"`
	Sort         int    `json:"sort"`
}

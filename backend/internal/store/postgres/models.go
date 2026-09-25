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

// ErrMismatch is returned when an evidence does not belong to the
// finding's inspection. Handlers map it to HTTP 400.
var ErrMismatch = errors.New("evidence does not belong to inspection")

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

// Evidence is one sealed file: original bytes + server-side hash + timestamp.
// Geolocation and device time are device-reported claims, not certified facts.
type Evidence struct {
	ID                string     `json:"id"`
	InspectionID      string     `json:"inspection_id"`
	RoomID            *string    `json:"room_id"`
	Type              string     `json:"type"`
	Mime              string     `json:"mime"`
	Size              int64      `json:"size"`
	StorageKey        string     `json:"storage_key"`
	SHA256            string     `json:"sha256"`
	CapturedAtDevice  *time.Time `json:"captured_at_device"`
	Latitude          *float64   `json:"latitude"`
	Longitude         *float64   `json:"longitude"`
	ReceivedAtServer  time.Time  `json:"received_at_server"`
	Sequence          int        `json:"sequence"`
}

// Finding is one reported defect: photo evidence + preset + optional note.
type Finding struct {
	ID         string    `json:"id"`
	RoomID     string    `json:"room_id"`
	EvidenceID string    `json:"evidence_id"`
	Preset     string    `json:"preset"`
	Note       *string   `json:"note"`
	CreatedAt  time.Time `json:"created_at"`
}

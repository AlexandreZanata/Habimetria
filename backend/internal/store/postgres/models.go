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

// ErrEmpty is returned when sealing an inspection without evidences.
// Handlers map it to HTTP 422.
var ErrEmpty = errors.New("cannot seal inspection without evidences")

// ErrChanged is returned when the evidence set changes mid-seal.
// The caller rebuilds the manifest and retries. Maps to HTTP 409.
var ErrChanged = errors.New("evidence set changed during seal, retry")

// ErrExitNotAllowed is returned when an exit inspection cannot start:
// no sealed entry yet, or an exit already exists. Maps to HTTP 409.
var ErrExitNotAllowed = errors.New("exit inspection not allowed")

// ErrAlreadyLinked is returned when an evidence is already attached to
// an occurrence. Maps to HTTP 409.
var ErrAlreadyLinked = errors.New("evidence already attached to occurrence")

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

// Occurrence is a post-seal timeline entry on a property. It never
// mutates any inspection, sealed or not.
type Occurrence struct {
	ID         string    `json:"id"`
	PropertyID string    `json:"property_id"`
	Title      string    `json:"title"`
	Body       *string   `json:"body"`
	CreatedAt  time.Time `json:"created_at"`
}

// OccurrenceEvidence links one photo evidence as proof on an occurrence.
// The evidence must belong to the occurrence's property.
type OccurrenceEvidence struct {
	OccurrenceID string `json:"occurrence_id"`
	EvidenceID   string `json:"evidence_id"`
}

package postgres

import (
	"errors"
	"time"
)

var (
	ErrServicePlanConflict = errors.New("service plan already exists")
	ErrServicePlanRetired  = errors.New("service plan is retired")
	ErrServicePlanInvalid  = errors.New("invalid service plan")
)

type ServicePlan struct {
	ID, Code  string
	CreatedAt time.Time
	RetiredAt *time.Time
}

type HostedAllowance struct {
	Capability string `json:"capability"`
	Unit       string `json:"unit"`
	Window     string `json:"window"`
	Limit      int64  `json:"limit"`
}

// Version terms contain no price, actor/device clock or mutable publication flag.
type ServicePlanVersionInput struct {
	Name          string
	BillingPeriod string
	SubjectScope  string
	FormatVersion int
	Allowances    []HostedAllowance
}

type ServicePlanVersion struct {
	ID, PlanID string
	Number     int
	ServicePlanVersionInput
	CreatedAt time.Time
}

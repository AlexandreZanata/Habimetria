package catalog

import (
	"errors"
	"fmt"
	"regexp"
	"strings"
	"unicode"
	"unicode/utf8"

	"github.com/google/uuid"
	"vault/internal/store/postgres"
)

var ErrInvalid = errors.New("invalid monthly service plan")
var stableCode = regexp.MustCompile(`^[a-z][a-z0-9_-]{2,63}$`)

func ValidateCode(code string) error {
	if !stableCode.MatchString(code) {
		return fmt.Errorf("stable plan code: %w", ErrInvalid)
	}
	return nil
}

func validID(id string) bool {
	parsed, err := uuid.Parse(id)
	return err == nil && parsed.String() == id
}

func ValidateVersion(terms postgres.ServicePlanVersionInput) error {
	if !utf8.ValidString(terms.Name) || strings.TrimSpace(terms.Name) != terms.Name ||
		utf8.RuneCountInString(terms.Name) < 3 || utf8.RuneCountInString(terms.Name) > 120 || strings.ContainsFunc(terms.Name, unicode.IsControl) {
		return fmt.Errorf("plan display name: %w", ErrInvalid)
	}
	if terms.BillingPeriod != "monthly" || terms.SubjectScope != "individual" || terms.FormatVersion != 1 {
		return fmt.Errorf("monthly individual format 1 required: %w", ErrInvalid)
	}
	if len(terms.Allowances) < 1 || len(terms.Allowances) > 4 {
		return fmt.Errorf("explicit hosted allowances required: %w", ErrInvalid)
	}
	seen := map[string]bool{}
	for _, allowance := range terms.Allowances {
		if allowance.Limit <= 0 || seen[allowance.Capability] {
			return fmt.Errorf("finite unique hosted allowance required: %w", ErrInvalid)
		}
		seen[allowance.Capability] = true
		unit, window := "bytes", "subscription_month"
		switch allowance.Capability {
		case "cloud_backup":
			window = "retained"
		case "cross_device_sync", "hosted_share":
		case "web_library":
			unit = "requests"
		default:
			return fmt.Errorf("unsupported paid capability: %w", ErrInvalid)
		}
		if allowance.Unit != unit || allowance.Window != window {
			return fmt.Errorf("explicit unit and allowance window required: %w", ErrInvalid)
		}
	}
	return nil
}

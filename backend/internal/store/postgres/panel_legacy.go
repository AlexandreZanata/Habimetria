package postgres

import (
	"context"
	"fmt"
)

// ResourceOwner resolves the stable Property owner of an existing API
// resource. A nil owner identifies a legacy unclaimed resource.
func (s *PanelStore) ResourceOwner(ctx context.Context, resource, id string) (*string, error) {
	const property = `SELECT p.owner_user_id::text FROM properties p `
	const tenancy = property + `JOIN tenancies t ON t.property_id=p.id `
	const inspection = tenancy + `JOIN inspections i ON i.tenancy_id=t.id `
	var query string
	switch resource {
	case "properties":
		query = property + `WHERE p.id=$1`
	case "tenancies":
		query = tenancy + `WHERE t.id=$1`
	case "inspections":
		query = inspection + `WHERE i.id=$1`
	case "rooms":
		query = inspection + `JOIN rooms r ON r.inspection_id=i.id WHERE r.id=$1`
	case "reports":
		query = inspection + `JOIN reports r ON r.inspection_id=i.id WHERE r.id=$1`
	case "occurrences":
		query = property + `JOIN occurrences o ON o.property_id=p.id WHERE o.id=$1`
	default:
		return nil, fmt.Errorf("unsupported resource scope %q", resource)
	}
	var owner *string
	if err := s.pool.QueryRow(ctx, query, id).Scan(&owner); err != nil {
		return nil, fmt.Errorf("resolve resource owner: %w", panelMissing(err))
	}
	return owner, nil
}

package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"vault/internal/authz"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

var (
	ErrPanelDenied     = errors.New("operation not permitted")
	ErrPanelEmailTaken = errors.New("email already registered")
	ErrPanelPrimary    = errors.New("primary owner is protected")
)

type PanelStore struct{ pool *pgxpool.Pool }

func NewPanelStore(pool *pgxpool.Pool) *PanelStore { return &PanelStore{pool: pool} }

type PanelAccount struct {
	ID, Email, Name, Role                       string
	Active, Primary, Managed, Enrolled, Pending bool
	CreatedAt                                   time.Time
}

type PanelActor struct {
	PanelAccount
	Permissions []string
}

func (a PanelActor) Can(permission string) bool {
	return authz.Can(a.Active, a.Role, a.Permissions, permission)
}

const panelAccountColumns = `u.id::text, u.email::text, u.display_name, u.panel_role,
	u.panel_active, u.panel_primary, u.created_at, u.panel_managed, u.panel_password IS NOT NULL,
	u.registration_started_at IS NOT NULL AND u.email_verified_at IS NULL`

const panelPermissionColumn = `COALESCE((SELECT permissions FROM panel_role_permissions p WHERE p.role=u.panel_role), '{}')`

func scanPanelActor(row pgx.Row) (PanelActor, error) {
	var a PanelActor
	err := row.Scan(&a.ID, &a.Email, &a.Name, &a.Role, &a.Active, &a.Primary, &a.CreatedAt, &a.Managed, &a.Enrolled, &a.Pending, &a.Permissions)
	return a, panelMissing(err)
}

func panelMissing(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return ErrNotFound
	}
	return err
}

func panelUnique(err error) error {
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" && pg.ConstraintName == "users_email_key" {
		return ErrPanelEmailTaken
	}
	return err
}

func (s *PanelStore) Session(ctx context.Context, hash []byte) (PanelActor, error) {
	a, err := scanPanelActor(s.pool.QueryRow(ctx, `SELECT `+panelAccountColumns+`, `+panelPermissionColumn+`
		FROM users u JOIN panel_sessions s ON s.user_id=u.id
		WHERE s.token_hash=$1 AND s.expires_at > now() AND u.panel_active`, hash))
	if err != nil {
		return PanelActor{}, fmt.Errorf("read panel session: %w", err)
	}
	return a, nil
}

func (s *PanelStore) Activate(ctx context.Context, linkHash, password, sessionHash []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if _, err := tx.Exec(ctx, `LOCK TABLE users IN ROW EXCLUSIVE MODE`); err != nil {
			return fmt.Errorf("lock account table for activation: %w", err)
		}
		// Lock the account first, in the same order as administrative mutations.
		var user string
		err := tx.QueryRow(ctx, `SELECT u.id::text FROM users u
			JOIN panel_access_links l ON l.user_id=u.id
			WHERE l.token_hash=$1 AND l.expires_at > now() AND u.panel_active
			AND u.panel_managed AND (u.panel_password IS NULL OR u.panel_primary) FOR UPDATE OF u`, linkHash).Scan(&user)
		if err != nil {
			return fmt.Errorf("validate access link: %w", panelMissing(err))
		}
		tag, err := tx.Exec(ctx, `DELETE FROM panel_access_links WHERE token_hash=$1 AND expires_at > now()`, linkHash)
		if err != nil {
			return fmt.Errorf("consume access link: %w", err)
		}
		if tag.RowsAffected() != 1 {
			return ErrNotFound
		}
		if _, err := tx.Exec(ctx, `UPDATE users SET panel_password=$2 WHERE id=$1`, user, password); err != nil {
			return fmt.Errorf("store account credential: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_sessions WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("invalidate prior sessions: %w", err)
		}
		return panelSession(ctx, tx, user, sessionHash)
	})
}

func (s *PanelStore) Logout(ctx context.Context, hash []byte) error {
	_, err := s.pool.Exec(ctx, `DELETE FROM panel_sessions WHERE token_hash=$1`, hash)
	return err
}

func panelLink(ctx context.Context, tx pgx.Tx, user string, hash []byte) error {
	_, err := tx.Exec(ctx, `INSERT INTO panel_access_links(token_hash,user_id,expires_at)
		VALUES ($1,$2,now() + interval '30 minutes')
		ON CONFLICT(user_id) DO UPDATE SET token_hash=excluded.token_hash, expires_at=excluded.expires_at`, hash, user)
	if err != nil {
		return fmt.Errorf("issue access link: %w", err)
	}
	return nil
}

func panelAudit(ctx context.Context, tx pgx.Tx, actor, event string, subject *string) error {
	_, err := tx.Exec(ctx, `INSERT INTO panel_audit(actor_id,event,subject_id) VALUES ($1,$2,$3)`, actor, event, subject)
	if err != nil {
		return fmt.Errorf("write panel audit: %w", err)
	}
	return nil
}

package storage

import (
	"context"
	"io"
	"time"
)

// Port abstracts object storage (MinIO local, S3/R2 prod).
// Keys are opaque; only signed URLs ever leave the server.
type Port interface {
	PutOriginal(ctx context.Context, key string, r io.Reader, mime string) error
	SignedGetURL(ctx context.Context, key string, ttl time.Duration) (string, error)
	Delete(ctx context.Context, key string) error
}

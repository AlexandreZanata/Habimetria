package s3store

import (
	"context"
	"fmt"
	"io"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
)

// Config holds S3/MinIO connection settings. Path-style is required for MinIO.
type Config struct {
	Endpoint     string
	Region       string
	Bucket       string
	AccessKey    string
	SecretKey    string
	UsePathStyle bool
}

// Store implements storage.Port over S3-compatible APIs.
type Store struct {
	client  *s3.Client
	presign *s3.PresignClient
	bucket  string
}

// New builds an S3 store. Static credentials only — no IAM discovery in MVP.
func New(cfg Config) *Store {
	client := s3.New(s3.Options{
		Region:       cfg.Region,
		Credentials:  credentials.NewStaticCredentialsProvider(cfg.AccessKey, cfg.SecretKey, ""),
		BaseEndpoint: aws.String(cfg.Endpoint),
		UsePathStyle: cfg.UsePathStyle,
	})
	return &Store{client: client, presign: s3.NewPresignClient(client), bucket: cfg.Bucket}
}

// EnsureBucket creates the bucket when missing. Best-effort on boot.
func (s *Store) EnsureBucket(ctx context.Context) error {
	_, err := s.client.HeadBucket(ctx, &s3.HeadBucketInput{Bucket: &s.bucket})
	if err == nil {
		return nil
	}
	_, err = s.client.CreateBucket(ctx, &s3.CreateBucketInput{Bucket: &s.bucket})
	if err != nil {
		return fmt.Errorf("create bucket %s: %w", s.bucket, err)
	}
	return nil
}

// PutOriginal uploads immutable bytes. Key: originals/{inspection_id}/{evidence_id}.
func (s *Store) PutOriginal(ctx context.Context, key string, r io.Reader, mime string) error {
	_, err := s.client.PutObject(ctx, &s3.PutObjectInput{
		Bucket:      &s.bucket,
		Key:         &key,
		Body:        r,
		ContentType: aws.String(mime),
	})
	if err != nil {
		return fmt.Errorf("put %s: %w", key, err)
	}
	return nil
}

// SignedGetURL returns a short-lived GET URL. Default TTL enforced by caller.
func (s *Store) SignedGetURL(ctx context.Context, key string, ttl time.Duration) (string, error) {
	out, err := s.presign.PresignGetObject(ctx, &s3.GetObjectInput{
		Bucket: &s.bucket,
		Key:    &key,
	}, s3.WithPresignExpires(ttl))
	if err != nil {
		return "", fmt.Errorf("presign %s: %w", key, err)
	}
	return out.URL, nil
}

// Delete removes an orphan (DB write failed after upload). Best-effort.
func (s *Store) Delete(ctx context.Context, key string) error {
	_, err := s.client.DeleteObject(ctx, &s3.DeleteObjectInput{Bucket: &s.bucket, Key: &key})
	if err != nil {
		return fmt.Errorf("delete %s: %w", key, err)
	}
	return nil
}

func (s *Store) OpenOriginal(ctx context.Context, key string) (io.ReadCloser, error) {
	out, err := s.client.GetObject(ctx, &s3.GetObjectInput{Bucket: &s.bucket, Key: &key})
	if err != nil {
		return nil, fmt.Errorf("read stored original: %w", err)
	}
	return out.Body, nil
}

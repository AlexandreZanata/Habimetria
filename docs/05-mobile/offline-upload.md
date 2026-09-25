# Offline Upload — Never Lose a Photo

```text
shutter → save local → enqueue → background upload → server ack → ✓
```

- Local store: DataStore/SQLDelight + files dir. Survives kill.
- States per item: `queued | uploading | confirmed | failed`.
- Room header shows: `✓ 12 saved · ↑ 2 sending · ! 1 waiting`.
- Retry: exponential backoff, resume on connectivity, idempotency-key per evidence.
- Server ack = `{ evidence_id, received_at_server }`. Only then show ✓.
- Signed upload: `POST /evidences:upload-url` → PUT bytes → confirm hash.

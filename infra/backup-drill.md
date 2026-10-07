# Backup and Restore Drill (quarterly)

Rehearses PostgreSQL PITR plus object-storage recovery against RTO/RPO
declared with the prefeitura. Synthetic data only; record the dated
result in the acceptance report. No production credentials in commands.

## 0. Declare targets

- RTO: <time to healthy API after total loss>. RPO: <max data loss>.
- Source: `pgdata` volume + `miniodata` (or S3 bucket with versioning).

## 1. Snapshot (staging clone, never production)

```sh
docker compose -f infra/docker-compose.yml exec postgres \
  pg_dump -U vault -Fc vault > /tmp/vault-drill.dump
aws --endpoint-url "$S3_ENDPOINT" s3 sync s3://vault /tmp/vault-s3-drill
```

## 2. Simulate loss on the clone

Stop the clone stack, remove its volumes, recreate empty ones.

## 3. Restore and verify

```sh
pg_restore -U vault -d vault /tmp/vault-drill.dump
aws --endpoint-url "$S3_ENDPOINT" s3 sync /tmp/vault-s3-drill s3://vault
docker compose -f infra/docker-compose.yml up -d api
curl -f http://localhost:8080/healthz
(cd backend && go run ./cmd/verify --format json testdata/fixture-package)
```

Pass: API healthy, verifier accepts, a sealed package opens with all
originals. Fail: file a defect before beta; unresolved restore failure
blocks release.

## 4. Cleanup

Destroy clone volumes and local drill copies (`shred -u` on any host
holding personal data, even synthetic-adjacent).

.PHONY: verify verify-commerce-config verify-mobile verify-ios-macos verify-all bench bench-apk up down test test-integration vet fmt-check cover secrets

verify: vet fmt-check test

verify-commerce-config:
	python3 infra/verify_commerce_config.py

verify-mobile:
	cd app && ./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug

bench:
	cd app && ./gradlew :shared:jvmTest --tests "vault.local.PerfProbeTest"

bench-apk:
	cd app && ./gradlew :composeApp:assembleRelease
	ls -lh app/composeApp/build/outputs/apk/release/*.apk
	du -h app/composeApp/build/outputs/apk/release/*.apk

verify-ios-macos:
	cd app && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
	xcodebuild -scheme iosApp -sdk iphonesimulator build

verify-all: verify verify-mobile

vet:
	cd backend && go vet ./...

test:
	cd backend && go test ./...

test-integration:
	cd backend && go test -run Integration ./...

fmt-check:
	cd backend && test -z "$$(gofmt -l .)" || (gofmt -l . && exit 1)

cover:
	cd backend && ./check_coverage.sh

# Coarse high-confidence secret patterns (no dependencies, runs anywhere).
# CI additionally runs gitleaks over the full history.
secrets:
	! git grep -n -E "AKIA[0-9A-Z]{16}|sk_live_[A-Za-z0-9]+|ghp_[A-Za-z0-9]{20,}|-----BEGIN [A-Z ]*PRIVATE KEY-----" -- . ':!*.md' || (echo "possible secret committed" && exit 1)

# Audit pack for controladoria: one command exporting the evidence bundle
# (commit, gates, migrations, coverage, fixture verification) to a
# timestamped text file. Evidence only; legal conclusions stay counsel-owned.
audit:
	./audit_pack.sh

up:
	cp -n infra/.env.example infra/.env 2>/dev/null || true
	docker compose -f infra/docker-compose.yml up --build

down:
	docker compose -f infra/docker-compose.yml down

.PHONY: verify verify-mobile verify-ios-macos verify-all up down test vet

verify: vet test

verify-mobile:
	cd app && ./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug

verify-ios-macos:
	cd app && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
	xcodebuild -scheme iosApp -sdk iphonesimulator build

verify-all: verify verify-mobile

vet:
	cd backend && go vet ./...

test:
	cd backend && go test ./...

up:
	cp -n infra/.env.example infra/.env 2>/dev/null || true
	docker compose -f infra/docker-compose.yml up --build

down:
	docker compose -f infra/docker-compose.yml down

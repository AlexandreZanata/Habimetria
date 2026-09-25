.PHONY: verify up down test vet

verify: vet test

vet:
	cd backend && go vet ./...

test:
	cd backend && go test ./...

up:
	cp -n infra/.env.example infra/.env 2>/dev/null || true
	docker compose -f infra/docker-compose.yml up --build

down:
	docker compose -f infra/docker-compose.yml down

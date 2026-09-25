# syntax=docker/dockerfile:1
FROM golang:1.25-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /api ./cmd/api

FROM alpine:3.20
RUN adduser -D app
USER app
COPY --from=build /api /api
EXPOSE 8080
ENTRYPOINT ["/api"]

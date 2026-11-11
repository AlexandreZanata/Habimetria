# syntax=docker/dockerfile:1
FROM golang:1.25-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /api ./cmd/api
RUN CGO_ENABLED=0 go build -o /panel-owner ./cmd/panel-owner
RUN CGO_ENABLED=0 go build -o /account-mail ./cmd/account-mail

FROM alpine:3.20
RUN apk add --no-cache ca-certificates && adduser -D app
USER app
COPY --from=build /api /api
COPY --from=build /panel-owner /panel-owner
COPY --from=build /account-mail /account-mail
EXPOSE 8080
ENTRYPOINT ["/api"]

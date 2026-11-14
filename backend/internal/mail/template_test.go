package mail

import (
	"io"
	"mime/quotedprintable"
	stdmail "net/mail"
	"strings"
	"testing"

	"github.com/google/uuid"
)

func TestBoundedVerificationTemplate(t *testing.T) {
	settings := Settings{Address: "127.0.0.1:1025", Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}
	token, operation := proof(t), uuid.NewString()
	for range 2 {
		data, err := message(settings, "recipient@example.test", token, operation)
		if err != nil {
			t.Fatal(err)
		}
		parsed, err := stdmail.ReadMessage(strings.NewReader(string(data)))
		if err != nil {
			t.Fatal(err)
		}
		body, err := io.ReadAll(quotedprintable.NewReader(parsed.Body))
		if err != nil {
			t.Fatal(err)
		}
		if !strings.Contains(string(body), settings.Origin+"/painel/verificar#verificar="+token) || parsed.Header.Get("Message-ID") != "<"+operation+".email-verification@localhost>" || len(data) > 2048 {
			t.Fatal("template identity/origin/bounds differ")
		}
	}
	for _, tc := range [][3]string{{"bad", token, operation}, {"victim@example.test\r\nBCC:x@example.test", token, operation}, {"a@example.test", "bad", operation}, {"a@example.test", token, "bad\r\n"}} {
		if _, err := message(settings, tc[0], tc[1], tc[2]); err == nil {
			t.Fatal("unsafe verification payload accepted")
		}
	}
	for _, edit := range []func(*Settings){
		func(s *Settings) { s.Address = "remote.example.test:1025" }, func(s *Settings) { s.Mode = "optional" },
		func(s *Settings) { s.From = "bad" }, func(s *Settings) { s.Origin = "https://user:password@example.test" },
		func(s *Settings) { s.Origin = "http://outside.example.test" }, func(s *Settings) { s.Address = "localhost:0" },
		func(s *Settings) { s.Username = "user\n" }, func(s *Settings) { s.Origin += "/path" },
	} {
		value := settings
		edit(&value)
		if _, err := NewSMTP(value, nil); err == nil {
			t.Fatal("unsafe SMTP configuration accepted")
		}
	}
	if _, err := NewSMTP(settings, []byte("password")); err == nil {
		t.Fatal("plain SMTP credential accepted")
	}
	settings.Mode = "tls"
	settings.Username = "fixture"
	if _, err := NewSMTP(settings, []byte("secret\x00")); err == nil {
		t.Fatal("invalid SASL credential accepted")
	}
}

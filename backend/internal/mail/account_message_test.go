package mail

import (
	"encoding/json"
	"fmt"
	"io"
	"mime/quotedprintable"
	stdmail "net/mail"
	"strings"
	"testing"

	"github.com/google/uuid"
)

func TestRecoveryAndSecurityTemplates(t *testing.T) {
	s := Settings{Origin: "http://localhost:8080", From: "sender@example.test"}
	for _, kind := range []string{"password_recovery", "password_changed"} {
		m := AccountMessage{Template: kind, Recipient: "customer@example.test", Operation: uuid.NewString()}
		if kind == "password_recovery" {
			m.Token = proof(t)
		}
		data, err := accountMessage(s, m)
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
		if len(data) > 2048 || !strings.Contains(string(body), "http://localhost:8080/painel/") {
			t.Fatal("template bounds/origin changed")
		}
		if kind == "password_recovery" && !strings.Contains(string(body), "/painel/redefinir#recuperar="+m.Token) {
			t.Fatal("missing recovery link")
		}
		if kind == "password_changed" && strings.Contains(string(body), "#") {
			t.Fatal("notice has a capability")
		}
		encoded, err := json.Marshal(m)
		if err != nil {
			t.Fatal(err)
		}
		for _, text := range []string{fmt.Sprintf("%+v", m), string(encoded)} {
			if !strings.Contains(text, "redacted") || strings.Contains(text, m.Recipient) {
				t.Fatal("message leaked")
			}
		}
	}
	for _, m := range []AccountMessage{
		{Template: "password_changed", Recipient: "customer@example.test", Token: "password-must-never-be-sent", Operation: uuid.NewString()},
		{Template: "untrusted", Recipient: "customer@example.test", Token: proof(t), Operation: uuid.NewString()},
	} {
		if _, err := accountMessage(s, m); err == nil {
			t.Fatal("unsafe template accepted")
		}
	}
}

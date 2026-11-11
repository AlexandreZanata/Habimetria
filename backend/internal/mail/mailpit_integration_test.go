package mail

import (
	"context"
	"github.com/google/uuid"
	"strings"
	"testing"
	"time"
	"vault/internal/testutil/mailfixture"
)

func TestSMTPMailpitDeliveryIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	sender, err := NewSMTP(Settings{Address: capture.SMTP, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	token, operation := proof(t), uuid.NewString()
	if err := sender.SendVerification(ctx, "recipient@example.test", token, operation); err != nil {
		t.Fatal(err)
	}
	message := capture.Find(t, operation, "localhost")
	if !strings.Contains(message.Text, "#verificar="+token) {
		t.Fatal("captured real SMTP proof differs")
	}
}

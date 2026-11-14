package mail

import (
	"context"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"net"
	"strings"
	"testing"
	"time"

	"vault/internal/accountproof"

	"github.com/google/uuid"
)

func proof(t *testing.T) string {
	t.Helper()
	key, err := accountproof.New([]byte(strings.Repeat("s", 32)))
	if err != nil {
		t.Fatal(err)
	}
	_, sealed := key.Issue("fixture", "fixture")
	value, err := key.Open("fixture", "fixture", sealed)
	if err != nil {
		t.Fatal(err)
	}
	return value
}

func TestSMTPVerifiedTLSAndAuthentication(t *testing.T) {
	for _, mode := range []string{"starttls", "tls"} {
		t.Run(mode, func(t *testing.T) {
			server := smtpServer(t, mode, true, "")
			sender, err := NewSMTP(Settings{Address: server.address, Mode: mode, From: "sender@example.test", Origin: "https://accounts.example.test", Username: "fixture-user", Roots: server.roots}, []byte("fixture-password"))
			if err != nil {
				t.Fatal(err)
			}
			if err := sender.SendVerification(context.Background(), "recipient@example.test", proof(t), uuid.NewString()); err != nil {
				t.Fatal(err)
			}
			got := <-server.observed
			if !got.secure || !got.authenticated || !strings.Contains(got.body, "Message-ID:") {
				t.Fatal("SMTP bypassed verified TLS/authentication or omitted identity")
			}
			encoded, err := json.Marshal(sender)
			if err != nil {
				t.Fatal(err)
			}
			for _, text := range []string{fmt.Sprintf("%#v", sender), string(encoded)} {
				if !strings.Contains(text, "redacted") || strings.Contains(text, "fixture-password") {
					t.Fatal("SMTP credential formatting exposed")
				}
			}
		})
	}
}

func TestSMTPRejectsDowngradeAndUntrustedCertificate(t *testing.T) {
	for _, missingTLS := range []bool{true, false} {
		server := smtpServer(t, "starttls", !missingTLS, "")
		sender, err := NewSMTP(Settings{Address: server.address, Mode: "starttls", From: "sender@example.test", Origin: "https://accounts.example.test", Username: "fixture-user", Roots: x509.NewCertPool()}, []byte("fixture-password"))
		if err != nil {
			t.Fatal(err)
		}
		err = sender.SendVerification(context.Background(), "recipient@example.test", proof(t), uuid.NewString())
		code, retry := Failure(err)
		if err == nil || code != "tls" || retry {
			t.Fatal("missing/untrusted TLS accepted or downgraded")
		}
		if got := <-server.observed; got.authenticated || got.body != "" {
			t.Fatal("SMTP sent credential/body before TLS validation")
		}
	}
}

func TestSMTPFailuresAndCancellation(t *testing.T) {
	for _, reply := range []string{"450 temporary recipient@example.test", "550 secret fixture-password recipient@example.test"} {
		server := smtpServer(t, "plain", false, reply)
		sender, err := NewSMTP(Settings{Address: server.address, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
		if err != nil {
			t.Fatal(err)
		}
		err = sender.SendVerification(context.Background(), "recipient@example.test", proof(t), uuid.NewString())
		code, retry := Failure(err)
		if err == nil || retry != strings.HasPrefix(reply, "450") || (code != "transport" && code != "rejected") {
			t.Fatal("wrong SMTP failure classification")
		}
		for _, format := range []string{"%v", "%+v", "%#v"} {
			if strings.Contains(fmt.Sprintf(format, err), "recipient@example.test") {
				t.Fatal("SMTP server text leaked")
			}
		}
	}
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	accepted := make(chan net.Conn, 1)
	go func() {
		connection, err := listener.Accept()
		if err == nil {
			accepted <- connection
		}
	}()
	sender, err := NewSMTP(Settings{Address: listener.Addr().String(), Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 50*time.Millisecond)
	defer cancel()
	if err := sender.SendVerification(ctx, "recipient@example.test", proof(t), uuid.NewString()); err == nil {
		t.Fatal("unresponsive SMTP accepted")
	}
	connection := <-accepted
	connection.Close()
}

func TestTLSConnectionDropRemainsRetryable(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		connection, err := listener.Accept()
		if err == nil {
			connection.Close()
		}
	}()
	sender, err := NewSMTP(Settings{Address: listener.Addr().String(), Mode: "tls", From: "sender@example.test", Origin: "https://accounts.example.test"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	err = sender.SendVerification(context.Background(), "recipient@example.test", proof(t), uuid.NewString())
	code, retry := Failure(err)
	if err == nil || code != "transport" || !retry {
		t.Fatal("TLS transport outage was made permanent")
	}
}

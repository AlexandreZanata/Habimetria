package mail

import (
	"bufio"
	"crypto/tls"
	"crypto/x509"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

type smtpObservation struct {
	body                  string
	authenticated, secure bool
}
type smtpFixture struct {
	address  string
	roots    *x509.CertPool
	observed chan smtpObservation
}

func smtpServer(t *testing.T, mode string, advertise bool, rejection string) smtpFixture {
	t.Helper()
	httpServer := httptest.NewTLSServer(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {}))
	certificate := httpServer.TLS.Certificates[0]
	roots := x509.NewCertPool()
	roots.AddCert(httpServer.Certificate())
	httpServer.Close()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	tlsConfig := &tls.Config{Certificates: []tls.Certificate{certificate}, MinVersion: tls.VersionTLS12}
	if mode == "tls" {
		listener = tls.NewListener(listener, tlsConfig)
	}
	observed := make(chan smtpObservation, 1)
	go func() {
		connection, err := listener.Accept()
		if err != nil {
			return
		}
		defer connection.Close()
		_ = connection.SetDeadline(time.Now().Add(5 * time.Second))
		state := smtpObservation{secure: mode == "tls"}
		defer func() { observed <- state }()
		reader := bufio.NewReader(connection)
		write := func(text string) bool { _, err := io.WriteString(connection, text+"\r\n"); return err == nil }
		if !write("220 fixture ESMTP") {
			return
		}
		for {
			line, err := reader.ReadString('\n')
			if err != nil {
				return
			}
			line = strings.TrimSpace(line)
			switch {
			case strings.HasPrefix(line, "EHLO"):
				if advertise && !state.secure {
					write("250-fixture\r\n250-STARTTLS\r\n250 AUTH PLAIN")
				} else {
					write("250-fixture\r\n250 AUTH PLAIN")
				}
			case line == "STARTTLS":
				if !write("220 ready") {
					return
				}
				secure := tls.Server(connection, tlsConfig)
				if secure.Handshake() != nil {
					return
				}
				connection = secure
				reader = bufio.NewReader(connection)
				state.secure = true
			case strings.HasPrefix(line, "AUTH"):
				state.authenticated = true
				if !state.secure {
					write("535 insecure auth")
					return
				}
				write("235 accepted")
			case strings.HasPrefix(line, "MAIL FROM:"):
				write("250 accepted")
			case strings.HasPrefix(line, "RCPT TO:"):
				if rejection != "" {
					write(rejection)
				} else {
					write("250 accepted")
				}
			case line == "DATA":
				write("354 continue")
				for {
					line, err := reader.ReadString('\n')
					if err != nil {
						return
					}
					if line == ".\r\n" {
						break
					}
					state.body += line
				}
				write("250 accepted")
			case line == "QUIT":
				write("221 bye")
				return
			default:
				write("250 accepted")
			}
		}
	}()
	t.Cleanup(func() { _ = listener.Close() })
	return smtpFixture{listener.Addr().String(), roots, observed}
}

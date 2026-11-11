package mailfixture

import (
	"encoding/json"
	"errors"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"testing"
	"time"
)

type Capture struct {
	SMTP, HTTP string
	client     *http.Client
}
type Message struct {
	Text string
	To   []struct{ Address string }
}

func New(t *testing.T) *Capture {
	t.Helper()
	address, origin := os.Getenv("MAIL_TEST_SMTP"), os.Getenv("MAIL_TEST_HTTP")
	if address == "" && origin == "" {
		t.Skip("MAIL_TEST_SMTP/HTTP unset: SMTP capture unverified")
	}
	host, port, err := net.SplitHostPort(address)
	n, pErr := strconv.Atoi(port)
	if err != nil || pErr != nil || host != "127.0.0.1" || n < 1 || n > 65535 {
		t.Fatal("SMTP fixture requires an explicit loopback port")
	}
	u, err := url.Parse(origin)
	if err != nil || u.Scheme != "http" || u.Hostname() != "127.0.0.1" || u.User != nil || u.Path != "" || u.RawQuery != "" || u.Fragment != "" {
		t.Fatal("mail capture requires an explicit loopback HTTP origin")
	}
	n, err = strconv.Atoi(u.Port())
	if err != nil || n < 1 || n > 65535 {
		t.Fatal("invalid mail capture port")
	}
	client := &http.Client{Timeout: 3 * time.Second, Transport: &http.Transport{Proxy: nil}, CheckRedirect: func(*http.Request, []*http.Request) error { return errors.New("fixture redirect denied") }}
	t.Cleanup(client.CloseIdleConnections)
	return &Capture{address, origin, client}
}

func (c *Capture) Find(t *testing.T, operation, domain string) Message {
	t.Helper()
	read := func(path string, out any) {
		t.Helper()
		response, err := c.client.Get(c.HTTP + path)
		if err != nil {
			t.Fatal("mail capture unavailable")
		}
		defer response.Body.Close()
		if response.StatusCode != 200 || json.NewDecoder(io.LimitReader(response.Body, 1<<20)).Decode(out) != nil {
			t.Fatal("invalid mail capture response")
		}
	}
	for range 40 {
		var found struct{ Messages []struct{ ID string } }
		read("/api/v1/search?query="+url.QueryEscape("message-id:"+operation+".email-verification@"+domain), &found)
		if len(found.Messages) > 0 {
			var message Message
			read("/api/v1/message/"+url.PathEscape(found.Messages[0].ID), &message)
			return message
		}
		time.Sleep(50 * time.Millisecond)
	}
	t.Fatal("accepted SMTP message not captured")
	return Message{}
}

package mail

import (
	"crypto/x509"
	"errors"
	"net"
	stdmail "net/mail"
	"net/url"
	"strconv"
	"strings"
)

var ErrConfiguration = errors.New("invalid account mail configuration")

type Settings struct {
	Address, Mode, From, Username, Origin string
	Roots                                 *x509.CertPool
}

func loopback(host string) bool {
	if strings.EqualFold(host, "localhost") {
		return true
	}
	ip := net.ParseIP(host)
	return ip != nil && ip.IsLoopback()
}

func validAddress(address string) bool {
	a, err := stdmail.ParseAddress(address)
	if err != nil || a.Address != address || len(address) > 254 || strings.ContainsAny(address, "\r\n") {
		return false
	}
	for _, r := range address {
		if r > 127 {
			return false
		}
	}
	return true
}

func (s Settings) Validate() error {
	host, port, err := net.SplitHostPort(s.Address)
	n, portErr := strconv.Atoi(port)
	if err != nil || host == "" || portErr != nil || n < 1 || n > 65535 || !validAddress(s.From) || len(s.Username) > 254 || strings.ContainsAny(s.Username, "\r\n\x00") {
		return ErrConfiguration
	}
	u, err := url.Parse(s.Origin)
	if err != nil || len(s.Origin) > 256 || u.Hostname() == "" || u.User != nil || u.Path != "" || u.RawQuery != "" || u.Fragment != "" || u.Opaque != "" || s.Origin != u.Scheme+"://"+u.Host {
		return ErrConfiguration
	}
	if u.Scheme != "https" && (u.Scheme != "http" || !loopback(u.Hostname())) {
		return ErrConfiguration
	}
	switch s.Mode {
	case "plain":
		if !loopback(host) || !loopback(u.Hostname()) || u.Scheme != "http" || s.Username != "" {
			return ErrConfiguration
		}
	case "starttls", "tls":
	default:
		return ErrConfiguration
	}
	return nil
}

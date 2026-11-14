package accountproof_test

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"vault/internal/accountproof"
)

func TestProofBindingAndRedaction(t *testing.T) {
	key, err := accountproof.New(bytes.Repeat([]byte{47}, 32))
	if err != nil {
		t.Fatal(err)
	}
	hash, envelope := key.Issue("user", "operation")
	token, err := key.Open("user", "operation", envelope)
	if err != nil || len(token) != 43 || len(envelope) != 61 {
		t.Fatalf("proof roundtrip: %v", err)
	}
	digest, err := accountproof.Digest(token)
	if err != nil || !bytes.Equal(hash, digest) {
		t.Fatal("verification digest differs")
	}
	for _, binding := range [][2]string{{"other", "operation"}, {"user", "other"}, {"user:operation", ""}} {
		if _, err := key.Open(binding[0], binding[1], envelope); err == nil {
			t.Fatal("proof escaped binding")
		}
	}
	for _, candidate := range [][]byte{nil, envelope[:30], append([]byte{2}, envelope[1:]...), append([]byte(nil), envelope...)} {
		if len(candidate) == 61 && candidate[0] == 1 {
			candidate[30] ^= 1
		}
		if _, err := key.Open("user", "operation", candidate); err == nil {
			t.Fatal("altered envelope accepted")
		}
	}
	other, _ := accountproof.New(bytes.Repeat([]byte{48}, 32))
	if _, err := other.Open("user", "operation", envelope); err == nil {
		t.Fatal("wrong master accepted")
	}
	secondHash, second := key.Issue("user", "operation")
	if bytes.Equal(hash, secondHash) || bytes.Equal(envelope, second) {
		t.Fatal("proof/nonce reused")
	}
	if bytes.Equal(key.Throttle("source", "same"), key.Throttle("address", "same")) {
		t.Fatal("throttle domains mixed")
	}
	if !bytes.Equal(key.Throttle("address", "same"), key.Throttle("address", "same")) {
		t.Fatal("throttle unstable")
	}
	encoded, err := json.Marshal(key)
	if err != nil {
		t.Fatal(err)
	}
	for _, text := range []string{fmt.Sprintf("%v", key), fmt.Sprintf("%+v", key), fmt.Sprintf("%#v", key), string(encoded)} {
		if !strings.Contains(text, "redacted") {
			t.Fatal("key formatting not redacted")
		}
	}
	for _, bad := range []string{"", strings.Repeat("a", 42), strings.Repeat("!", 43), token + "=", token + "\n"} {
		if _, err := accountproof.Digest(bad); err == nil {
			t.Fatal("invalid token accepted")
		}
	}
	if _, err := accountproof.New([]byte("short")); err == nil {
		t.Fatal("short master accepted")
	}
}

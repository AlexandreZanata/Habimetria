package accountproof

import (
	"bytes"
	"testing"
)

func TestRecoveryPurposeIsolation(t *testing.T) {
	k, err := New(bytes.Repeat([]byte{31}, 32))
	if err != nil {
		t.Fatal(err)
	}
	digest, envelope := k.IssueRecovery("user", "operation")
	token, err := k.OpenRecovery("user", "operation", envelope)
	if err != nil {
		t.Fatal(err)
	}
	actual, err := Digest(token)
	if err != nil || !bytes.Equal(digest, actual) {
		t.Fatal("recovery digest differs")
	}
	if _, err = k.Open("user", "operation", envelope); err == nil {
		t.Fatal("recovery opened as verification")
	}
	_, verification := k.Issue("user", "operation")
	if _, err = k.OpenRecovery("user", "operation", verification); err == nil {
		t.Fatal("verification opened as recovery")
	}
	for _, tc := range [][2]string{{"other", "operation"}, {"user", "other"}} {
		if _, err = k.OpenRecovery(tc[0], tc[1], envelope); err == nil {
			t.Fatal("recovery binding bypassed")
		}
	}
}

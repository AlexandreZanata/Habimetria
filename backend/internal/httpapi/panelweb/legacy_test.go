package panelweb_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"vault/internal/httpapi/panelweb"
)

func TestAbsentLegacyAuthorizationFailsClosed(t *testing.T) {
	for _, policy := range []panelweb.LegacyPolicy{{}, {AllowUnclaimedDevelopment: true}} {
		out := httptest.NewRecorder()
		panelweb.ProtectLegacy(nil, "", policy)("properties", func(http.ResponseWriter, *http.Request) {
			t.Fatal("missing authorization dispatched")
		})(out, httptest.NewRequest("POST", "/api/v1/properties", nil))
		if out.Code != 503 {
			t.Fatalf("nil authorization returned %d", out.Code)
		}
	}
}

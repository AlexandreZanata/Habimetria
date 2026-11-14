package routepolicy

import (
	"net/http"
	"sort"
)

type Guard func(Rule, http.Handler) http.Handler

// Router permits registration only from an explicit policy catalog. It never
// exposes its underlying mux, so callers cannot bypass classified registration.
type Router struct {
	mux   *http.ServeMux
	rules map[string]Rule
	bound map[string]Rule
	guard Guard
}

func New(rules []Rule, guard Guard) *Router {
	r := &Router{mux: http.NewServeMux(), rules: map[string]Rule{}, bound: map[string]Rule{}, guard: guard}
	for _, rule := range rules {
		if rule.Pattern == "" {
			panic("empty route policy pattern")
		}
		if _, exists := r.rules[rule.Pattern]; exists {
			panic("duplicate route policy: " + rule.Pattern)
		}
		if rule.Scope != "" && rule.Class != HostedOwner {
			panic("owner scope on a non-owner route: " + rule.Pattern)
		}
		rule.requiresGuard() // Validate even a catalog entry not yet bound.
		r.rules[rule.Pattern] = rule
	}
	return r
}

func (r *Router) Handle(pattern string, handler http.Handler) {
	rule, ok := r.rules[pattern]
	if !ok {
		panic("unclassified route: " + pattern)
	}
	if handler == nil {
		panic("missing route handler: " + pattern)
	}
	if rule.requiresGuard() {
		var protected http.Handler
		if r.guard != nil {
			protected = r.guard(rule, handler)
		}
		if protected == nil {
			protected = http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
				http.Error(w, "hosted access unavailable", http.StatusServiceUnavailable)
			})
		}
		handler = protected
	}
	r.mux.Handle(pattern, handler)
	r.bound[pattern] = rule
}

func (r *Router) HandleFunc(pattern string, handler http.HandlerFunc) {
	if handler == nil {
		panic("missing route handler: " + pattern)
	}
	r.Handle(pattern, handler)
}

func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	r.mux.ServeHTTP(w, req)
}

// Rules returns a detached, sorted inventory of actual registrations.
func (r *Router) Rules() []Rule {
	out := make([]Rule, 0, len(r.bound))
	for _, rule := range r.bound {
		out = append(out, rule)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].Pattern < out[j].Pattern })
	return out
}

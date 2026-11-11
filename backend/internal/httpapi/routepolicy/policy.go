package routepolicy

// Class describes transport authority, not a paid entitlement or domain state.
type Class string

const (
	PublicControl   Class = "public_control"
	AccountControl  Class = "account_control"
	HostedOwner     Class = "hosted_owner"
	StaffControl    Class = "staff_control"
	ReportAccess    Class = "report_access"
	PublicRecipient Class = "public_recipient"
	Delegated       Class = "delegated"
)

// Rule binds a concrete Go ServeMux pattern to its authority boundary.
// Scope is the Property-owner lookup scope for private API resources.
type Rule struct {
	Pattern string
	Class   Class
	Scope   string
}

func (r Rule) requiresGuard() bool {
	switch r.Class {
	case PublicControl, PublicRecipient, Delegated:
		return false
	case AccountControl, HostedOwner, StaffControl, ReportAccess:
		return true
	default:
		panic("unknown route policy class: " + string(r.Class))
	}
}

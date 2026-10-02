// Command verify is the independent offline verifier for local evidence
// packages (see docs/03-architecture/offline-package.md). It shares no
// code with the Kotlin app: same published format, separate
// implementation, separate runtime.
//
// Usage: verify [-trust keyId,...] [-certified keyId,...] [-format text|json] <package-dir>
//
// Exit 0 when the package is accepted, 1 when rejected, 2 on usage errors.
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strings"
)

func splitSet(s string) map[string]bool {
	out := map[string]bool{}
	for _, part := range strings.Split(s, ",") {
		part = strings.TrimSpace(part)
		if part != "" {
			out[part] = true
		}
	}
	return out
}

func main() {
	trustFlag := flag.String("trust", "", "comma-separated trusted key fingerprints (PINNED)")
	certFlag := flag.String("certified", "", "comma-separated certified key fingerprints")
	format := flag.String("format", "text", "output format: text or json")
	flag.Parse()
	if flag.NArg() != 1 {
		fmt.Fprintln(os.Stderr, "usage: verify [-trust k,...] [-certified k,...] [-format text|json] <package-dir>")
		os.Exit(2)
	}
	v := verifyPackage(flag.Arg(0), splitSet(*trustFlag), splitSet(*certFlag))
	if *format == "json" {
		raw, _ := json.MarshalIndent(v, "", "  ")
		fmt.Println(string(raw))
	} else {
		fmt.Printf("pdf: %s\nreceipt: %s\nmanifest: %s\n", v.PDF, v.Receipt, v.Manifest)
		fmt.Printf("evidences: %d checked, %d missing, %d modified\n",
			v.EvidencesChecked, len(v.EvidencesMissing), len(v.EvidencesModified))
		fmt.Printf("signature: %v\nissuer trust: %s\n", v.SignatureValid, v.IssuerTrust)
		fmt.Printf("verdict: %s\n", map[bool]string{true: "ACCEPT", false: "REJECT"}[v.Accepted])
	}
	if !v.Accepted {
		os.Exit(1)
	}
}

package main

import (
	"bytes"
	"fmt"
)

// canonicalReceipt rebuilds the exact signed bytes: fixed field order,
// matching the Kotlin receiptBytes builder.
func canonicalReceipt(r receiptBody) ([]byte, error) {
	var buf bytes.Buffer
	buf.WriteString(`{"algorithm":"ECDSA-P256-SHA256-raw64","formatVersion":1,`)
	writeField(&buf, "manifestId", r.ManifestID)
	writeField(&buf, "manifestSha256", r.ManifestSHA)
	writeField(&buf, "pdfSha256", r.PDFSHA)
	buf.WriteString(`"purpose":"local-receipt/v1",`)
	writeField(&buf, "reportId", r.ReportID)
	buf.WriteString(`"signerKeyId":`)
	writeJSONString(&buf, r.SignerKeyID)
	buf.WriteString(`}`)
	return buf.Bytes(), nil
}

func writeField(buf *bytes.Buffer, name, value string) {
	buf.WriteByte('"')
	buf.WriteString(name)
	buf.WriteString(`":`)
	writeJSONString(buf, value)
	buf.WriteByte(',')
}

func writeJSONString(buf *bytes.Buffer, s string) {
	buf.WriteByte('"')
	for _, r := range s {
		switch r {
		case '"':
			buf.WriteString(`\"`)
		case '\\':
			buf.WriteString(`\\`)
		case '\n':
			buf.WriteString(`\n`)
		case '\r':
			buf.WriteString(`\r`)
		case '\t':
			buf.WriteString(`\t`)
		default:
			if r < 0x20 {
				fmt.Fprintf(buf, `\u%04x`, r)
			} else {
				buf.WriteRune(r)
			}
		}
	}
	buf.WriteByte('"')
}

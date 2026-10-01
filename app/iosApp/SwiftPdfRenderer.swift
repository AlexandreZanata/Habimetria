import CoreImage
import PDFKit
import UIKit
import shared

// Swift PDF pipeline for the shared UI. Implements the Kotlin
// PlatformPdf protocol: same pages as Android (cover + rooms + seal).
// Compiled on macOS (Xcode/CI); Linux klib checks only the Kotlin side.
final class SwiftPdfRenderer: NSObject, PlatformPdf {
    private let page = CGRect(x: 0, y: 0, width: 595, height: 842)

    func qr(content: String, sizePx: Int32) -> Data {
        let filter = CIFilter(name: "CIQRCodeGenerator")!
        filter.setValue(Data(content.utf8), forKey: "inputMessage")
        let raw = filter.outputImage!
        let scale = CGFloat(sizePx) / raw.extent.width
        let scaled = raw.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let context = CIContext()
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return Data() }
        return UIImage(cgImage: cg).pngData() ?? Data()
    }

    func render(doc: PdfDoc, seal: SealBlock) -> Data {
        let pdf = PDFDocument()
        var pages: [PDFPage] = []
        pages.append(textPage(lines: [
            (doc.title, 20), ("Verify code: \(doc.verifyCode)", 12),
            (String(doc.verifyUrl.prefix(64)), 10),
            ("Sealed (device claim): \(doc.sealedAt)", 10),
        ]))
        for section in doc.sections {
            let renderer = UIGraphicsPDFRenderer(bounds: page)
            let data = renderer.pdfData { ctx in
                ctx.beginPage()
                var y = 80.0
                y = self.draw(section.label, size: 20, y: y, in: ctx.cgContext) + 12
                for photo in section.photos {
                    if y < 640, let img = UIImage(data: photo.bytes) {
                        let w = 515.0
                        let h = min(w * img.size.height / img.size.width, 300)
                        img.draw(in: CGRect(x: 40, y: y, width: w, height: h))
                        y += h + 8
                    }
                    y = self.draw(String(photo.caption.prefix(70)), size: 10, y: y) + 6
                }
                for finding in section.findings where y < 780 {
                    y = self.draw("• \(finding.preset) — \(finding.note ?? "")".prefix(70).description,
                                  size: 10, y: y) + 6
                }
            }
            if let p = PDFDocument(data: data)?.page(at: 0) { pages.append(p) }
        }
        pages.append(qrPage(doc: doc))
        let out = PDFDocument()
        pages.enumerated().forEach { i, p in out.insert(p, at: i) }
        return out.dataRepresentation() ?? Data()
    }

    func share(path: String, mime: String) {
        let sheet = UIActivityViewController(
            activityItems: [URL(fileURLWithPath: path)], applicationActivities: nil)
        UIApplication.shared.keyWindow?.rootViewController?.present(sheet, animated: true)
    }

    // MARK: - helpers

    private func textPage(lines: [(String, CGFloat)]) -> PDFPage {
        let renderer = UIGraphicsPDFRenderer(bounds: page)
        let data = renderer.pdfData { ctx in
            ctx.beginPage()
            var y = 80.0
            for (line, size) in lines { y = self.draw(line, size: size, y: y, in: ctx.cgContext) + 12 }
        }
        return PDFDocument(data: data)!.page(at: 0)!
    }

    private func qrPage(doc: PdfDoc) -> PDFPage {
        let renderer = UIGraphicsPDFRenderer(bounds: page)
        let data = renderer.pdfData { ctx in
            ctx.beginPage()
            var y = 80.0
            y = self.draw("Verify this report", size: 20, y: y, in: ctx.cgContext) + 12
            y = self.draw(String(doc.verifyUrl.prefix(64)), size: 10, y: y, in: ctx.cgContext) + 8
            y = self.draw("Manifest: \(doc.manifestSha.prefix(32))…", size: 10, y: y, in: ctx.cgContext) + 16
            if let qr = UIImage(data: qr(content: doc.verifyUrl, sizePx: 256)) {
                qr.draw(in: CGRect(x: 40, y: y, width: 220, height: 220))
            }
        }
        return PDFDocument(data: data)!.page(at: 0)!
    }

    private func draw(_ line: String, size: CGFloat, y: CGFloat, in ctx: CGContext) -> CGFloat {
        // CoreText-free path: NSString drawing.
        let rect = CGRect(x: 40, y: y, width: 515, height: size + 8)
        (line as NSString).draw(in: rect, withAttributes: [.font: UIFont.systemFont(ofSize: size)])
        return y + size + 8
    }
}

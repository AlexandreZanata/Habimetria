package vault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import vault.local.FileStatus
import vault.local.IssuerTrust
import vault.local.PackageVerdict

@Composable
internal fun VerdictBlock(verdict: PackageVerdict) {
    Text(
        if (verdict.accepted()) "✓ ${Strings.pdfChecks}" else "✗ ${Strings.review}",
        color = if (verdict.accepted()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        style = MaterialTheme.typography.titleMedium,
    )
    Text(when { !verdict.declarationsIntact -> "Declarações: verificação falhou ou não pôde ser concluída"
        verdict.declarations.isEmpty() -> "Declarações: nenhuma registrada neste pacote"
        else -> "Declarações e índice: íntegros" })
    verdict.declarations.forEach { DeclarationStatus(it) }
    Text("Assinaturas locais válidas: ${if (verdict.signatureValid) "sim" else "não"}", style = MaterialTheme.typography.bodyMedium)
    VerdictRow("PDF", verdict.pdf)
    VerdictRow(Strings.sealManifestLabel, verdict.manifest)
    VerdictRow(Strings.pdfReceipt, verdict.receipt)
    Text(
        "${Strings.evidence}: ${verdict.evidencesChecked} · anexos e originais " +
            "ausentes ${verdict.evidencesMissing.size} · " +
            "alterados ${verdict.evidencesModified.size}",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (verdict.evidencesMissing.isNotEmpty()) Text(verdict.evidencesMissing.joinToString(", "))
    if (verdict.evidencesModified.isNotEmpty()) Text(verdict.evidencesModified.joinToString(", "))
    Text(
        "${Strings.trustTitle}: " + when (verdict.issuerTrust) {
            IssuerTrust.CERTIFIED -> Strings.trustCertified
            IssuerTrust.PINNED -> Strings.trustPinned
            else -> Strings.trustUnknown
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun VerdictRow(label: String, status: FileStatus) {
    Text(
        "$label: ${when (status) { FileStatus.OK -> "íntegro"; FileStatus.MISSING -> "ausente"; FileStatus.MODIFIED -> "alterado" }}",
        color = if (status == FileStatus.OK) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

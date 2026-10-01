package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.FileStatus
import vault.local.IssuerTrust
import vault.local.LocalSession
import vault.local.PackageVerifier
import vault.local.PackageVerdict
import vault.local.ReportGenerator
import vault.local.db.TrustedIssuer
import vault.local.parseQrIdentity
import vault.local.platformSigner

// LocalVerifyScreen: the five checks separately — PDF integrity,
// manifest integrity, evidence integrity, signature validity and
// issuer trust. QR and package paths import identity; only the
// recomputed bytes grant acceptance. Unknown signers stay unknown.
@Composable
fun LocalVerifyScreen(
    session: LocalSession,
    inspectionId: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val signer = remember(session.filesDir) { platformSigner(session.filesDir) }
    val generator = remember(session, signer) {
        ReportGenerator(session.db, session.filesDir, session.originals, session.derivatives, signer)
    }
    val verifier = remember(signer) { PackageVerifier(signer) }
    var qrText by remember { mutableStateOf("") }
    var packagePath by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<PackageVerdict?>(null) }
    var trusted by remember { mutableStateOf<List<TrustedIssuer>>(emptyList()) }
    var exportDir by remember { mutableStateOf<String?>(null) }
    var trustLabel by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refreshTrust() {
        scope.launch {
            trusted = session.trust.list()
        }
    }

    LaunchedEffect(inspectionId) { refreshTrust() }

    suspend fun currentTrustSets(): Pair<Set<String>, Set<String>> =
        session.trust.trustedKeys() to session.trust.certifiedKeys()

    ScreenColumn {
        ScreenTitle(Strings.verifyTitle)
        ErrorText(error)

        Text(Strings.qrLabel, style = MaterialTheme.typography.titleMedium)
        VaultTextField(value = qrText, onValueChange = { qrText = it }, label = Strings.qrLabel)
        VaultButton(
            text = Strings.qrImport,
            onClick = {
                scope.launch {
                    error = null
                    val ref = parseQrIdentity(qrText)
                    if (ref == null) {
                        error = Strings.qrBad
                        return@launch
                    }
                    try {
                        val report = generator.reportById(ref.reportId)
                        if (report == null || report.manifestSha256 != ref.manifestSha256) {
                            error = Strings.qrNotFound
                            return@launch
                        }
                        val (pinned, certified) = currentTrustSets()
                        val dir = generator.exportPackage(report.id)
                        packagePath = dir
                        exportDir = dir
                        verdict = verifier.verify(dir, pinned, certified)
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
        )

        Text(Strings.packageLabel, style = MaterialTheme.typography.titleMedium)
        VaultTextField(value = packagePath, onValueChange = { packagePath = it }, label = Strings.packageLabel)
        VaultButton(
            text = Strings.packageVerify,
            onClick = {
                scope.launch {
                    error = null
                    try {
                        val (pinned, certified) = currentTrustSets()
                        verdict = verifier.verify(packagePath.trim(), pinned, certified)
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
            enabled = packagePath.trim().isNotEmpty(),
        )
        exportDir?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        verdict?.let { VerdictBlock(it) }

        Text(
            "${Strings.trustTitle}: ${signer.keyId.take(16)}… " +
                "(${if (signer.hardwareBacked) Strings.sealHardware else Strings.sealSoftware})",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultTextField(
                value = trustLabel,
                onValueChange = { trustLabel = it },
                label = Strings.trustNameHint,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            session.trust.trust(signer.keyId, trustLabel.ifBlank { signer.keyId.take(8) })
                            trustLabel = ""
                            refreshTrust()
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        }
                    }
                },
            ) { Text(Strings.trustPin) }
        }
        trusted.forEach { issuer ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${issuer.label} · ${issuer.keyId.take(12)}… · " + when (issuer.source) {
                        "CERTIFIED" -> Strings.trustCertified
                        else -> Strings.trustPinned
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            session.trust.revoke(issuer.keyId)
                            refreshTrust()
                        }
                    },
                ) { Text(Strings.trustRevoke) }
            }
        }
        VaultButton(
            text = Strings.exportPackage,
            onClick = {
                scope.launch {
                    try {
                        val manifest = generator.manifestFor(inspectionId)
                        val reportId = manifest?.let { generator.existingReport(it.id)?.id }
                        if (reportId == null) {
                            error = Strings.pdfSealFirst
                            return@launch
                        }
                        exportDir = generator.exportPackage(reportId)
                        packagePath = exportDir.orEmpty()
                    } catch (e: Exception) {
                        error = userErrorMessage(e)
                    }
                }
            },
        )
        VaultButton(text = Strings.back, onClick = onBack)
    }
}

@Composable
private fun VerdictBlock(verdict: PackageVerdict) {
    Text(
        if (verdict.accepted()) "✓ ${Strings.pdfChecks}" else "✗ ${Strings.review}",
        color = if (verdict.accepted()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        style = MaterialTheme.typography.titleMedium,
    )
    VerdictRow("PDF", verdict.pdf)
    VerdictRow(Strings.sealManifestLabel, verdict.manifest)
    VerdictRow(Strings.pdfReceipt, verdict.receipt)
    Text(
        "${Strings.evidence}: ${verdict.evidencesChecked} · " +
            "ausentes ${verdict.evidencesMissing.size} · " +
            "alteradas ${verdict.evidencesModified.size}",
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
        "$label: ${status.name.lowercase()}",
        color = if (status == FileStatus.OK) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

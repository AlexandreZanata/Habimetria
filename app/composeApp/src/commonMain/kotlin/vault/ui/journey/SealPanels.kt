package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import vault.local.SealEligibility
import vault.local.db.LocalManifest
import vault.local.formatSealedAtDevice
import vault.ui.Strings
import vault.ui.shotLabel

internal object SealStrings {
    const val beforeTitle = "O que acontece ao lacrar"
    const val beforeBody = "O lacre fixa as fotos e os registros desta vistoria. Depois, registre novos problemas como ocorrências."
    const val summary = "Resumo da vistoria"
    const val photos = "Fotos"
    const val records = "Dados do lacre"
    const val signingKey = "Chave de assinatura"
    const val hardware = "Protegida por hardware"
    const val software = "Chave de software"
    const val deviceTime = "Horário do lacre"
    const val deviceTimeBody = "O lacre usa o relógio do dispositivo, sem certificação de horário."
    const val manifestHash = "Hash do manifesto"
    const val completedBody = "O registro foi lacrado neste dispositivo. Você já pode gerar o relatório em PDF."
    const val action = "Lacrar vistoria"
    const val reportDetails = "Na revisão, complete os dados profissionais e confirme a autoria antes de lacrar."
}

@Composable
internal fun SealSummary(eligibility: SealEligibility) {
    SealInfoCard(SealStrings.summary) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SealCount(Strings.rooms, eligibility.rooms)
            SealCount(SealStrings.photos, eligibility.evidences)
            SealCount(Strings.findingsList, eligibility.findings)
        }
        val gaps = eligibility.gaps.filter { it.percent < 100 }
        if (gaps.isNotEmpty()) {
            Text(Strings.sealIncomplete, style = MaterialTheme.typography.titleSmall)
            for (gap in gaps) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${gap.label} · ${gap.percent}%", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${Strings.missingShots}: ${gap.missing.joinToString(", ") { shotLabel(it) }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SealRecordDetails(keyId: String, hardwareBacked: Boolean, manifest: LocalManifest?) {
    SealInfoCard(SealStrings.records) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(SealStrings.signingKey, style = MaterialTheme.typography.titleSmall)
            SealFingerprint(manifest?.signerKeyId ?: keyId)
            Text(
                if (manifest?.hardwareBacked ?: hardwareBacked) SealStrings.hardware else SealStrings.software,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(SealStrings.deviceTime, style = MaterialTheme.typography.titleSmall)
            if (manifest != null) {
                Text(formatSealedAtDevice(manifest.sealedAtDevice), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                SealStrings.deviceTimeBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (manifest != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(SealStrings.manifestHash, style = MaterialTheme.typography.titleSmall)
                SealFingerprint(manifest.canonicalSha256)
            }
        }
    }
}

@Composable
private fun SealFingerprint(value: String) {
    Text(
        if (value.length > 16) "${value.take(16)}…" else value,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    )
}

@Composable
private fun SealCount(label: String, value: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHighest) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(value.toString(), color = colors.primary, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SealInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

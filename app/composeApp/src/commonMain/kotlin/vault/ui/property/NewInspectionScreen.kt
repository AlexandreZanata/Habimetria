package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.ui.AppContainer
import vault.ui.BrandWhite
import vault.ui.ErrorText
import vault.ui.ScreenTitle
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.userErrorMessage

// Dedicated address screen reached from the Home hero. The form gets
// full height here instead of squeezing under the Home cards; input
// survives rotation via saveable state, prefill from the latest
// Property is a hint only and never fabricated on submit.
@Composable
fun NewInspectionScreen(
    container: AppContainer,
    onReady: (inspectionId: String) -> Unit,
) {
    var street by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var complement by rememberSaveable { mutableStateOf("") }
    var neighborhood by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var technical by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val local = container.local

    LaunchedEffect(local) {
        try {
            local?.repository?.propertyOverviews(limit = 1)?.firstOrNull()?.property?.let {
                if (street.isBlank() && it.street.isNotBlank()) street = it.street
                if (number.isBlank() && it.number.isNotBlank()) number = it.number
                if (complement.isBlank()) complement = it.complement.orEmpty()
                if (neighborhood.isBlank() && it.neighborhood.isNotBlank()) neighborhood = it.neighborhood
            }
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(PropertyStrings.inspectionAddressTitle)
        if (local != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !technical,
                    onClick = { technical = false },
                    enabled = !busy,
                    label = { Text(PropertyStrings.rentalMode) },
                )
                FilterChip(
                    selected = technical,
                    onClick = { technical = true },
                    enabled = !busy,
                    label = { Text(PropertyStrings.technicalMode) },
                )
            }
            PropertyAddressForm(
                nickname, { nickname = it }, street, { street = it },
                number, { number = it }, complement, { complement = it },
                neighborhood, { neighborhood = it }, showNickname = true,
            )
        }
        ErrorText(error)
        if (local != null) {
            VaultButton(
                text = if (busy) Strings.starting else Strings.startInspection,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            createPropertyAndStart(
                                container, nickname, street, number, complement, neighborhood, onReady, technical = technical,
                            )
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && isPropertyFormValid(street, number, neighborhood),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = BrandWhite,
                ),
            )
        }
    }
}

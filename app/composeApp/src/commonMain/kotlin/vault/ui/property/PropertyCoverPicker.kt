package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.ui.AppContainer
import vault.ui.CaptureResult
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.captureErrorString
import vault.ui.userErrorMessage

// Cover photo for a Property card: gallery pick or camera shot saved to
// durable storage, path recorded on the row. Organizational only.
@Composable
fun PropertyCoverSection(
    container: AppContainer,
    propertyId: String,
    photoPath: String?,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember(propertyId) { mutableStateOf(false) }
    var error by remember(propertyId) { mutableStateOf<String?>(null) }
    val session = container.local
    val picker = container.filePicker

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(PropertyStrings.coverTitle, style = MaterialTheme.typography.titleMedium)
        PropertyCover(photoPath, Modifier.size(104.dp))
        ErrorText(error)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (picker != null && session != null) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            var picked: vault.ui.PickedLocalFile? = null
                            try {
                                picked = picker.pick("image/*", 10L * 1024 * 1024)
                                if (picked != null) {
                                    val bytes = withContext(Dispatchers.IO) {
                                        FileSystem.SYSTEM.read(picked.path.toPath()) { readByteArray() }
                                    }
                                    withContext(Dispatchers.IO) {
                                        session.propertyCovers.saveCover(propertyId, bytes)
                                    }
                                    session.repository.setPropertyPhoto(
                                        propertyId,
                                        session.propertyCovers.coverPath(propertyId),
                                    )
                                    onSaved()
                                }
                            } catch (e: Exception) {
                                error = userErrorMessage(e)
                            } finally {
                                picked?.let {
                                    runCatching {
                                        FileSystem.SYSTEM.delete(it.path.toPath(), mustExist = false)
                                    }
                                }
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                ) {
                    Text(if (photoPath.isNullOrBlank()) PropertyStrings.coverPick else PropertyStrings.coverChange)
                }
            }
            OutlinedButton(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val shot = when (
                                val result = container.photoCapture.captureDetailed()
                            ) {
                                is CaptureResult.Failure -> {
                                    error = captureErrorString(result.reason)
                                    return@launch
                                }
                                is CaptureResult.Photo -> result
                            }
                            if (session != null) {
                                withContext(Dispatchers.IO) {
                                    session.propertyCovers.saveCover(propertyId, shot.bytes)
                                }
                                session.repository.setPropertyPhoto(
                                    propertyId,
                                    session.propertyCovers.coverPath(propertyId),
                                )
                                onSaved()
                            } else {
                                error = Strings.actionFailed
                            }
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) { Text(Strings.photograph) }
            if (!photoPath.isNullOrBlank() && session != null) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                session.repository.setPropertyPhoto(propertyId, null)
                                withContext(Dispatchers.IO) {
                                    session.propertyCovers.deleteCover(propertyId)
                                }
                                onSaved()
                            } catch (e: Exception) {
                                error = userErrorMessage(e)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                ) { Text(PropertyStrings.coverRemove) }
            }
        }
    }
}

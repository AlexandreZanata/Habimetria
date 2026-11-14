package vault.ui.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
internal fun PropertyCoverActions(
    container: AppContainer,
    propertyId: String,
    photoPath: String?,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember(propertyId) { mutableStateOf(false) }
    var expanded by remember(propertyId) { mutableStateOf(false) }
    var error by remember(propertyId) { mutableStateOf<String?>(null) }
    val session = container.local
    val picker = container.filePicker

    val colors = MaterialTheme.colorScheme
    Box {
        TextButton(onClick = { expanded = true }, enabled = !busy) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (busy) Strings.saving else PropertyStrings.coverTitle)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 184.dp),
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            if (picker != null && session != null) {
                DropdownMenuItem(
                    text = { Text(if (photoPath.isNullOrBlank()) PropertyStrings.coverPick else PropertyStrings.coverChange) },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        expanded = false
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
                )
            }
            DropdownMenuItem(
                text = { Text(Strings.photograph) },
                leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                onClick = {
                    expanded = false
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
            )
            if (!photoPath.isNullOrBlank() && session != null) {
                HorizontalDivider(color = colors.outlineVariant)
                DropdownMenuItem(
                    text = { Text(PropertyStrings.coverRemove) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    colors = MenuDefaults.itemColors(textColor = colors.error, leadingIconColor = colors.error),
                    onClick = {
                        expanded = false
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
                )
            }
        }
    }
    ErrorText(error)
}

package vault.ui.property

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import vault.local.HomeBannerPreference
import vault.ui.Strings

internal class HomeBannerState(private val preference: HomeBannerPreference, private val scope: CoroutineScope) {
    var visible by mutableStateOf<Boolean?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var saving by mutableStateOf(false)
    val canChange: Boolean get() = visible != null && !saving

    suspend fun load() {
        try {
            visible = preference.isVisible()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            visible = true
            error = Strings.homeBannerReadFailed
        }
    }

    fun setVisible(visible: Boolean) {
        if (!canChange) return
        saving = true
        error = null
        scope.launch {
            try {
                preference.setVisible(visible)
                this@HomeBannerState.visible = visible
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = Strings.homeBannerSaveFailed
            } finally {
                saving = false
            }
        }
    }
}

@Composable
internal fun rememberHomeBannerState(preference: HomeBannerPreference): HomeBannerState {
    val scope = rememberCoroutineScope()
    val state = remember(preference) { HomeBannerState(preference, scope) }
    LaunchedEffect(state) { state.load() }
    return state
}

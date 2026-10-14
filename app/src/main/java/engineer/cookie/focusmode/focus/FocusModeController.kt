package engineer.cookie.focusmode.focus

import android.content.Context
import android.content.Intent
import engineer.cookie.focusmode.dnd.DndController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the Focus Mode lifecycle: persists the flag, applies/reverts the DND
 * policy and runs the foreground status service.
 */
class FocusModeController(
    context: Context,
    private val preferences: FocusModePreferences,
    private val dndController: DndController,
) {

    private val appContext = context.applicationContext
    private val _isActive = MutableStateFlow(preferences.isActive)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            _isActive.value = true
            preferences.isActive = true
            dndController.enable()
            startStatusService()
        } else {
            _isActive.value = false
            preferences.isActive = false
            dndController.disable()
            appContext.stopService(Intent(appContext, FocusModeService::class.java))
        }
    }

    private fun startStatusService() {
        val intent = Intent(appContext, FocusModeService::class.java)
            .setAction(FocusModeService.ACTION_START)
        runCatching { appContext.startForegroundService(intent) }
    }
}

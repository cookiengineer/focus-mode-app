package engineer.cookie.focusmode.focus

import android.content.Context
import android.content.Intent
import engineer.cookie.focusmode.dnd.DndController
import engineer.cookie.focusmode.freeze.AppSuspendController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the Focus Mode lifecycle: persists the flag, applies/reverts the DND
 * policy, suspends/releases other user apps and runs the foreground status
 * service.
 */
class FocusModeController(
    context: Context,
    private val preferences: FocusModePreferences,
    private val dndController: DndController,
    private val suspendController: AppSuspendController,
    private val scope: CoroutineScope,
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
            // Freezing every user app is a binder-heavy operation; keep it off the UI thread.
            scope.launch { suspendController.suspendUserApps() }
        } else {
            _isActive.value = false
            preferences.isActive = false
            dndController.disable()
            scope.launch { suspendController.unsuspendAll() }
            appContext.stopService(Intent(appContext, FocusModeService::class.java))
        }
    }

    private fun startStatusService() {
        val intent = Intent(appContext, FocusModeService::class.java)
            .setAction(FocusModeService.ACTION_START)
        runCatching { appContext.startForegroundService(intent) }
    }
}

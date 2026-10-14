package engineer.cookie.focusmode

import android.app.Application
import engineer.cookie.focusmode.focus.NotificationChannels
import engineer.cookie.focusmode.freeze.KeepAliveRepository
import kotlinx.coroutines.launch

class FocusModeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        NotificationChannels.create(this)
        AppGraph.keepAliveRepository.seedDefaults(KeepAliveRepository.DEFAULT_KEEP_ALIVE)
        AppGraph.applicationScope.launch {
            AppGraph.allowlistRepository.refresh()
        }
    }
}

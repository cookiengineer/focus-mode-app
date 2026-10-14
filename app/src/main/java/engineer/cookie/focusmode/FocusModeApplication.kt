package engineer.cookie.focusmode

import android.app.Application
import engineer.cookie.focusmode.focus.NotificationChannels
import kotlinx.coroutines.launch

class FocusModeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        NotificationChannels.create(this)
        AppGraph.applicationScope.launch {
            AppGraph.allowlistRepository.refresh()
        }
    }
}

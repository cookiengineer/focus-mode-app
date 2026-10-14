package engineer.cookie.focusmode

import android.content.Context
import engineer.cookie.focusmode.data.AllowlistRepository
import engineer.cookie.focusmode.data.AppDatabase
import engineer.cookie.focusmode.data.ContactRepository
import engineer.cookie.focusmode.dnd.ContactStarManager
import engineer.cookie.focusmode.dnd.DndController
import engineer.cookie.focusmode.focus.FocusModeController
import engineer.cookie.focusmode.focus.FocusModePreferences
import engineer.cookie.focusmode.freeze.AppCatalog
import engineer.cookie.focusmode.freeze.AppSuspendController
import engineer.cookie.focusmode.freeze.KeepAliveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Minimal service locator. Initialised once from [FocusModeApplication] so both
 * the UI and the background services share the same instances.
 */
object AppGraph {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var appContext: Context

    val preferences: FocusModePreferences by lazy { FocusModePreferences(appContext) }
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val allowlistRepository: AllowlistRepository by lazy {
        AllowlistRepository(database.allowedContactDao(), appContext)
    }
    val contactRepository: ContactRepository by lazy { ContactRepository(appContext) }
    val starManager: ContactStarManager by lazy { ContactStarManager(appContext) }
    val dndController: DndController by lazy {
        DndController(appContext, preferences, allowlistRepository, starManager)
    }
    val appSuspendController: AppSuspendController by lazy {
        AppSuspendController(appContext, preferences)
    }
    val keepAliveRepository: KeepAliveRepository by lazy {
        KeepAliveRepository(preferences)
    }
    val appCatalog: AppCatalog by lazy { AppCatalog(appContext) }
    val focusModeController: FocusModeController by lazy {
        FocusModeController(
            appContext,
            preferences,
            dndController,
            appSuspendController,
            applicationScope,
        )
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

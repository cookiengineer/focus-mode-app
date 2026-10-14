package engineer.cookie.focusmode.dnd

import android.app.NotificationManager
import android.content.Context
import engineer.cookie.focusmode.data.AllowlistRepository
import engineer.cookie.focusmode.focus.FocusModePreferences

/**
 * Applies and reverts the Do Not Disturb policy used by Focus Mode.
 *
 * When enabled it switches the device into priority interruption mode and allows
 * only calls/messages/conversations from *starred* contacts (the allowlist, via
 * [ContactStarManager]). Everything else is silenced.
 *
 * The previously active policy and interruption filter are saved on enable and
 * restored verbatim on disable.
 */
class DndController(
    context: Context,
    private val preferences: FocusModePreferences,
    private val allowlistRepository: AllowlistRepository,
    private val starManager: ContactStarManager,
) {

    private val appContext = context.applicationContext
    private val notificationManager: NotificationManager?
        get() = appContext.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean =
        notificationManager?.isNotificationPolicyAccessGranted == true

    fun enable() {
        val nm = notificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return

        if (!preferences.hasSavedPolicy) {
            val current = nm.notificationPolicy
            preferences.savedInterruptionFilter = nm.currentInterruptionFilter
            preferences.savedPolicyCategories = current.priorityCategories
            preferences.savedPolicyCallSenders = current.priorityCallSenders
            preferences.savedPolicyMessageSenders = current.priorityMessageSenders
            preferences.savedPolicyConversationSenders = current.priorityConversationSenders
            preferences.savedPolicySuppressedEffects = current.suppressedVisualEffects
            preferences.hasSavedPolicy = true
        }

        val newStars = starManager.star(allowlistRepository.snapshot.value.contactIds)
        if (newStars.isNotEmpty()) {
            preferences.selfStarredIds = preferences.selfStarredIds + newStars
        }

        val categories = NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
            NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
            NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS

        val policy = NotificationManager.Policy(
            categories,
            NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
            NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
            /* suppressedVisualEffects = */ 0,
            NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT,
        )
        nm.notificationPolicy = policy
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun disable() {
        val nm = notificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted && preferences.hasSavedPolicy) {
            val policy = NotificationManager.Policy(
                preferences.savedPolicyCategories,
                preferences.savedPolicyCallSenders,
                preferences.savedPolicyMessageSenders,
                preferences.savedPolicySuppressedEffects,
                preferences.savedPolicyConversationSenders,
            )
            runCatching { nm.notificationPolicy = policy }
            runCatching { nm.setInterruptionFilter(preferences.savedInterruptionFilter) }
        }
        starManager.unstar(preferences.selfStarredIds)
        preferences.selfStarredIds = emptySet()
        preferences.clearSavedPolicy()
    }
}

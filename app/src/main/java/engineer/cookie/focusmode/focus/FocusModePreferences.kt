package engineer.cookie.focusmode.focus

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists Focus Mode state plus the Do Not Disturb policy that was active before
 * Focus Mode was enabled, so it can be restored exactly on shutdown.
 */
class FocusModePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)

    var isActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVE, value).apply()

    var hasSavedPolicy: Boolean
        get() = prefs.getBoolean(KEY_HAS_SAVED_POLICY, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SAVED_POLICY, value).apply()

    var savedInterruptionFilter: Int
        get() = prefs.getInt(KEY_INTERRUPTION_FILTER, DEFAULT_INTERRUPTION_FILTER)
        set(value) = prefs.edit().putInt(KEY_INTERRUPTION_FILTER, value).apply()

    var savedPolicyCategories: Int
        get() = prefs.getInt(KEY_POLICY_CATEGORIES, 0)
        set(value) = prefs.edit().putInt(KEY_POLICY_CATEGORIES, value).apply()

    var savedPolicyCallSenders: Int
        get() = prefs.getInt(KEY_POLICY_CALL_SENDERS, 0)
        set(value) = prefs.edit().putInt(KEY_POLICY_CALL_SENDERS, value).apply()

    var savedPolicyMessageSenders: Int
        get() = prefs.getInt(KEY_POLICY_MESSAGE_SENDERS, 0)
        set(value) = prefs.edit().putInt(KEY_POLICY_MESSAGE_SENDERS, value).apply()

    var savedPolicyConversationSenders: Int
        get() = prefs.getInt(KEY_POLICY_CONVERSATION_SENDERS, 0)
        set(value) = prefs.edit().putInt(KEY_POLICY_CONVERSATION_SENDERS, value).apply()

    var savedPolicySuppressedEffects: Int
        get() = prefs.getInt(KEY_POLICY_SUPPRESSED_EFFECTS, 0)
        set(value) = prefs.edit().putInt(KEY_POLICY_SUPPRESSED_EFFECTS, value).apply()

    var selfStarredIds: Set<Long>
        get() = prefs.getStringSet(KEY_SELF_STARRED_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
        set(value) = prefs.edit()
            .putStringSet(KEY_SELF_STARRED_IDS, value.map { it.toString() }.toSet())
            .apply()

    /** Packages Focus Mode suspended, so they can be restored exactly on shutdown. */
    var suspendedPackages: Set<String>
        get() = prefs.getStringSet(KEY_SUSPENDED_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit()
            .putStringSet(KEY_SUSPENDED_PACKAGES, value)
            .apply()

    /** User apps that are never frozen, even while Focus Mode is active. */
    var neverFreezePackages: Set<String>
        get() = prefs.getStringSet(KEY_NEVER_FREEZE_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit()
            .putStringSet(KEY_NEVER_FREEZE_PACKAGES, value)
            .apply()

    var keepAliveSeeded: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ALIVE_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_ALIVE_SEEDED, value).apply()

    fun clearSavedPolicy() {
        prefs.edit()
            .remove(KEY_INTERRUPTION_FILTER)
            .remove(KEY_POLICY_CATEGORIES)
            .remove(KEY_POLICY_CALL_SENDERS)
            .remove(KEY_POLICY_MESSAGE_SENDERS)
            .remove(KEY_POLICY_CONVERSATION_SENDERS)
            .remove(KEY_POLICY_SUPPRESSED_EFFECTS)
            .putBoolean(KEY_HAS_SAVED_POLICY, false)
            .apply()
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_HAS_SAVED_POLICY = "has_saved_policy"
        const val KEY_INTERRUPTION_FILTER = "saved_interruption_filter"
        const val KEY_POLICY_CATEGORIES = "saved_policy_categories"
        const val KEY_POLICY_CALL_SENDERS = "saved_policy_call_senders"
        const val KEY_POLICY_MESSAGE_SENDERS = "saved_policy_message_senders"
        const val KEY_POLICY_CONVERSATION_SENDERS = "saved_policy_conversation_senders"
        const val KEY_POLICY_SUPPRESSED_EFFECTS = "saved_policy_suppressed_effects"
        const val KEY_SELF_STARRED_IDS = "self_starred_ids"
        const val KEY_SUSPENDED_PACKAGES = "suspended_packages"
        const val KEY_NEVER_FREEZE_PACKAGES = "never_freeze_packages"
        const val KEY_KEEP_ALIVE_SEEDED = "keep_alive_seeded"
        const val DEFAULT_INTERRUPTION_FILTER = 1 // NotificationManager.INTERRUPTION_FILTER_ALL
    }
}

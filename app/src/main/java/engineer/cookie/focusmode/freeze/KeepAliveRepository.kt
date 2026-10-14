package engineer.cookie.focusmode.freeze

import engineer.cookie.focusmode.focus.FocusModePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User apps that must never be frozen while Focus Mode is active (for example a
 * firewall/VPN or a browser). Persisted and exposed as a flow for the UI.
 */
class KeepAliveRepository(private val preferences: FocusModePreferences) {

    private val _packages = MutableStateFlow(preferences.neverFreezePackages)
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    fun setKept(packageName: String, kept: Boolean) {
        val updated = if (kept) {
            preferences.neverFreezePackages + packageName
        } else {
            preferences.neverFreezePackages - packageName
        }
        preferences.neverFreezePackages = updated
        _packages.value = updated
    }

    /** Adds [defaults] once, without clobbering choices the user made afterwards. */
    fun seedDefaults(defaults: Set<String>) {
        if (preferences.keepAliveSeeded) return
        preferences.keepAliveSeeded = true
        if (defaults.isEmpty()) return
        val updated = preferences.neverFreezePackages + defaults
        preferences.neverFreezePackages = updated
        _packages.value = updated
    }

    companion object {
        /** Seeded on first run: NetGuard (firewall/VPN) and Fennec (Firefox). */
        val DEFAULT_KEEP_ALIVE = setOf(
            "eu.faircode.netguard",
            "org.mozilla.fennec_fdroid",
        )
    }
}

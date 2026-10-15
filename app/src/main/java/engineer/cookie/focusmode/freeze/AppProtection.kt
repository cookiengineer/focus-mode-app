package engineer.cookie.focusmode.freeze

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityManager

/**
 * Works out which installed packages must never be suspended and which ones are
 * user-facing (have a launcher entry).
 *
 * Suspending a system component - the launcher, SystemUI, Settings, Contacts,
 * the file picker, the default dialer/SMS, an enabled keyboard, an
 * accessibility service or a device admin - would break core functionality, so
 * all of those are protected. Every other user-facing package (including
 * preinstalled apps like Music, Gallery or Browser) may be frozen.
 */
class AppProtection(private val context: Context) {

    /** Package names that must never be suspended, whatever the user chooses. */
    fun protectedPackages(): Set<String> {
        val protected = HashSet<String>(SYSTEM_PROTECTED)
        protected.add(context.packageName)
        protected.addAll(homePackages())
        protected.addAll(enabledInputMethods())
        protected.addAll(enabledAccessibilityPackages())
        protected.addAll(deviceAdminPackages())
        defaultDialerPackage()?.let(protected::add)
        defaultSmsPackage()?.let(protected::add)
        return protected
    }

    /** Installed packages that expose a launcher activity (user-facing apps). */
    fun launchablePackages(): Set<String> = runCatching {
        queryActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        ).mapNotNull { it.activityInfo?.packageName }.toSet()
    }.getOrDefault(emptySet())

    private fun homePackages(): Set<String> = runCatching {
        queryActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        ).mapNotNull { it.activityInfo?.packageName }.toSet()
    }.getOrDefault(emptySet())

    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }

    private fun enabledInputMethods(): Set<String> = runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS,
        )?.split(':')
            ?.map { it.substringBefore('/') }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
    }.getOrDefault(emptySet())

    private fun defaultDialerPackage(): String? =
        runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        }.getOrNull()

    private fun defaultSmsPackage(): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    private fun enabledAccessibilityPackages(): Set<String> = runCatching {
        context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
            ?.toSet()
            .orEmpty()
    }.getOrDefault(emptySet())

    private fun deviceAdminPackages(): Set<String> = runCatching {
        context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            ?.activeAdmins
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
    }.getOrDefault(emptySet())

    private companion object {
        /**
         * Core AOSP/LineageOS packages that always have to stay available. The
         * launcher and keyboards are resolved dynamically on top of these.
         */
        val SYSTEM_PROTECTED = setOf(
            "com.android.systemui",
            "com.android.settings",
            "org.lineageos.lineagesettings",
            "org.lineageos.lineageparts",
            "com.android.contacts",
            "com.android.documentsui",
        )
    }
}

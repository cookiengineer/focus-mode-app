package engineer.cookie.focusmode.freeze

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import engineer.cookie.focusmode.admin.FocusDeviceAdminReceiver
import engineer.cookie.focusmode.focus.FocusModePreferences

/** Outcome of a suspend pass. [failed] are packages the system refused to suspend. */
data class SuspendResult(
    val suspended: List<String>,
    val failed: List<String>,
)

/**
 * Freezes other apps while Focus Mode is active by suspending them as the
 * device owner.
 *
 * A suspended package cannot start activities, its notifications are hidden, it
 * cannot appear in recents, show toasts/dialogs or ring the device. Suspension is
 * instant, reversible and persists across reboots. It requires the app to be the
 * device owner (a one-time `adb shell dpm set-device-owner` step); everything
 * degrades gracefully to a no-op when it is not.
 */
class AppSuspendController(
    context: Context,
    private val preferences: FocusModePreferences,
) {

    private val appContext = context.applicationContext
    private val protection = AppProtection(appContext)
    private val devicePolicyManager: DevicePolicyManager?
        get() = appContext.getSystemService(DevicePolicyManager::class.java)

    private val admin: ComponentName
        get() = ComponentName(appContext, FocusDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean =
        devicePolicyManager?.isDeviceOwnerApp(appContext.packageName) == true

    /**
     * Suspends every eligible app and remembers which ones succeeded so they
     * can be restored exactly on shutdown. No-op when not the device owner.
     */
    fun suspendUserApps(): SuspendResult {
        val dpm = devicePolicyManager
        if (dpm == null || !isDeviceOwner()) return SuspendResult(emptyList(), emptyList())

        val targets = selectTargets()
        if (targets.isEmpty()) {
            preferences.suspendedPackages = emptySet()
            return SuspendResult(emptyList(), emptyList())
        }

        val refused = runCatching {
            dpm.setPackagesSuspended(admin, targets.toTypedArray(), true).toSet()
        }.getOrElse { emptySet() }

        val suspended = targets.filterNot { it in refused }
        preferences.suspendedPackages = suspended.toSet()
        return SuspendResult(suspended, refused.toList())
    }

    /** Restores every app that Focus Mode suspended. No-op when not the device owner. */
    fun unsuspendAll() {
        val dpm = devicePolicyManager ?: return
        if (!isDeviceOwner()) return

        val packages = preferences.suspendedPackages
        if (packages.isEmpty()) return

        runCatching { dpm.setPackagesSuspended(admin, packages.toTypedArray(), false) }
        preferences.suspendedPackages = emptySet()
    }

    /** Removes Focus Mode as device owner (also unsuspends first). */
    fun deactivateDeviceOwner() {
        unsuspendAll()
        val dpm = devicePolicyManager ?: return
        if (!isDeviceOwner()) return
        @Suppress("DEPRECATION")
        runCatching { dpm.clearDeviceOwnerApp(appContext.packageName) }
    }

    private fun selectTargets(): List<String> =
        AppFilter.selectTargets(
            installed = installedApps(),
            ownPackage = appContext.packageName,
            protectedPackages = protection.protectedPackages(),
            launchablePackages = protection.launchablePackages(),
            neverFreezePackages = preferences.neverFreezePackages,
        )

    private fun installedApps(): List<InstalledApp> {
        val pm = appContext.packageManager
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val systemMask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return infos.map { info ->
            InstalledApp(
                packageName = info.packageName,
                isSystem = info.flags and systemMask != 0,
            )
        }
    }
}

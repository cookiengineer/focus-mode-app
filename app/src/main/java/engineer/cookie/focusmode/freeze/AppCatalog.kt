package engineer.cookie.focusmode.freeze

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * An app the user can choose to keep running during Focus Mode. This includes
 * third-party apps as well as user-facing preinstalled apps (Music, Gallery,
 * Browser, ...).
 */
data class UserApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

/**
 * Lists the apps shown in the "Kept running" picker: every third-party app plus
 * the preinstalled apps that have a launcher entry. Core system packages that
 * are never frozen are left out so the picker has no dead switches.
 */
class AppCatalog(private val context: Context) {

    private val protection = AppProtection(context)

    fun userApps(): List<UserApp> {
        val pm = context.packageManager
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val systemMask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        val launchable = protection.launchablePackages()
        val protected = protection.protectedPackages()
        return infos.asSequence()
            .filter { info ->
                val isSystem = info.flags and systemMask != 0
                !isSystem || info.packageName in launchable
            }
            .filterNot { it.packageName in protected }
            .map { info ->
                UserApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    isSystem = info.flags and systemMask != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

package engineer.cookie.focusmode.freeze

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/** A third-party user app the user can choose to keep running during Focus Mode. */
data class UserApp(
    val packageName: String,
    val label: String,
)

/** Lists the third-party user apps shown in the "Kept running" picker. */
class AppCatalog(private val context: Context) {

    fun userApps(): List<UserApp> {
        val pm = context.packageManager
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val systemMask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return infos.asSequence()
            .filter { it.flags and systemMask == 0 }
            .filter { it.packageName != context.packageName }
            .map { UserApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

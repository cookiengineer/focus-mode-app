package engineer.cookie.focusmode.util

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Snapshot of every special access Focus Mode depends on. */
data class AccessState(
    val readContacts: Boolean,
    val writeContacts: Boolean,
    val postNotifications: Boolean,
    val dndPolicy: Boolean,
    val callScreeningRole: Boolean,
    val deviceOwner: Boolean,
) {
    /** Minimum needed for the core (DND + starring) behaviour. */
    val coreGranted: Boolean get() = readContacts && writeContacts && dndPolicy

    val allGranted: Boolean
        get() = coreGranted && postNotifications && callScreeningRole && deviceOwner
}

object SpecialAccess {

    val runtimePermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
    }

    fun current(context: Context): AccessState {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val roleManager = context.getSystemService(RoleManager::class.java)
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        return AccessState(
            readContacts = isGranted(context, Manifest.permission.READ_CONTACTS),
            writeContacts = isGranted(context, Manifest.permission.WRITE_CONTACTS),
            postNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                isGranted(context, Manifest.permission.POST_NOTIFICATIONS),
            dndPolicy = notificationManager?.isNotificationPolicyAccessGranted == true,
            callScreeningRole = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true,
            deviceOwner = devicePolicyManager?.isDeviceOwnerApp(context.packageName) == true,
        )
    }

    /** The exact one-time ADB command that makes Focus Mode the device owner. */
    fun deviceOwnerCommand(context: Context): String =
        "adb shell dpm set-device-owner " +
            "${context.packageName}/engineer.cookie.focusmode.admin.FocusDeviceAdminReceiver"

    fun dndSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun callScreeningRoleIntent(context: Context): Intent? =
        context.getSystemService(RoleManager::class.java)
            ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

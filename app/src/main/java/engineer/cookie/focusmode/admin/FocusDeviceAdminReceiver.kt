package engineer.cookie.focusmode.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Enables Focus Mode to act as the device owner.
 *
 * Device ownership is what allows [engineer.cookie.focusmode.freeze.AppSuspendController]
 * to suspend (freeze) other user apps via `DevicePolicyManager.setPackagesSuspended()`
 * without root. The receiver itself is intentionally empty; it only exists so the
 * system can bind the app as a device administrator / owner.
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver()

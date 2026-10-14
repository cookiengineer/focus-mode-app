package engineer.cookie.focusmode.freeze

/** Platform-independent view of an installed app; kept small so it is unit testable. */
data class InstalledApp(
    val packageName: String,
    val isSystem: Boolean,
)

/**
 * Decides which user apps Focus Mode should suspend.
 *
 * Suspending a system app can destabilise the phone, and suspending the current
 * keyboard, an accessibility service, a device admin, the launcher, the default
 * dialer or the default SMS app would break core functionality. All of those are
 * excluded here.
 */
object AppFilter {

    fun selectTargets(
        installed: List<InstalledApp>,
        ownPackage: String,
        defaultDialer: String?,
        defaultSms: String?,
        defaultIme: String?,
        accessibilityPackages: Set<String>,
        deviceAdminPackages: Set<String>,
        neverFreezePackages: Set<String> = emptySet(),
    ): List<String> {
        val excluded = HashSet<String>()
        excluded.add(ownPackage)
        defaultDialer?.let(excluded::add)
        defaultSms?.let(excluded::add)
        defaultIme?.let(excluded::add)
        excluded.addAll(accessibilityPackages)
        excluded.addAll(deviceAdminPackages)
        excluded.addAll(neverFreezePackages)

        return installed.asSequence()
            .filterNot { it.isSystem }
            .map { it.packageName }
            .filterNot { it in excluded }
            .distinct()
            .sorted()
            .toList()
    }
}

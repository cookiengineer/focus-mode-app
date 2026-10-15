package engineer.cookie.focusmode.freeze

/** Platform-independent view of an installed app; kept small so it is unit testable. */
data class InstalledApp(
    val packageName: String,
    val isSystem: Boolean,
)

/**
 * Decides which apps Focus Mode should suspend.
 *
 * Third-party apps are always eligible. Preinstalled (system) apps are only
 * eligible when they are user-facing - i.e. they expose a launcher activity - so
 * background system components are never touched. Anything in [protectedPackages]
 * (launcher, SystemUI, Settings, Contacts, file picker, default dialer/SMS,
 * enabled keyboards, accessibility services, device admins) or in
 * [neverFreezePackages] (the user's allowlist) is excluded, as is Focus Mode
 * itself.
 */
object AppFilter {

    fun selectTargets(
        installed: List<InstalledApp>,
        ownPackage: String,
        protectedPackages: Set<String>,
        launchablePackages: Set<String>,
        neverFreezePackages: Set<String> = emptySet(),
    ): List<String> {
        val excluded = HashSet<String>()
        excluded.add(ownPackage)
        excluded.addAll(protectedPackages)
        excluded.addAll(neverFreezePackages)

        return installed.asSequence()
            .filter { !it.isSystem || it.packageName in launchablePackages }
            .map { it.packageName }
            .filterNot { it in excluded }
            .distinct()
            .sorted()
            .toList()
    }
}

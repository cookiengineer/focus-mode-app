package engineer.cookie.focusmode

import engineer.cookie.focusmode.freeze.AppFilter
import engineer.cookie.focusmode.freeze.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppFilterTest {

    private val own = "engineer.cookie.focusmode"
    private val dialer = "org.lineageos.dialer"
    private val sms = "com.android.messaging"
    private val ime = "com.android.inputmethod.latin"

    private fun targets(
        installed: List<InstalledApp>,
        accessibility: Set<String> = emptySet(),
        admins: Set<String> = emptySet(),
        neverFreeze: Set<String> = emptySet(),
    ) = AppFilter.selectTargets(
        installed = installed,
        ownPackage = own,
        defaultDialer = dialer,
        defaultSms = sms,
        defaultIme = ime,
        accessibilityPackages = accessibility,
        deviceAdminPackages = admins,
        neverFreezePackages = neverFreeze,
    )

    @Test
    fun keepsOnlyThirdPartyUserApps() {
        val installed = listOf(
            InstalledApp("com.example.chat", isSystem = false),
            InstalledApp("com.example.news", isSystem = false),
            InstalledApp("com.android.systemui", isSystem = true),
            InstalledApp("com.example.clock", isSystem = false),
        )
        assertEquals(
            listOf("com.example.chat", "com.example.clock", "com.example.news"),
            targets(installed),
        )
    }

    @Test
    fun excludesSystemApps() {
        val installed = listOf(
            InstalledApp("com.android.settings", isSystem = true),
            InstalledApp("com.example.game", isSystem = false),
        )
        assertEquals(listOf("com.example.game"), targets(installed))
    }

    @Test
    fun excludesFocusModeItself() {
        val installed = listOf(
            InstalledApp(own, isSystem = false),
            InstalledApp("com.example.music", isSystem = false),
        )
        assertEquals(listOf("com.example.music"), targets(installed))
    }

    @Test
    fun excludesCriticalCommsAndInputApps() {
        val installed = listOf(
            InstalledApp(dialer, isSystem = false),
            InstalledApp(sms, isSystem = false),
            InstalledApp(ime, isSystem = false),
            InstalledApp("com.example.video", isSystem = false),
        )
        assertEquals(listOf("com.example.video"), targets(installed))
    }

    @Test
    fun excludesAccessibilityServicesAndDeviceAdmins() {
        val installed = listOf(
            InstalledApp("com.example.talkback", isSystem = false),
            InstalledApp("com.example.mdm", isSystem = false),
            InstalledApp("com.example.browser", isSystem = false),
        )
        assertEquals(
            listOf("com.example.browser"),
            targets(
                installed,
                accessibility = setOf("com.example.talkback"),
                admins = setOf("com.example.mdm"),
            ),
        )
    }

    @Test
    fun excludesNeverFreezeAllowlist() {
        val installed = listOf(
            InstalledApp("eu.faircode.netguard", isSystem = false),
            InstalledApp("org.mozilla.fennec_fdroid", isSystem = false),
            InstalledApp("com.example.chat", isSystem = false),
        )
        assertEquals(
            listOf("com.example.chat"),
            targets(
                installed,
                neverFreeze = setOf("eu.faircode.netguard", "org.mozilla.fennec_fdroid"),
            ),
        )
    }

    @Test
    fun resultIsSortedAndDeduplicated() {
        val installed = listOf(
            InstalledApp("com.example.z", isSystem = false),
            InstalledApp("com.example.a", isSystem = false),
            InstalledApp("com.example.a", isSystem = false),
        )
        assertEquals(listOf("com.example.a", "com.example.z"), targets(installed))
    }
}

package engineer.cookie.focusmode

import engineer.cookie.focusmode.freeze.AppFilter
import engineer.cookie.focusmode.freeze.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppFilterTest {

    private val own = "engineer.cookie.focusmode"

    private fun targets(
        installed: List<InstalledApp>,
        protected: Set<String> = emptySet(),
        launchable: Set<String> = emptySet(),
        neverFreeze: Set<String> = emptySet(),
    ) = AppFilter.selectTargets(
        installed = installed,
        ownPackage = own,
        protectedPackages = protected,
        launchablePackages = launchable,
        neverFreezePackages = neverFreeze,
    )

    @Test
    fun keepsThirdPartyApps() {
        val installed = listOf(
            InstalledApp("com.example.chat", isSystem = false),
            InstalledApp("com.example.news", isSystem = false),
            InstalledApp("com.example.clock", isSystem = false),
        )
        assertEquals(
            listOf("com.example.chat", "com.example.clock", "com.example.news"),
            targets(installed),
        )
    }

    @Test
    fun keepsLaunchableSystemApps() {
        val installed = listOf(
            InstalledApp("org.lineageos.eleven", isSystem = true),
            InstalledApp("com.android.gallery3d", isSystem = true),
            InstalledApp("com.example.chat", isSystem = false),
        )
        assertEquals(
            listOf("com.android.gallery3d", "com.example.chat", "org.lineageos.eleven"),
            targets(
                installed,
                launchable = setOf("org.lineageos.eleven", "com.android.gallery3d"),
            ),
        )
    }

    @Test
    fun excludesNonLaunchableSystemApps() {
        val installed = listOf(
            InstalledApp("com.android.providers.media", isSystem = true),
            InstalledApp("com.android.systemui", isSystem = true),
            InstalledApp("com.example.game", isSystem = false),
        )
        assertEquals(listOf("com.example.game"), targets(installed))
    }

    @Test
    fun excludesProtectedPackages() {
        val installed = listOf(
            InstalledApp("com.android.settings", isSystem = true),
            InstalledApp("com.android.documentsui", isSystem = true),
            InstalledApp("org.lineageos.eleven", isSystem = true),
            InstalledApp("com.example.browser", isSystem = false),
        )
        assertEquals(
            listOf("com.example.browser", "org.lineageos.eleven"),
            targets(
                installed,
                protected = setOf("com.android.settings", "com.android.documentsui"),
                launchable = setOf(
                    "com.android.settings",
                    "com.android.documentsui",
                    "org.lineageos.eleven",
                ),
            ),
        )
    }

    @Test
    fun excludesFocusModeItself() {
        val installed = listOf(
            InstalledApp(own, isSystem = false),
            InstalledApp("org.lineageos.eleven", isSystem = true),
        )
        assertEquals(
            listOf("org.lineageos.eleven"),
            targets(installed, launchable = setOf("org.lineageos.eleven")),
        )
    }

    @Test
    fun excludesNeverFreezeAllowlist() {
        val installed = listOf(
            InstalledApp("eu.faircode.netguard", isSystem = false),
            InstalledApp("org.mozilla.fennec_fdroid", isSystem = false),
            InstalledApp("org.lineageos.eleven", isSystem = true),
            InstalledApp("com.example.chat", isSystem = false),
        )
        assertEquals(
            listOf("com.example.chat", "org.lineageos.eleven"),
            targets(
                installed,
                launchable = setOf("org.lineageos.eleven"),
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

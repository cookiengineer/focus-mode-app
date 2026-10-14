package engineer.cookie.focusmode

import com.google.i18n.phonenumbers.PhoneNumberUtil
import engineer.cookie.focusmode.util.AllowlistSnapshot
import engineer.cookie.focusmode.util.PhoneMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneMatcherTest {

    private val util = PhoneNumberUtil.getInstance()

    private fun snapshotOf(vararg numbers: String, region: String = "US"): AllowlistSnapshot {
        val parsed = numbers.map { util.parse(it, region) }
        val e164 = parsed.map { util.format(it, PhoneNumberUtil.PhoneNumberFormat.E164) }.toSet()
        val suffixes = numbers.map { raw ->
            val digits = raw.filter(Char::isDigit)
            digits.takeLast(7)
        }.filter { it.length == 7 }.toSet()
        return AllowlistSnapshot(parsed, e164, suffixes, emptyList())
    }

    @Test
    fun matchesFormattedVariantsOfTheSameNumber() {
        val snapshot = snapshotOf("+1 415 555 0132")
        assertTrue(PhoneMatcher.isAllowed("415-555-0132", "US", snapshot))
        assertTrue(PhoneMatcher.isAllowed("+14155550132", "US", snapshot))
        assertTrue(PhoneMatcher.isAllowed("(415) 555-0132", "US", snapshot))
    }

    @Test
    fun rejectsDifferentNumber() {
        val snapshot = snapshotOf("+1 415 555 0132")
        assertFalse(PhoneMatcher.isAllowed("+1 415 555 0199", "US", snapshot))
        // Same last seven digits but a different area code must NOT match.
        assertFalse(PhoneMatcher.isAllowed("4085550132", "US", snapshot))
    }

    @Test
    fun emptyAllowlistNeverMatches() {
        assertFalse(PhoneMatcher.isAllowed("+14155550132", "US", AllowlistSnapshot.EMPTY))
        assertFalse(PhoneMatcher.isAllowed(null, "US", AllowlistSnapshot.EMPTY))
    }

    @Test
    fun fallsBackToDigitSuffixForUnparseableNumbers() {
        val snapshot = snapshotOf("+1 415 555 0132")
        // 19 digits: libphonenumber rejects this as TOO_LONG, so the fallback runs.
        assertTrue(PhoneMatcher.isAllowed("1234567890125550132", "US", snapshot))
    }
}

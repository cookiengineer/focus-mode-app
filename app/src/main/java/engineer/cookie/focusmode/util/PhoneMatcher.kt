package engineer.cookie.focusmode.util

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * A lightweight, immutable snapshot of the allowlist used for fast, synchronous
 * matching inside the call screening service.
 */
data class AllowlistSnapshot(
    val numbers: List<com.google.i18n.phonenumbers.Phonenumber.PhoneNumber>,
    val e164: Set<String>,
    val digitSuffixes: Set<String>,
    val contactIds: List<Long>,
) {
    val isEmpty: Boolean get() = numbers.isEmpty() && digitSuffixes.isEmpty()

    companion object {
        val EMPTY = AllowlistSnapshot(emptyList(), emptySet(), emptySet(), emptyList())
    }
}

/**
 * Matches an incoming call number against the allowlist.
 *
 * For any number libphonenumber can parse, matching is strict: exact E.164
 * equality or libphonenumber EXACT_MATCH (which already accounts for formatting
 * and country-code variants). This avoids false positives between two numbers
 * that merely share their last few digits.
 *
 * Only when the incoming number cannot be parsed at all do we fall back to a
 * conservative last-7-digit comparison.
 */
object PhoneMatcher {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun isAllowed(incomingRaw: String?, region: String, snapshot: AllowlistSnapshot): Boolean {
        if (incomingRaw.isNullOrBlank() || snapshot.isEmpty) return false

        val parsed = try {
            util.parse(incomingRaw, region)
        } catch (_: NumberParseException) {
            null
        }

        if (parsed != null) {
            val e164 = if (util.isValidNumber(parsed)) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
            if (e164 != null && snapshot.e164.contains(e164)) return true

            for (allowed in snapshot.numbers) {
                val match = runCatching { util.isNumberMatch(parsed, allowed) }.getOrNull()
                if (match == PhoneNumberUtil.MatchType.EXACT_MATCH) return true
            }
            return false
        }

        val digits = PhoneNormalizer.digits(incomingRaw)
        if (digits.length >= 7) {
            for (suffix in snapshot.digitSuffixes) {
                if (digits.length >= suffix.length && digits.endsWith(suffix)) return true
            }
        }
        return false
    }
}

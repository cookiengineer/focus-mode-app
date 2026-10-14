package engineer.cookie.focusmode.util

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Offline phone number normalization built on libphonenumber.
 *
 * No network and no Google services are involved; the region is derived from the
 * SIM/network country or the device locale.
 */
object PhoneNormalizer {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun defaultRegion(context: Context): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val network = telephony?.networkCountryIso
        if (!network.isNullOrBlank()) return network.uppercase(Locale.ROOT)
        val sim = telephony?.simCountryIso
        if (!sim.isNullOrBlank()) return sim.uppercase(Locale.ROOT)
        return Locale.getDefault().country.ifBlank { "US" }.uppercase(Locale.ROOT)
    }

    /** Returns an E.164 string when the number is valid for the given region, else null. */
    fun toE164(raw: String, region: String): String? {
        return try {
            val parsed = util.parse(raw, region)
            if (util.isValidNumber(parsed)) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
        } catch (_: NumberParseException) {
            null
        }
    }

    fun toE164(context: Context, raw: String): String? = toE164(raw, defaultRegion(context))

    /** Digits only, used as a last-resort fallback for matching. */
    fun digits(raw: String): String = raw.filter { it.isDigit() }
}

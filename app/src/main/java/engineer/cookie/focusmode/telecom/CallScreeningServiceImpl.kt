package engineer.cookie.focusmode.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import engineer.cookie.focusmode.AppGraph
import engineer.cookie.focusmode.util.PhoneMatcher
import engineer.cookie.focusmode.util.PhoneNormalizer

/**
 * Silences incoming calls that are not on the allowlist while Focus Mode is on.
 *
 * For allowlisted numbers the call proceeds normally. For everything else the
 * call is rejected before it rings. Emergency numbers are always allowed.
 */
class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val builder = CallResponse.Builder()

        if (!AppGraph.focusModeController.isActive.value) {
            respondToCall(callDetails, builder.setDisallowCall(false).build())
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart
        val isEmergency = rawNumber != null && runCatching {
            getSystemService(TelephonyManager::class.java)?.isEmergencyNumber(rawNumber) == true
        }.getOrDefault(false)

        val allowed = isEmergency || PhoneMatcher.isAllowed(
            rawNumber,
            PhoneNormalizer.defaultRegion(this),
            AppGraph.allowlistRepository.snapshot.value,
        )

        if (allowed) {
            builder.setDisallowCall(false)
        } else {
            builder.setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
        }

        respondToCall(callDetails, builder.build())
    }
}

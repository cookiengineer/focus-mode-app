package engineer.cookie.focusmode.data

import android.content.Context
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import engineer.cookie.focusmode.util.AllowlistSnapshot
import engineer.cookie.focusmode.util.PhoneNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the allowlist. Keeps an in-memory [AllowlistSnapshot]
 * so the call screening service can match synchronously without hitting the DB.
 */
class AllowlistRepository(
    private val dao: AllowedContactDao,
    context: Context,
) {

    private val region: String = PhoneNormalizer.defaultRegion(context)

    private val _snapshot = MutableStateFlow(AllowlistSnapshot.EMPTY)
    val snapshot: StateFlow<AllowlistSnapshot> = _snapshot.asStateFlow()

    val contacts: Flow<List<AllowedContact>> = dao.observeAll()

    suspend fun refresh() {
        _snapshot.value = buildSnapshot(dao.getAll())
    }

    suspend fun add(contact: AllowedContact) {
        dao.insert(contact)
        refresh()
    }

    suspend fun removeById(id: Long) {
        dao.deleteById(id)
        refresh()
    }

    suspend fun count(): Int = dao.count()

    private fun buildSnapshot(entries: List<AllowedContact>): AllowlistSnapshot {
        val util = PhoneNumberUtil.getInstance()
        val numbers = ArrayList<com.google.i18n.phonenumbers.Phonenumber.PhoneNumber>()
        val e164 = LinkedHashSet<String>()
        val suffixes = LinkedHashSet<String>()

        for (entry in entries) {
            if (entry.e164.isNotBlank()) e164.add(entry.e164)

            val digits = PhoneNormalizer.digits(entry.rawNumber)
            if (digits.length >= 7) suffixes.add(digits.takeLast(7))

            try {
                numbers.add(util.parse(entry.rawNumber, region))
            } catch (_: NumberParseException) {
                // Fall back to the digit suffix for this entry.
            }
        }

        val contactIds = entries.mapNotNull { it.contactId }.distinct()
        return AllowlistSnapshot(numbers, e164, suffixes, contactIds)
    }
}

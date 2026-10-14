package engineer.cookie.focusmode.dnd

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract

/**
 * Reflects the app allowlist into the platform "starred" (favorite) flag.
 *
 * Do Not Disturb priority mode can only allow calls/messages by *category*
 * (anyone / contacts / starred), so allowlisted contacts are starred to make
 * the OS let exactly those people through.
 *
 * Only contacts that were not already starred by the user are changed, and the
 * IDs of those are returned so they can be un-starred when Focus Mode ends.
 */
class ContactStarManager(private val context: Context) {

    private val resolver get() = context.contentResolver

    fun star(contactIds: List<Long>): List<Long> {
        val newlyStarred = ArrayList<Long>()
        for (id in contactIds.distinct()) {
            if (isStarred(id) != false) continue
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, 1)
            }
            val updated = runCatching {
                resolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(id.toString()),
                )
            }.getOrDefault(0)
            if (updated > 0) newlyStarred.add(id)
        }
        return newlyStarred
    }

    fun unstar(contactIds: Collection<Long>) {
        for (id in contactIds.distinct()) {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, 0)
            }
            runCatching {
                resolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(id.toString()),
                )
            }
        }
    }

    /** Returns true if starred, false if not, null if the contact could not be read. */
    private fun isStarred(contactId: Long): Boolean? {
        return runCatching {
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.STARRED),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) != 0 else null
            }
        }.getOrNull()
    }
}

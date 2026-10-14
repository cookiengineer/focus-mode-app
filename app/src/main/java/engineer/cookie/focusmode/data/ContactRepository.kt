package engineer.cookie.focusmode.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** A single phone number belonging to a contact. */
data class PhoneEntry(
    val contactId: Long,
    val lookupKey: String?,
    val displayName: String,
    val number: String,
    val label: String?,
)

/**
 * Reads contacts and their phone numbers from the platform Contacts provider.
 * Works with the AOSP Contacts app on LineageOS and does not touch the network.
 */
class ContactRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    /**
     * Resolves all phone numbers for the contact behind [contactUri], which is the
     * URI returned by the system contact picker.
     */
    fun phonesForContactUri(contactUri: Uri): List<PhoneEntry> {
        var contactId = -1L
        var displayName = ""
        var lookupKey: String? = null

        resolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.LOOKUP_KEY,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getLong(0)
                displayName = cursor.getString(1) ?: ""
                lookupKey = cursor.getString(2)
            }
        }

        if (contactId <= 0L) return emptyList()
        return phonesForContactId(contactId, displayName, lookupKey)
    }

    fun phonesForContactId(contactId: Long, displayName: String, lookupKey: String?): List<PhoneEntry> {
        val entries = ArrayList<PhoneEntry>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex) ?: continue
                val type = cursor.getInt(typeIndex)
                val customLabel = cursor.getString(labelIndex)
                val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.resources,
                    type,
                    customLabel,
                ).toString()
                entries.add(PhoneEntry(contactId, lookupKey, displayName, number, label))
            }
        }
        return entries
    }
}

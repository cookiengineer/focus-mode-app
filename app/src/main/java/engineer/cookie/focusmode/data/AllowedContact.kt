package engineer.cookie.focusmode.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One allowlisted contact phone number.
 *
 * A contact may appear more than once if it has several allowlisted numbers; the
 * [e164] value is unique so the same number cannot be added twice.
 */
@Entity(
    tableName = "allowed_contacts",
    indices = [Index(value = ["e164"], unique = true)],
)
data class AllowedContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long?,
    val lookupKey: String?,
    val displayName: String,
    val rawNumber: String,
    val e164: String,
)

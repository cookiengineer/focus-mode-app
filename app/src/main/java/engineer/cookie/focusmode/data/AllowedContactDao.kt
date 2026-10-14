package engineer.cookie.focusmode.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedContactDao {

    @Query("SELECT * FROM allowed_contacts ORDER BY displayName COLLATE NOCASE, e164")
    fun observeAll(): Flow<List<AllowedContact>>

    @Query("SELECT * FROM allowed_contacts ORDER BY displayName COLLATE NOCASE, e164")
    suspend fun getAll(): List<AllowedContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: AllowedContact): Long

    @Query("DELETE FROM allowed_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM allowed_contacts")
    suspend fun count(): Int
}

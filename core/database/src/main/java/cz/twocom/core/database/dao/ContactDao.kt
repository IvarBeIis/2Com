package cz.twocom.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cz.twocom.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isBlocked = 0 ORDER BY lastSeenAt DESC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE peerHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("UPDATE contacts SET lastSeenAt = :ts WHERE peerHash = :hash")
    suspend fun updateLastSeen(hash: String, ts: Long)

    @Query("DELETE FROM contacts WHERE peerHash = :hash")
    suspend fun deleteByHash(hash: String)
}

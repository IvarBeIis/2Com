package cz.twocom.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.twocom.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactId = :contactId AND isDeleted = 0 ORDER BY sentAt ASC")
    fun observeByContact(contactId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE contactId = :contactId AND isDeleted = 0 ORDER BY sentAt DESC LIMIT 1")
    suspend fun lastMessage(contactId: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status, deliveredAt = :ts WHERE messageUuid = :uuid")
    suspend fun markDelivered(uuid: String, ts: Long, status: String = "DELIVERED")

    @Query("UPDATE messages SET status = 'READ', readAt = :ts WHERE contactId = :contactId AND isSelf = 0")
    suspend fun markAllRead(contactId: Long, ts: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE contactId = :contactId AND status != 'READ' AND isSelf = 0")
    fun unreadCount(contactId: Long): Flow<Int>
}

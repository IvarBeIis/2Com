package cz.twocom.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageUuid: String,
    val contactId: Long,
    val isSelf: Boolean,
    val text: String?,
    val status: String, // SENDING | SENT | DELIVERED | READ | FAILED
    val sentAt: Long,
    val deliveredAt: Long?,
    val readAt: Long?,
    val expiresAt: Long?,
    val isDeleted: Boolean = false,
)

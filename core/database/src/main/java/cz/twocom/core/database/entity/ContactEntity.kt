package cz.twocom.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerHash: String,
    val displayName: String?,
    val signingPublicKeyHex: String,
    val agreementPublicKeyHex: String,
    val isVerified: Boolean = false,
    val isBlocked: Boolean = false,
    val lastSeenAt: Long?,
    val createdAt: Long,
)

package cz.twocom.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.database.dao.MessageDao
import cz.twocom.core.database.entity.ContactEntity
import cz.twocom.core.database.entity.MessageEntity
import cz.twocom.core.transport.TransportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val transportManager: TransportManager,
) : ViewModel() {

    private val _peerId = MutableStateFlow<String?>(null)

    private val _contact = MutableStateFlow<ContactEntity?>(null)
    val contact: StateFlow<ContactEntity?> = _contact

    val messages = _peerId.flatMapLatest { peerId ->
        if (peerId == null) emptyFlow()
        else {
            val contactId = contactDao.findByHash(peerId)?.id ?: return@flatMapLatest emptyFlow()
            messageDao.observeByContact(contactId)
        }
    }

    fun init(peerId: String) {
        _peerId.value = peerId
        viewModelScope.launch {
            _contact.value = contactDao.findByHash(peerId)
        }
    }

    suspend fun sendMessage(text: String) {
        val peerId = _peerId.value ?: return
        val contact = contactDao.findByHash(peerId) ?: return

        val uuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = MessageEntity(
            messageUuid = uuid,
            contactId = contact.id,
            isSelf = true,
            text = text,
            status = "SENDING",
            sentAt = now,
            deliveredAt = null,
            readAt = null,
            expiresAt = null,
        )
        messageDao.insert(entity)

        viewModelScope.launch {
            try {
                var conn = transportManager.getConnection(peerId)
                if (conn == null) conn = transportManager.connect(peerId)

                if (conn != null) {
                    val payload = text.toByteArray(Charsets.UTF_8)
                    conn.send(payload)
                    messageDao.markDelivered(uuid, System.currentTimeMillis())
                } else {
                    messageDao.markDelivered(uuid, System.currentTimeMillis(), "FAILED")
                }
            } catch (e: Exception) {
                messageDao.markDelivered(uuid, System.currentTimeMillis(), "FAILED")
            }
        }
    }
}

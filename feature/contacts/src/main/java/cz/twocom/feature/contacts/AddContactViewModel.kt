package cz.twocom.feature.contacts

import androidx.lifecycle.ViewModel
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.database.entity.ContactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class AddContactState {
    object Idle : AddContactState()
    object Loading : AddContactState()
}

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val contactDao: ContactDao,
) : ViewModel() {

    private val _state = MutableStateFlow<AddContactState>(AddContactState.Idle)
    val state: StateFlow<AddContactState> = _state

    suspend fun addContact(hash: String, displayName: String?): Boolean {
        if (hash.length != 64) return false
        _state.value = AddContactState.Loading
        return try {
            val existing = contactDao.findByHash(hash)
            if (existing != null) {
                _state.value = AddContactState.Idle
                true
            } else {
                contactDao.insert(
                    ContactEntity(
                        peerHash = hash,
                        displayName = displayName,
                        signingPublicKeyHex = "",
                        agreementPublicKeyHex = "",
                        lastSeenAt = null,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                _state.value = AddContactState.Idle
                true
            }
        } catch (e: Exception) {
            _state.value = AddContactState.Idle
            false
        }
    }
}

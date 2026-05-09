package cz.twocom.feature.chat

import androidx.lifecycle.ViewModel
import cz.twocom.core.database.dao.ContactDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(contactDao: ContactDao) : ViewModel() {
    val contacts = contactDao.observeAll()
}

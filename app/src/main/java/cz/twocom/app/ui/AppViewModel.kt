package cz.twocom.app.ui

import androidx.lifecycle.ViewModel
import cz.twocom.core.crypto.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val identityManager: IdentityManager,
) : ViewModel() {
    val hasIdentity = flow { emit(identityManager.loadIdentity() != null) }
}

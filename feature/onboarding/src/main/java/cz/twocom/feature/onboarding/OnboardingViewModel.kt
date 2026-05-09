package cz.twocom.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.twocom.core.crypto.Identity
import cz.twocom.core.crypto.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingState {
    object Loading : OnboardingState()
    data class Ready(val identity: Identity) : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val identityManager: IdentityManager,
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Loading)
    val state: StateFlow<OnboardingState> = _state

    init {
        generateIdentity()
    }

    private fun generateIdentity() {
        viewModelScope.launch {
            try {
                val identity = identityManager.generateIdentity()
                _state.value = OnboardingState.Ready(identity)
            } catch (e: Exception) {
                _state.value = OnboardingState.Error(e.message ?: "Identity generation failed")
            }
        }
    }
}

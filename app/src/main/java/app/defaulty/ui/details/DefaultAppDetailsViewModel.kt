package app.defaulty.ui.details

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.SupportedRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val role: SupportedRole? = null,
    val defaultApp: DefaultAppInfo? = null,
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the Default App Details screen.
 * Queries the current holder for a specific role
 * and provides intents for changing the default.
 *
 * Re-queries after returning from system UI (Product Rule 11).
 */
class DefaultAppDetailsViewModel(
    application: Application,
    private val role: SupportedRole,
) : AndroidViewModel(application) {

    private val app = application as DefaultyApp
    private val repository = app.defaultAppsRepository

    private val _uiState = MutableStateFlow(DetailsUiState(role = role))
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-query the current default. Called on ON_RESUME. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val info = repository.getDefaultForRole(role)
            _uiState.update {
                DetailsUiState(
                    role = role,
                    defaultApp = info,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Get the intent to change the default for this role.
     * Uses RoleManager.createRequestRoleIntent() for role-backed defaults.
     * Returns null if the intent cannot be created.
     */
    fun getChangeDefaultIntent(): Intent? =
        repository.createChangeDefaultIntent(role)

    /**
     * Fallback intent: open general app settings for the current holder.
     * Used when the primary intent is unavailable (Spec Section 8).
     */
    fun getFallbackSettingsIntent(): Intent? =
        _uiState.value.defaultApp?.holderPackageName?.let {
            repository.createAppSettingsIntent(it)
        }

    /** Factory for creating this ViewModel with a specific role. */
    class Factory(
        private val application: Application,
        private val role: SupportedRole,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DefaultAppDetailsViewModel(application, role) as T
    }
}

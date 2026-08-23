package app.defaulty.ui.details

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.data.preferences.ApplyMode
import app.defaulty.data.system.PrivilegedShellManager
import app.defaulty.domain.model.CandidateAppInfo
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.SupportedRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val role: SupportedRole? = null,
    val defaultApp: DefaultAppInfo? = null,
    val candidateApps: List<CandidateAppInfo> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the Default App Details screen.
 * Queries the current holder for a specific role, discovers
 * all candidate installed applications, and provides intents
 * for changing the default via Android system settings.
 *
 * Supports dual-mode apply via [PrivilegedShellManager]:
 * Root (KernelSU/Magisk/APatch) and Shizuku (ADB Binder IPC).
 *
 * Re-queries after returning from system UI (Product Rule 11).
 */
class DefaultAppDetailsViewModel(
    application: Application,
    private val role: SupportedRole,
) : AndroidViewModel(application) {

    private val app = application as DefaultyApp
    private val repository = app.defaultAppsRepository
    private val userPreferences = app.userPreferences

    private val _uiState = MutableStateFlow(DetailsUiState(role = role))
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-query the current default and candidate apps. Called on ON_RESUME. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.defaultApp == null && _uiState.value.candidateApps.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val info = repository.getDefaultForRole(role)
            val candidates = repository.getCandidateAppsForRole(role)
            _uiState.update {
                DetailsUiState(
                    role = role,
                    defaultApp = info,
                    candidateApps = candidates,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Check if 1-Tap Apply is capable with the current apply mode.
     */
    suspend fun is1TapApplyCapable(): Boolean {
        val mode = userPreferences.applyMode.first()
        return PrivilegedShellManager.is1TapApplyCapable(mode)
    }

    /**
     * Apply default role directly via the privileged shell (Root or Shizuku)
     * based on the current user ApplyMode preference.
     *
     * Refreshes state on success.
     */
    suspend fun applyDefaultViaPrivilegedShell(packageName: String): Boolean {
        val mode = userPreferences.applyMode.first()
        val success = PrivilegedShellManager.applyDefaultRole(role, packageName, mode)
        if (success) {
            refresh()
        }
        return success
    }

    /**
     * Get the intent to change the default for this role via system settings.
     */
    fun getChangeDefaultIntent(): Intent =
        repository.createChangeDefaultIntent(role)

    /**
     * Fallback intent: open general app settings for the current holder.
     * Used when the primary intent is unavailable (Spec Section 8).
     */
    fun getFallbackSettingsIntent(): Intent? =
        _uiState.value.defaultApp?.holderPackageName?.let {
            repository.createAppSettingsIntent(it)
        }

    /**
     * Intent to open App Info for a specific package.
     */
    fun getAppSettingsIntent(packageName: String): Intent =
        repository.createAppSettingsIntent(packageName)

    /**
     * Intent to open Link Handling for a specific package.
     */
    fun getManageLinksIntent(packageName: String): Intent =
        repository.createManageLinksIntent(packageName)

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

package app.defaulty.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.MediaDefaultAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val primaryDefaults: List<DefaultAppInfo> = emptyList(),
    val otherRoleDefaults: List<DefaultAppInfo> = emptyList(),
    val mediaDefaults: List<MediaDefaultAppInfo> = emptyList(),
    val isLoading: Boolean = true,
) {
    val hasOtherContent: Boolean
        get() = otherRoleDefaults.isNotEmpty() || mediaDefaults.isNotEmpty()
}

/**
 * ViewModel for the Home screen.
 * Queries available default apps and media handlers on a background thread,
 * splits into prioritized "Your defaults" (primary) and structured "Others" categories.
 * Refreshes on ON_RESUME via the Screen composable.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DefaultyApp).defaultAppsRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-query all defaults from the system. Called on ON_RESUME. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.primaryDefaults.isEmpty() && !_uiState.value.hasOtherContent) {
                _uiState.update { it.copy(isLoading = true) }
            }

            val roleDefaults = repository.getAvailableDefaults()
            val primary = roleDefaults.filter { it.role.isPrimary }.sortedBy { it.role.sortOrder }
            val otherRoles = roleDefaults.filter { !it.role.isPrimary }.sortedBy { it.role.sortOrder }
            val mediaDefaults = repository.getAvailableMediaDefaults().sortedBy { it.type.sortOrder }

            _uiState.update {
                HomeUiState(
                    primaryDefaults = primary,
                    otherRoleDefaults = otherRoles,
                    mediaDefaults = mediaDefaults,
                    isLoading = false,
                )
            }
        }
    }
}

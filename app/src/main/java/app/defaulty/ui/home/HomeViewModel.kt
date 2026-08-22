package app.defaulty.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.domain.model.DefaultAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val primaryDefaults: List<DefaultAppInfo> = emptyList(),
    val otherDefaults: List<DefaultAppInfo> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the Home screen.
 * Queries available default apps on a background thread,
 * splits into "Your defaults" (primary) and "Other" categories.
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
            _uiState.update { it.copy(isLoading = true) }

            val defaults = repository.getAvailableDefaults()
            val primary = defaults.filter { it.role.isPrimary }
            val other = defaults.filter { !it.role.isPrimary }

            _uiState.update {
                HomeUiState(
                    primaryDefaults = primary,
                    otherDefaults = other,
                    isLoading = false,
                )
            }
        }
    }
}

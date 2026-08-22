package app.defaulty.ui.links

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.domain.model.LinkHandlingAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LinksUiState(
    val apps: List<LinkHandlingAppInfo> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the Opening Links screen.
 * Queries all installed apps with domain verification configured.
 * Re-queries on ON_RESUME after returning from system settings.
 */
class OpeningLinksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DefaultyApp).defaultAppsRepository

    private val _uiState = MutableStateFlow(LinksUiState())
    val uiState: StateFlow<LinksUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-query link handling apps. Called on ON_RESUME. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val apps = repository.getLinkHandlingApps()
            _uiState.update {
                LinksUiState(apps = apps, isLoading = false)
            }
        }
    }

    /** Create an intent to manage link handling for a specific app. */
    fun createManageLinksIntent(packageName: String): Intent =
        repository.createManageLinksIntent(packageName)

    /** Fallback intent for when the link settings action is unavailable. */
    fun createFallbackIntent(packageName: String): Intent =
        repository.createAppSettingsIntent(packageName)
}

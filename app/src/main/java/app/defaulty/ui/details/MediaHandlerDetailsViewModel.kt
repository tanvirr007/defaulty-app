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
import app.defaulty.domain.model.MediaDefaultAppInfo
import app.defaulty.domain.model.MediaHandlerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaDetailsUiState(
    val type: MediaHandlerType? = null,
    val defaultApp: MediaDefaultAppInfo? = null,
    val candidateApps: List<CandidateAppInfo> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the Media Handler Details screen.
 * Queries the current default handler and all compatible installed apps
 * for media/file types (Video, Gallery, Music, PDF, Email).
 */
class MediaHandlerDetailsViewModel(
    application: Application,
    private val type: MediaHandlerType,
) : AndroidViewModel(application) {

    private val app = application as DefaultyApp
    private val repository = app.defaultAppsRepository
    private val userPreferences = app.userPreferences

    private val _uiState = MutableStateFlow(MediaDetailsUiState(type = type))
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Check if 1-Tap Apply is capable with the current apply mode.
     */
    suspend fun is1TapApplyCapable(): Boolean {
        val mode = userPreferences.applyMode.first()
        return PrivilegedShellManager.is1TapApplyCapable(mode)
    }

    /**
     * Clears the preferred activities for an active default package via privileged shell.
     * Disallowed if only 1 app candidate exists.
     */
    suspend fun clearActiveDefault(packageName: String): Boolean {
        if (_uiState.value.candidateApps.size <= 1) {
            return false
        }
        val mode = userPreferences.applyMode.first()
        val success = PrivilegedShellManager.clearPackagePreferredActivities(packageName, mode)
        if (success) {
            refresh()
        }
        return success
    }

    /** Re-query the current default and candidate apps. Called on ON_RESUME. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.defaultApp == null && _uiState.value.candidateApps.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val info = repository.getMediaDefault(type)
            val candidates = repository.getCandidateAppsForMedia(type)
            _uiState.update {
                MediaDetailsUiState(
                    type = type,
                    defaultApp = info,
                    candidateApps = candidates,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Intent to trigger Android's "Open with..." system chooser.
     */
    fun getMediaChooserIntent(promptTitle: String): Intent =
        repository.createMediaChooserIntent(type, promptTitle)

    /**
     * Intent to launch sample media directly.
     */
    fun getMediaSampleIntent(): Intent =
        repository.createMediaSampleIntent(type)

    /**
     * Fallback intent: open general app settings for the current default package.
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
     * Intent to open Link Handling / Open by default for a specific package.
     */
    fun getManageLinksIntent(packageName: String): Intent =
        repository.createManageLinksIntent(packageName)

    /**
     * Intent to open App Info settings for clearing defaults.
     */
    fun getAppOpenByDefaultSettingsIntent(packageName: String): Intent =
        repository.createAppSettingsIntent(packageName)

    /** Factory for creating this ViewModel with a specific media handler type. */
    class Factory(
        private val application: Application,
        private val type: MediaHandlerType,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MediaHandlerDetailsViewModel(application, type) as T
    }
}

package app.defaulty.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Onboarding wizard.
 * Manages theme selection during onboarding and
 * persists the completion flag to DataStore.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as DefaultyApp).userPreferences

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingCompleted(true)
            onComplete()
        }
    }
}

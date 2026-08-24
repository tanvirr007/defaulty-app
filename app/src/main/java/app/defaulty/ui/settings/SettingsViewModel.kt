package app.defaulty.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.defaulty.DefaultyApp
import app.defaulty.data.preferences.ApplyMode
import app.defaulty.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 * Manages theme mode and apply mode preferences via DataStore.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as DefaultyApp).userPreferences

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    val applyMode: StateFlow<ApplyMode> = prefs.applyMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApplyMode.STANDARD,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }
}

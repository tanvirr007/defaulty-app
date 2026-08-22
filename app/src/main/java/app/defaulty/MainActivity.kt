package app.defaulty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.defaulty.data.preferences.ThemeMode
import app.defaulty.navigation.DefaultyNavGraph
import app.defaulty.navigation.Screen
import app.defaulty.theme.DefaultyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Spec Section 13: Native Android splash screen (SplashScreen API, not a custom Activity)
        val splashScreen = installSplashScreen()

        // Spec Section 5: Genuine Android edge-to-edge rendering
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val app = application as DefaultyApp
        val userPreferences = app.userPreferences

        // Check if onboarding completed synchronously before drawing to avoid start-destination flicker
        val isOnboardingCompleted = runBlocking {
            userPreferences.onboardingCompleted.first()
        }

        val startDestination = if (isOnboardingCompleted) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        setContent {
            val themeMode by userPreferences.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )

            DefaultyTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    DefaultyNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }
}

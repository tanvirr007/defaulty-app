package app.defaulty.theme

import androidx.compose.ui.graphics.Color

// Static fallback colors for devices without dynamic color support.
// Since minSdk is 31 (Android 12), dynamic color is always available,
// but these exist as a safety net per Product Rule 14.

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

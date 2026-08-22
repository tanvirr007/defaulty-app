package app.defaulty.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.defaulty.R

val GoogleSansFlexRounded = FontFamily(
    Font(R.font.google_sans_flex_rounded)
)

private val defaultTypography = Typography()

/**
 * Defaulty Material 3 Typography powered by Google Sans Flex Rounded.
 */
val DefaultyTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSansFlexRounded),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSansFlexRounded),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSansFlexRounded),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSansFlexRounded),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSansFlexRounded),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSansFlexRounded),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSansFlexRounded),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSansFlexRounded),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSansFlexRounded),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFlexRounded),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFlexRounded),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFlexRounded),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFlexRounded),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFlexRounded),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFlexRounded),
)

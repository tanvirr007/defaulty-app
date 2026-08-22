package app.defaulty.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed application that is capable of
 * fulfilling a specific system role (e.g., an installed browser).
 */
data class CandidateAppInfo(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val isDefault: Boolean,
)

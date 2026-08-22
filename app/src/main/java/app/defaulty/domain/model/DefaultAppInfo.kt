package app.defaulty.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents a default app for a specific role,
 * including the current holder's info (if any).
 */
data class DefaultAppInfo(
    val role: SupportedRole,
    val holderPackageName: String?,
    val holderAppLabel: String?,
    val holderAppIcon: Drawable?,
    val isAvailable: Boolean,
)

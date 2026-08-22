package app.defaulty.domain.model

import android.graphics.drawable.Drawable

/**
 * Information about the current default/preferred app for a media or file handler.
 */
data class MediaDefaultAppInfo(
    val type: MediaHandlerType,
    val holderPackageName: String?,
    val holderAppLabel: String?,
    val holderAppIcon: Drawable?,
    val candidateCount: Int,
    val isAvailable: Boolean = candidateCount > 0,
)

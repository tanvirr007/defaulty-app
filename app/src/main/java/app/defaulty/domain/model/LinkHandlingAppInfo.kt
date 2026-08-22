package app.defaulty.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed app's link-handling configuration,
 * as reported by DomainVerificationManager.
 */
data class LinkHandlingAppInfo(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val verifiedDomains: List<String>,
    val allDomains: List<String>,
    val isLinkHandlingEnabled: Boolean,
)

package app.defaulty.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single source of truth for all known RoleManager roles (Spec Section 16a).
 *
 * To add support for a new Android role:
 *   1. Add one entry to this enum with the role string, label, and icon.
 *   2. No UI code changes needed (Product Rule 13).
 *
 * Each role is probed at runtime via RoleManager.isRoleAvailable() —
 * never assumed present based on SDK version alone (Product Rule 14).
 *
 * Role name strings are used directly instead of RoleManager constants
 * for maximum future-proofing — new roles can be added before the
 * compile SDK exposes a constant for them.
 */
enum class SupportedRole(
    val roleName: String,
    val displayLabel: String,
    val icon: ImageVector,
    val isPrimary: Boolean,
    val description: String,
) {
    BROWSER(
        roleName = "android.app.role.BROWSER",
        displayLabel = "Browser",
        icon = Icons.Default.Language,
        isPrimary = true,
        description = "web browsing",
    ),
    PHONE(
        roleName = "android.app.role.DIALER",
        displayLabel = "Phone",
        icon = Icons.Default.Phone,
        isPrimary = true,
        description = "phone calls",
    ),
    SMS(
        roleName = "android.app.role.SMS",
        displayLabel = "SMS",
        icon = Icons.Default.Sms,
        isPrimary = true,
        description = "messaging",
    ),
    HOME(
        roleName = "android.app.role.HOME",
        displayLabel = "Home / Launcher",
        icon = Icons.Default.Home,
        isPrimary = true,
        description = "your home screen",
    ),
    ASSISTANT(
        roleName = "android.app.role.ASSISTANT",
        displayLabel = "Assistant",
        icon = Icons.Default.Assistant,
        isPrimary = true,
        description = "digital assistant",
    ),
    CALL_SCREENING(
        roleName = "android.app.role.CALL_SCREENING",
        displayLabel = "Call Screening",
        icon = Icons.Default.PhoneCallback,
        isPrimary = false,
        description = "call screening and caller ID",
    ),
    CALL_REDIRECTION(
        roleName = "android.app.role.CALL_REDIRECTION",
        displayLabel = "Call Redirection",
        icon = Icons.Default.PhoneForwarded,
        isPrimary = false,
        description = "outgoing call redirection",
    ),
    // Future roles: add one entry here per new RoleManager role.
    // The capability layer probes isRoleAvailable() at runtime,
    // and the UI automatically discovers and displays available roles.
    ;

    companion object {
        /** Look up a role by its Android role name string. */
        fun fromRoleName(roleName: String): SupportedRole? =
            entries.find { it.roleName == roleName }

        /** Look up a role by its enum name (used for navigation args). */
        fun fromId(id: String): SupportedRole? =
            entries.find { it.name == id }
    }
}

package app.defaulty.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
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
    val sortOrder: Int,
) {
    BROWSER(
        roleName = "android.app.role.BROWSER",
        displayLabel = "Browser",
        icon = Icons.Default.Language,
        isPrimary = true,
        description = "web browsing",
        sortOrder = 1,
    ),
    PHONE(
        roleName = "android.app.role.DIALER",
        displayLabel = "Phone",
        icon = Icons.Default.Phone,
        isPrimary = true,
        description = "phone calls",
        sortOrder = 2,
    ),
    SMS(
        roleName = "android.app.role.SMS",
        displayLabel = "SMS",
        icon = Icons.Default.Sms,
        isPrimary = true,
        description = "messaging",
        sortOrder = 3,
    ),
    HOME(
        roleName = "android.app.role.HOME",
        displayLabel = "Launcher",
        icon = Icons.Default.Home,
        isPrimary = true,
        description = "your launcher",
        sortOrder = 4,
    ),
    ASSISTANT(
        roleName = "android.app.role.ASSISTANT",
        displayLabel = "Assistant",
        icon = Icons.Default.Assistant,
        isPrimary = true,
        description = "digital assistant",
        sortOrder = 5,
    ),
    NOTES(
        roleName = "android.app.role.NOTES",
        displayLabel = "Notes",
        icon = Icons.Default.EditNote,
        isPrimary = false,
        description = "taking notes",
        sortOrder = 6,
    ),
    WALLET(
        roleName = "android.app.role.WALLET",
        displayLabel = "Wallet",
        icon = Icons.Default.AccountBalanceWallet,
        isPrimary = false,
        description = "wallet and contactless payments",
        sortOrder = 7,
    ),
    CALL_SCREENING(
        roleName = "android.app.role.CALL_SCREENING",
        displayLabel = "Call Screening",
        icon = Icons.AutoMirrored.Filled.PhoneCallback,
        isPrimary = false,
        description = "call screening and caller ID",
        sortOrder = 8,
    ),
    CALL_REDIRECTION(
        roleName = "android.app.role.CALL_REDIRECTION",
        displayLabel = "Call Redirection",
        icon = Icons.AutoMirrored.Filled.PhoneForwarded,
        isPrimary = false,
        description = "outgoing call redirection",
        sortOrder = 9,
    ),
    EMERGENCY(
        roleName = "android.app.role.EMERGENCY",
        displayLabel = "Emergency",
        icon = Icons.Default.Emergency,
        isPrimary = false,
        description = "emergency assistance",
        sortOrder = 10,
    );

    /**
     * Generate standard ADB shell command to assign this role to a given package name.
     * Uses `--user 0` flag for maximum OEM compatibility.
     */
    fun getAdbCommand(packageName: String): String {
        return if (this == HOME) {
            "cmd role add-role-holder --user 0 android.app.role.HOME $packageName"
        } else {
            "cmd role add-role-holder --user 0 $roleName $packageName"
        }
    }

    /**
     * Whether this role supports having its default cleared to None.
     * Launcher (HOME) must never be cleared to None as an active launcher is required by Android.
     */
    val canClearHolder: Boolean
        get() = this != HOME

    companion object {
        /** Look up a role by its Android role name string. */
        fun fromRoleName(roleName: String): SupportedRole? =
            entries.find { it.roleName == roleName }

        /** Look up a role by its enum name (used for navigation args). */
        fun fromId(id: String): SupportedRole? =
            entries.find { it.name == id }
    }
}

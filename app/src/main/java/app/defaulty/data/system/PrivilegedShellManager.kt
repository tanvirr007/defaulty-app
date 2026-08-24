package app.defaulty.data.system

import android.util.Log
import app.defaulty.data.preferences.ApplyMode
import app.defaulty.domain.model.SupportedRole

/**
 * Unified coordinator for privileged shell execution across Root and Shizuku backends.
 *
 * Determines the best execution strategy based on the user's selected [ApplyMode]
 * and runtime availability of Root (KernelSU/Magisk/APatch) or Shizuku (ADB Binder IPC).
 *
 * All execution is 100% offline and local on-device.
 */
object PrivilegedShellManager {
    private const val TAG = "PrivilegedShellManager"

    /**
     * Checks if 1-Tap Apply is capable for the given mode.
     *
     * For [ApplyMode.AUTO], checks Root first, then Shizuku.
     */
    suspend fun is1TapApplyCapable(mode: ApplyMode): Boolean {
        return when (mode) {
            ApplyMode.ROOT -> RootShellManager.isRootAvailable()
            ApplyMode.SHIZUKU -> ShizukuManager.hasShizukuPermission()
            ApplyMode.AUTO -> {
                RootShellManager.isRootAvailable() || ShizukuManager.hasShizukuPermission()
            }
            ApplyMode.STANDARD -> false
        }
    }

    /**
     * Applies a default role using the selected execution backend.
     *
     * For [ApplyMode.AUTO], tries Root first, then Shizuku.
     *
     * @return true if the role was successfully applied, false otherwise.
     */
    suspend fun applyDefaultRole(
        role: SupportedRole,
        packageName: String,
        mode: ApplyMode,
    ): Boolean {
        return when (mode) {
            ApplyMode.ROOT -> applyViaRoot(role, packageName)
            ApplyMode.SHIZUKU -> applyViaShizuku(role, packageName)
            ApplyMode.AUTO -> {
                // Try Root first (fastest, most reliable)
                if (RootShellManager.isRootAvailable()) {
                    val success = applyViaRoot(role, packageName)
                    if (success) return true
                }
                // Fallback to Shizuku
                if (ShizukuManager.hasShizukuPermission()) {
                    return applyViaShizuku(role, packageName)
                }
                Log.w(TAG, "AUTO mode: neither Root nor Shizuku available")
                false
            }
            ApplyMode.STANDARD -> false
        }
    }

    /**
     * Clears all preferred activities for a package (e.g. Media/File "Always" defaults).
     */
    suspend fun clearPackagePreferredActivities(
        packageName: String,
        mode: ApplyMode,
    ): Boolean {
        return when (mode) {
            ApplyMode.ROOT -> RootShellManager.clearPackagePreferredActivities(packageName)
            ApplyMode.SHIZUKU -> ShizukuManager.clearPackagePreferredActivities(packageName)
            ApplyMode.AUTO -> {
                if (RootShellManager.isRootAvailable()) {
                    val success = RootShellManager.clearPackagePreferredActivities(packageName)
                    if (success) return true
                }
                if (ShizukuManager.hasShizukuPermission()) {
                    return ShizukuManager.clearPackagePreferredActivities(packageName)
                }
                false
            }
            ApplyMode.STANDARD -> false
        }
    }

    /**
     * Removes a role holder from a system role.
     */
    suspend fun removeRoleHolder(
        role: SupportedRole,
        packageName: String,
        mode: ApplyMode,
    ): Boolean {
        return when (mode) {
            ApplyMode.ROOT -> RootShellManager.removeRoleHolder(role.roleName, packageName)
            ApplyMode.SHIZUKU -> ShizukuManager.removeRoleHolder(role.roleName, packageName)
            ApplyMode.AUTO -> {
                if (RootShellManager.isRootAvailable()) {
                    val success = RootShellManager.removeRoleHolder(role.roleName, packageName)
                    if (success) return true
                }
                if (ShizukuManager.hasShizukuPermission()) {
                    return ShizukuManager.removeRoleHolder(role.roleName, packageName)
                }
                false
            }
            ApplyMode.STANDARD -> false
        }
    }

    /**
     * Clears all role holders for a system role.
     */
    suspend fun clearRoleHolders(
        role: SupportedRole,
        mode: ApplyMode,
    ): Boolean {
        return when (mode) {
            ApplyMode.ROOT -> RootShellManager.clearRoleHolders(role.roleName)
            ApplyMode.SHIZUKU -> ShizukuManager.clearRoleHolders(role.roleName)
            ApplyMode.AUTO -> {
                if (RootShellManager.isRootAvailable()) {
                    val success = RootShellManager.clearRoleHolders(role.roleName)
                    if (success) return true
                }
                if (ShizukuManager.hasShizukuPermission()) {
                    return ShizukuManager.clearRoleHolders(role.roleName)
                }
                false
            }
            ApplyMode.STANDARD -> false
        }
    }

    /**
     * Returns a human-readable description of the active privilege backend.
     */
    suspend fun getActiveBackendLabel(mode: ApplyMode): String {
        return when (mode) {
            ApplyMode.ROOT -> "Root (KernelSU / Magisk)"
            ApplyMode.SHIZUKU -> "ADB / Shizuku"
            ApplyMode.AUTO -> {
                when {
                    RootShellManager.isRootAvailable() -> "Root (Auto-detected)"
                    ShizukuManager.hasShizukuPermission() -> "Shizuku (Auto-detected)"
                    else -> "Standard (No privileges)"
                }
            }
            ApplyMode.STANDARD -> "Standard (Settings)"
        }
    }

    /**
     * Gets status information for Root availability.
     */
    suspend fun getRootStatus(): PrivilegeStatus {
        val available = RootShellManager.isRootAvailable()
        return PrivilegeStatus(
            isAvailable = available,
            label = if (available) "Root access granted" else "Root not available",
        )
    }

    /**
     * Gets status information for Shizuku availability.
     */
    fun getShizukuStatus(): PrivilegeStatus {
        val binderAlive = ShizukuManager.isShizukuAvailable()
        val hasPermission = ShizukuManager.hasShizukuPermission()
        return PrivilegeStatus(
            isAvailable = hasPermission,
            isPartiallyAvailable = binderAlive && !hasPermission,
            label = when {
                hasPermission -> "Shizuku authorized"
                binderAlive -> "Shizuku running (needs authorization)"
                else -> "Shizuku not running"
            },
        )
    }

    private suspend fun applyViaRoot(role: SupportedRole, packageName: String): Boolean {
        return if (role == SupportedRole.HOME) {
            RootShellManager.applyHomeLauncher(packageName)
        } else {
            RootShellManager.applyDefaultRole(role.roleName, packageName)
        }
    }

    private suspend fun applyViaShizuku(role: SupportedRole, packageName: String): Boolean {
        return if (role == SupportedRole.HOME) {
            ShizukuManager.applyHomeLauncher(packageName)
        } else {
            ShizukuManager.applyDefaultRole(role.roleName, packageName)
        }
    }
}

/**
 * Simple data class representing the status of a privilege backend.
 */
data class PrivilegeStatus(
    val isAvailable: Boolean,
    val isPartiallyAvailable: Boolean = false,
    val label: String,
)

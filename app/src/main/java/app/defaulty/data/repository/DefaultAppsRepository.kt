package app.defaulty.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import app.defaulty.data.system.DomainVerificationWrapper
import app.defaulty.data.system.RoleManagerWrapper
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.LinkHandlingAppInfo
import app.defaulty.domain.model.SupportedRole

/**
 * Repository that combines role management and domain verification
 * into a single API surface for the UI layer.
 *
 * All queries are capability-driven: only roles where
 * isRoleAvailable() == true are returned (Spec Section 16).
 */
class DefaultAppsRepository(
    private val context: Context,
    private val roleManagerWrapper: RoleManagerWrapper,
    private val domainVerificationWrapper: DomainVerificationWrapper,
) {

    /**
     * Get all default app roles available on this device.
     * Iterates the single source of truth [SupportedRole] and
     * filters by runtime availability.
     */
    fun getAvailableDefaults(): List<DefaultAppInfo> =
        SupportedRole.entries.mapNotNull { role ->
            if (!roleManagerWrapper.isRoleAvailable(role.roleName)) return@mapNotNull null

            val holder = roleManagerWrapper.getRoleHolder(role.roleName)

            DefaultAppInfo(
                role = role,
                holderPackageName = holder,
                holderAppLabel = holder?.let { roleManagerWrapper.getAppLabel(it) },
                holderAppIcon = holder?.let { roleManagerWrapper.getAppIcon(it) },
                isAvailable = true,
            )
        }

    /**
     * Get the current default app info for a specific role.
     * Returns null if the role is not available on this device.
     */
    fun getDefaultForRole(role: SupportedRole): DefaultAppInfo? {
        if (!roleManagerWrapper.isRoleAvailable(role.roleName)) return null

        val holder = roleManagerWrapper.getRoleHolder(role.roleName)

        return DefaultAppInfo(
            role = role,
            holderPackageName = holder,
            holderAppLabel = holder?.let { roleManagerWrapper.getAppLabel(it) },
            holderAppIcon = holder?.let { roleManagerWrapper.getAppIcon(it) },
            isAvailable = true,
        )
    }

    /**
     * Create an intent to change the default for a role-backed category.
     * Uses RoleManager.createRequestRoleIntent() → system UI (Spec Section 8).
     */
    fun createChangeDefaultIntent(role: SupportedRole): Intent? =
        roleManagerWrapper.createRequestRoleIntent(role.roleName)

    /**
     * Create an intent to manage link handling for a specific package.
     * Uses Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS (Spec Section 10).
     */
    fun createManageLinksIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            Uri.parse("package:$packageName")
        )

    /**
     * Fallback intent to open general app settings for a package
     * when the specific settings action is unavailable (Spec Section 8).
     */
    fun createAppSettingsIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )

    /**
     * Get all apps with link handling configured via DomainVerificationManager.
     */
    fun getLinkHandlingApps(): List<LinkHandlingAppInfo> =
        domainVerificationWrapper.getAppsWithLinkHandling()
}

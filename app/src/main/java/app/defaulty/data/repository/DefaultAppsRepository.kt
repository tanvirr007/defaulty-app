package app.defaulty.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import app.defaulty.data.system.DomainVerificationWrapper
import app.defaulty.data.system.RoleManagerWrapper
import app.defaulty.domain.model.CandidateAppInfo
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.LinkHandlingAppInfo
import app.defaulty.domain.model.MediaDefaultAppInfo
import app.defaulty.domain.model.MediaHandlerType
import app.defaulty.domain.model.SupportedRole

/**
 * Repository that combines role management, media handlers, and domain verification
 * into a single API surface for the UI layer.
 */
class DefaultAppsRepository(
    private val context: Context,
    private val roleManagerWrapper: RoleManagerWrapper,
    private val domainVerificationWrapper: DomainVerificationWrapper,
) {

    /**
     * Get all default app roles available on this device.
     * Iterates the single source of truth [SupportedRole],
     * filters by runtime availability, and sorts by [SupportedRole.sortOrder].
     */
    fun getAvailableDefaults(): List<DefaultAppInfo> =
        SupportedRole.entries
            .filter { roleManagerWrapper.isRoleAvailable(it.roleName) }
            .sortedBy { it.sortOrder }
            .map { role ->
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
     * Get all media and file handler defaults available on this device.
     * Only returns categories that have at least 1 candidate app installed,
     * sorted by [MediaHandlerType.sortOrder].
     */
    fun getAvailableMediaDefaults(): List<MediaDefaultAppInfo> =
        MediaHandlerType.entries
            .map { type ->
                val candidates = roleManagerWrapper.getCandidatePackagesForMedia(type)
                val defaultHolder = roleManagerWrapper.resolveMediaDefault(type)
                MediaDefaultAppInfo(
                    type = type,
                    holderPackageName = defaultHolder,
                    holderAppLabel = defaultHolder?.let { roleManagerWrapper.getAppLabel(it) },
                    holderAppIcon = defaultHolder?.let { roleManagerWrapper.getAppIcon(it) },
                    candidateCount = candidates.size,
                    isAvailable = candidates.isNotEmpty(),
                )
            }
            .filter { it.isAvailable }
            .sortedBy { it.type.sortOrder }

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
     * Get the current default app info for a media/file handler type.
     */
    fun getMediaDefault(type: MediaHandlerType): MediaDefaultAppInfo {
        val candidates = roleManagerWrapper.getCandidatePackagesForMedia(type)
        val defaultHolder = roleManagerWrapper.resolveMediaDefault(type)

        return MediaDefaultAppInfo(
            type = type,
            holderPackageName = defaultHolder,
            holderAppLabel = defaultHolder?.let { roleManagerWrapper.getAppLabel(it) },
            holderAppIcon = defaultHolder?.let { roleManagerWrapper.getAppIcon(it) },
            candidateCount = candidates.size,
            isAvailable = candidates.isNotEmpty(),
        )
    }

    /**
     * Get all installed candidate apps capable of fulfilling a role.
     * Sorted with active default at the top, then alphabetically.
     */
    fun getCandidateAppsForRole(role: SupportedRole): List<CandidateAppInfo> {
        val currentHolder = roleManagerWrapper.getRoleHolder(role.roleName)?.takeIf {
            !roleManagerWrapper.isExcludedPackage(it)
        }
        val packageNames = roleManagerWrapper.getCandidatePackages(role.roleName).toMutableSet()
        currentHolder?.let { packageNames.add(it) }

        return packageNames
            .filter { !roleManagerWrapper.isExcludedPackage(it) }
            .mapNotNull { pkg ->
                val label = roleManagerWrapper.getAppLabel(pkg) ?: return@mapNotNull null
                val icon = roleManagerWrapper.getAppIcon(pkg)
                CandidateAppInfo(
                    packageName = pkg,
                    appLabel = label,
                    appIcon = icon,
                    isDefault = pkg == currentHolder,
                )
            }.sortedWith(
                compareByDescending<CandidateAppInfo> { it.isDefault }
                    .thenBy { it.appLabel.lowercase() }
            )
    }

    /**
     * Get all installed candidate apps capable of handling a media/file type.
     * Sorted with active default at the top, then alphabetically.
     */
    fun getCandidateAppsForMedia(type: MediaHandlerType): List<CandidateAppInfo> {
        val currentHolder = roleManagerWrapper.resolveMediaDefault(type)?.takeIf {
            !roleManagerWrapper.isExcludedPackage(it)
        }
        val packageNames = roleManagerWrapper.getCandidatePackagesForMedia(type).toMutableSet()
        currentHolder?.let { packageNames.add(it) }

        return packageNames
            .filter { !roleManagerWrapper.isExcludedPackage(it) }
            .mapNotNull { pkg ->
                val label = roleManagerWrapper.getAppLabel(pkg) ?: return@mapNotNull null
                val icon = roleManagerWrapper.getAppIcon(pkg)
                CandidateAppInfo(
                    packageName = pkg,
                    appLabel = label,
                    appIcon = icon,
                    isDefault = pkg == currentHolder,
                )
            }.sortedWith(
                compareByDescending<CandidateAppInfo> { it.isDefault }
                    .thenBy { it.appLabel.lowercase() }
            )
    }

    /**
     * Create an intent to change the default for a role-backed category.
     * Dispatches to Android's system default apps UI.
     */
    fun createChangeDefaultIntent(role: SupportedRole): Intent =
        roleManagerWrapper.createSystemDefaultAppsIntent(role.roleName)

    /**
     * Create an intent to test/choose the default for a media handler category.
     */
    fun createMediaChooserIntent(type: MediaHandlerType, promptTitle: String): Intent =
        roleManagerWrapper.createMediaChooserIntent(type, promptTitle)

    /**
     * Create sample media intent for direct launch.
     */
    fun createMediaSampleIntent(type: MediaHandlerType): Intent =
        roleManagerWrapper.createMediaSampleIntent(type)

    /**
     * Create an intent to manage link handling for a specific package.
     * Uses Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS.
     */
    fun createManageLinksIntent(packageName: String): Intent =
        Intent(
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
            Uri.parse("package:$packageName")
        )

    /**
     * Fallback intent to open general app settings for a package
     * when the specific settings action is unavailable.
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

package app.defaulty.data.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import app.defaulty.domain.model.MediaHandlerType

/**
 * Safe wrapper around RoleManager and Intent Resolver.
 *
 * All calls are guarded against OEM deviations and API-level differences.
 * Every system API call is treated as fallible at runtime (Product Rule 14).
 */
class RoleManagerWrapper(private val context: Context) {

    private val tag = "RoleManagerWrapper"

    private val roleManager: RoleManager? = try {
        context.getSystemService(RoleManager::class.java)
    } catch (e: Exception) {
        Log.w(tag, "Failed to get RoleManager service", e)
        null
    }

    /**
     * Check if a role is available on this device.
     * Returns false if unavailable or if the check itself fails.
     */
    fun isRoleAvailable(roleName: String): Boolean = try {
        roleManager?.isRoleAvailable(roleName) ?: false
    } catch (e: Exception) {
        Log.w(tag, "isRoleAvailable failed for $roleName", e)
        false
    }

    /**
     * Check if this app currently holds a role.
     * Returns null if the check fails.
     */
    fun isRoleHeld(roleName: String): Boolean? = try {
        roleManager?.isRoleHeld(roleName)
    } catch (e: Exception) {
        Log.w(tag, "isRoleHeld failed for $roleName", e)
        null
    }

    /**
     * Determine if a package should be excluded from candidate lists and default holders.
     * Excludes system settings, setup wizards, framework stubs, resolvers, and this app itself.
     */
    fun isExcludedPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        val lower = packageName.lowercase()

        // 1. Android framework & resolver stubs
        if (lower == "android" || lower.contains("resolver")) return true

        // 2. Defaulty app itself (do not list self as candidate default)
        if (packageName == context.packageName) return true

        // 3. Setup Wizards & OOBE Provisioning
        if (lower.contains("setupwizard") ||
            lower.contains("oobeprompt") ||
            lower.contains("provision") ||
            lower == "com.google.android.setupwizard" ||
            lower == "com.android.setupwizard" ||
            lower == "com.sec.android.app.secsetupwizard"
        ) return true

        // 4. System Settings packages (AOSP, Google, Samsung, Xiaomi/MIUI, ColorOS, OxygenOS, Transsion, Moto, Vivo, etc.)
        if (lower == "com.android.settings" ||
            lower == "com.google.android.settings" ||
            lower.endsWith(".settings") ||
            lower.contains(".settings.") ||
            lower.startsWith("com.android.settings.") ||
            lower.startsWith("com.google.android.settings.") ||
            lower == "com.xiaomi.misettings" ||
            lower == "com.miui.securitycenter" ||
            lower == "com.meizu.safe" ||
            lower == "com.huawei.systemmanager"
        ) return true

        // 5. System internal utilities / HTML viewers / Captive portal / Traceur
        if (lower == "com.android.htmlviewer" ||
            lower == "com.google.android.htmlviewer" ||
            lower == "com.android.captiveportallogin" ||
            lower == "com.google.android.captiveportallogin" ||
            lower == "com.android.traceur"
        ) return true

        return false
    }

    /**
     * Get the package name of the current holder of a role.
     * Returns null if no holder, role unavailable, or the check fails.
     */
    fun getRoleHolder(roleName: String): String? = try {
        val holderFromRoleManager = try {
            if (roleManager != null) {
                val method = roleManager.javaClass.getMethod("getRoleHolders", String::class.java)
                @Suppress("UNCHECKED_CAST")
                val holders = method.invoke(roleManager, roleName) as? List<*>
                val firstPkg = holders?.firstOrNull()?.toString()
                firstPkg?.takeIf { !isExcludedPackage(it) }
            } else null
        } catch (_: Throwable) {
            null
        }
        val holder = holderFromRoleManager ?: resolveDefaultViaIntent(roleName)
        holder?.takeIf { !isExcludedPackage(it) }
    } catch (e: Exception) {
        resolveDefaultViaIntent(roleName)?.takeIf { !isExcludedPackage(it) }
    }

    private fun resolveDefaultViaIntent(roleName: String): String? = try {
        when (roleName) {
            "android.app.role.BROWSER", RoleManager.ROLE_BROWSER -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                resolveInfo?.activityInfo?.packageName?.takeIf {
                    (resolveInfo.priority >= 0) && !isExcludedPackage(it)
                }
            }
            "android.app.role.DIALER", RoleManager.ROLE_DIALER -> {
                val telecomManager = context.getSystemService(TelecomManager::class.java)
                telecomManager?.defaultDialerPackage?.takeIf { !isExcludedPackage(it) }
            }
            "android.app.role.SMS", RoleManager.ROLE_SMS -> {
                Telephony.Sms.getDefaultSmsPackage(context)?.takeIf { !isExcludedPackage(it) }
            }
            "android.app.role.HOME", RoleManager.ROLE_HOME -> {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val act = resolveInfo?.activityInfo
                val activityName = act?.name?.lowercase().orEmpty()
                val isFallback = (resolveInfo?.priority ?: 0) < 0 ||
                    activityName.contains("fallback") ||
                    activityName.contains("setupwizard") ||
                    activityName.contains("provision")
                if (!isFallback && act?.packageName != null && !isExcludedPackage(act.packageName)) {
                    act.packageName
                } else {
                    null
                }
            }
            "android.app.role.ASSISTANT", RoleManager.ROLE_ASSISTANT -> {
                Settings.Secure.getString(context.contentResolver, "assistant")?.let { setting ->
                    setting.split("/").firstOrNull()?.takeIf { !isExcludedPackage(it) }
                }
            }
            "android.app.role.NOTES" -> {
                val intent = Intent("android.intent.action.CREATE_NOTE")
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                resolveInfo?.activityInfo?.packageName?.takeIf {
                    (resolveInfo.priority >= 0) && !isExcludedPackage(it)
                }
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.w(tag, "resolveDefaultViaIntent failed for $roleName", e)
        null
    }

    /**
     * Query all installed packages capable of fulfilling a role.
     */
    fun getCandidatePackages(roleName: String): List<String> = try {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()
        when (roleName) {
            "android.app.role.BROWSER", RoleManager.ROLE_BROWSER -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.DIALER", RoleManager.ROLE_DIALER -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:123456789"))
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.SMS", RoleManager.ROLE_SMS -> {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:123456789"))
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.HOME", RoleManager.ROLE_HOME -> {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val act = resolveInfo.activityInfo
                    if (act != null) {
                        val pkg = act.packageName
                        val activityName = act.name.lowercase()
                        val isFallback = resolveInfo.priority < 0 ||
                            activityName.contains("fallback") ||
                            activityName.contains("setupwizard") ||
                            activityName.contains("provision")
                        if (!isFallback && !isExcludedPackage(pkg)) {
                            packages.add(pkg)
                        }
                    }
                }
            }
            "android.app.role.ASSISTANT", RoleManager.ROLE_ASSISTANT -> {
                val intent = Intent(Intent.ACTION_ASSIST)
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.CALL_SCREENING", RoleManager.ROLE_CALL_SCREENING -> {
                val intent = Intent("android.telecom.CallScreeningService")
                val list = pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.serviceInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg)) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.CALL_REDIRECTION", RoleManager.ROLE_CALL_REDIRECTION -> {
                val intent = Intent("android.telecom.CallRedirectionService")
                val list = pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.serviceInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg)) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.NOTES" -> {
                val intent = Intent("android.intent.action.CREATE_NOTE")
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.WALLET" -> {
                val intent = Intent("android.service.quickaccesswallet.QuickAccessWalletService")
                val list = pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.serviceInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg)) {
                        packages.add(pkg)
                    }
                }
            }
            "android.app.role.EMERGENCY" -> {
                val intent = Intent("android.telephony.action.EMERGENCY_ASSISTANCE")
                val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                list.forEach { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName
                    if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                        packages.add(pkg)
                    }
                }
            }
        }
        packages.filter { !isExcludedPackage(it) }
    } catch (e: Exception) {
        Log.w(tag, "getCandidatePackages failed for $roleName", e)
        emptyList()
    }

    /**
     * Create an intent to request a role change via Android's system UI.
     * Falls back to system Default Apps settings or specific category settings.
     */
    fun createSystemDefaultAppsIntent(roleName: String): Intent {
        val pm = context.packageManager
        val specificIntent = when (roleName) {
            "android.app.role.HOME", RoleManager.ROLE_HOME -> Intent(Settings.ACTION_HOME_SETTINGS)
            "android.app.role.ASSISTANT", RoleManager.ROLE_ASSISTANT -> Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            else -> Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
        if (pm.resolveActivity(specificIntent, 0) != null) {
            return specificIntent
        }
        val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        if (pm.resolveActivity(defaultAppsIntent, 0) != null) {
            return defaultAppsIntent
        }
        return Intent(Settings.ACTION_SETTINGS)
    }

    /**
     * Create an intent to request a role change via Android's system UI.
     * Returns null if the intent cannot be created.
     */
    fun createRequestRoleIntent(roleName: String): Intent? = try {
        roleManager?.createRequestRoleIntent(roleName)
    } catch (e: Exception) {
        Log.w(tag, "createRequestRoleIntent failed for $roleName", e)
        null
    }

    /**
     * Create a prototype intent representing a media/file handler action.
     */
    fun createMediaSampleIntent(type: MediaHandlerType): Intent = when (type) {
        MediaHandlerType.VIDEO_PLAYER -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://media/external/video/media"), "video/*")
        }
        MediaHandlerType.GALLERY -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://media/external/images/media"), "image/*")
        }
        MediaHandlerType.MUSIC_PLAYER -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://media/external/audio/media"), "audio/*")
        }
        MediaHandlerType.PDF_VIEWER -> Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://dummy/document.pdf"), "application/pdf")
        }
        MediaHandlerType.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    }

    /**
     * Query all installed packages capable of fulfilling a media/file handler category.
     */
    fun getCandidatePackagesForMedia(type: MediaHandlerType): List<String> = try {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()

        // 1. Primary sample intent
        val primaryIntent = createMediaSampleIntent(type)
        pm.queryIntentActivities(primaryIntent, PackageManager.MATCH_ALL).forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo?.packageName
            if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                packages.add(pkg)
            }
        }

        // 2. Fallback query with plain MIME type
        if (type.mimeType != null) {
            val mimeIntent = Intent(Intent.ACTION_VIEW).apply {
                setType(type.mimeType)
            }
            pm.queryIntentActivities(mimeIntent, PackageManager.MATCH_ALL).forEach { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName
                if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
                    packages.add(pkg)
                }
            }
        }

        packages.filter { !isExcludedPackage(it) }
    } catch (e: Exception) {
        Log.w(tag, "getCandidatePackagesForMedia failed for $type", e)
        emptyList()
    }

    /**
     * Find the currently preferred default app for a media/file handler.
     */
    fun resolveMediaDefault(type: MediaHandlerType): String? = try {
        val pm = context.packageManager
        val intent = createMediaSampleIntent(type)
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = resolveInfo?.activityInfo?.packageName
        if (pkg != null && !isExcludedPackage(pkg) && resolveInfo.priority >= 0) {
            pkg
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w(tag, "resolveMediaDefault failed for $type", e)
        null
    }

    /**
     * Create an Intent allowing user to choose an app and set default.
     * Uses direct intent instead of Intent.createChooser to allow Android's
     * native resolver to offer the "Always" / "Just once" selection.
     */
    fun createMediaChooserIntent(type: MediaHandlerType, title: String): Intent {
        return createMediaSampleIntent(type)
    }

    /**
     * Get the user-visible label for an installed package.
     * Returns null if the package is not found or disabled.
     */
    fun getAppLabel(packageName: String): String? = try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null // App uninstalled or disabled — expected case
    } catch (e: Exception) {
        Log.w(tag, "getAppLabel failed for $packageName", e)
        null
    }

    /**
     * Get the icon drawable for an installed package.
     * Returns null if the package is not found.
     */
    fun getAppIcon(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: Exception) {
        Log.w(tag, "getAppIcon failed for $packageName", e)
        null
    }
}

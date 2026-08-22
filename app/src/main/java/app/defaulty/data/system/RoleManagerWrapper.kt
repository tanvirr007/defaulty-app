package app.defaulty.data.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

/**
 * Safe wrapper around RoleManager.
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
     * Get the package name of the current holder of a role.
     * Returns null if no holder, role unavailable, or the check fails.
     */
    fun getRoleHolder(roleName: String): String? = try {
        roleManager?.getRoleHolders(roleName)?.firstOrNull()
    } catch (e: Exception) {
        Log.w(tag, "getRoleHolder failed for $roleName", e)
        null
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

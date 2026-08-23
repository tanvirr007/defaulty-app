package app.defaulty.data.system

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Helper object managing communication with Shizuku (ADB privileged shell service).
 *
 * Allows Defaulty to execute `cmd role add-role-holder` directly in the background
 * when the user has Shizuku or wireless debugging enabled on their device.
 */
object ShizukuManager {
    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 2001

    /**
     * Checks if the Shizuku IPC binder service is alive and reachable.
     */
    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    /**
     * Checks if Defaulty has been granted permission to use Shizuku shell.
     */
    fun hasShizukuPermission(): Boolean = try {
        if (isShizukuAvailable()) {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } else {
            false
        }
    } catch (e: Throwable) {
        false
    }

    /**
     * Requests Shizuku shell execution permission.
     */
    fun requestPermission(requestCode: Int = SHIZUKU_PERMISSION_REQUEST_CODE) {
        try {
            if (isShizukuAvailable()) {
                if (!hasShizukuPermission()) {
                    Shizuku.requestPermission(requestCode)
                }
            } else {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to request Shizuku permission", e)
        }
    }

    /**
     * Helper to execute a command through Shizuku shell process via reflection.
     */
    private fun exec(cmd: Array<String>): Boolean {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, cmd, null, null) as Process
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to execute Shizuku command: ${cmd.joinToString(" ")}", e)
            false
        }
    }

    /**
     * Executes `cmd role add-role-holder` directly under the ADB shell UID (2000)
     * to apply a default system role instantly without navigating to Settings.
     */
    suspend fun applyDefaultRole(roleName: String, packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext false
        val cmd = arrayOf("cmd", "role", "add-role-holder", roleName, packageName, "0")
        exec(cmd)
    }

    /**
     * Executes `cmd package set-home-activity` or role add for the launcher.
     */
    suspend fun applyHomeLauncher(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext false
        val cmd = arrayOf("cmd", "role", "add-role-holder", "android.app.role.HOME", packageName, "0")
        exec(cmd)
    }
}


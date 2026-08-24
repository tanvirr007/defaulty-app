package app.defaulty.data.system

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Helper object managing communication with Shizuku (ADB privileged shell service).
 *
 * Allows Defaulty to execute `cmd role add-role-holder` directly in the background
 * when the user has Shizuku or wireless debugging enabled on their device.
 */
object ShizukuManager {
    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 2001

    /** Timeout for command execution (milliseconds). */
    private const val EXEC_TIMEOUT_MS = 10_000L

    /**
     * Global binder state tracked via sticky listeners in DefaultyApp.
     */
    @Volatile
    var isBinderAlive: Boolean = false
        private set

    /**
     * Called by DefaultyApp when the Shizuku binder is received.
     */
    fun onBinderReceived() {
        isBinderAlive = true
    }

    /**
     * Called by DefaultyApp when the Shizuku binder dies.
     */
    fun onBinderDead() {
        isBinderAlive = false
    }

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
     * Executes a command through Shizuku's privileged shell process via reflection.
     *
     * Uses reflection to access Shizuku.newProcess() which is not public in the API.
     * Drains both stdout and stderr streams before waitFor() to prevent
     * pipe buffer deadlocks on Android.
     */
    private suspend fun exec(cmd: Array<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = withTimeoutOrNull(EXEC_TIMEOUT_MS) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, cmd, null, null) as Process

                // Drain both streams BEFORE waitFor() to prevent pipe buffer deadlocks
                val stdout = drainStream(process.inputStream)
                val stderr = drainStream(process.errorStream)
                val exitCode = process.waitFor()

                Log.d(TAG, "Shizuku exec '${cmd.joinToString(" ")}': exit=$exitCode")
                if (exitCode != 0) {
                    Log.w(TAG, "Shizuku exec stderr: $stderr")
                }
                exitCode == 0
            }
            result ?: run {
                Log.w(TAG, "Shizuku exec timed out: ${cmd.joinToString(" ")}")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to execute Shizuku command: ${cmd.joinToString(" ")}", e)
            false
        }
    }

    /**
     * Executes `cmd role add-role-holder` directly under the ADB shell UID (2000)
     * to apply a default system role instantly without navigating to Settings.
     *
     * Tries `--user 0` flag first, falls back to positional `0` argument for compatibility.
     */
    suspend fun applyDefaultRole(roleName: String, packageName: String): Boolean {
        if (!hasShizukuPermission()) return false

        // Primary: --user 0 flag (clean AOSP syntax)
        val primaryCmd = arrayOf("cmd", "role", "add-role-holder", "--user", "0", roleName, packageName)
        if (exec(primaryCmd)) return true

        // Fallback: positional user argument (OEM compat)
        val fallbackCmd = arrayOf("cmd", "role", "add-role-holder", roleName, packageName, "0")
        return exec(fallbackCmd)
    }

    /**
     * Executes role add for the launcher.
     */
    suspend fun applyHomeLauncher(packageName: String): Boolean {
        if (!hasShizukuPermission()) return false
        return applyDefaultRole("android.app.role.HOME", packageName)
    }

    /**
     * Clears all preferred activities for a package (e.g. Media/File "Always" defaults).
     * Once cleared, Android's Intent Resolver will prompt the user with "Just once" / "Always" again.
     */
    suspend fun clearPackagePreferredActivities(packageName: String): Boolean {
        if (!hasShizukuPermission()) return false
        val primaryCmd = arrayOf("cmd", "package", "clear-package-preferred-activities", "--user", "0", packageName)
        if (exec(primaryCmd)) return true

        val fallbackCmd = arrayOf("pm", "clear-package-preferred-activities", packageName)
        return exec(fallbackCmd)
    }

    /**
     * Removes a role holder from a specific system role.
     */
    suspend fun removeRoleHolder(roleName: String, packageName: String): Boolean {
        if (!hasShizukuPermission()) return false
        val primaryCmd = arrayOf("cmd", "role", "remove-role-holder", "--user", "0", roleName, packageName, "0")
        if (exec(primaryCmd)) return true

        val fallbackCmd = arrayOf("cmd", "role", "remove-role-holder", roleName, packageName, "0")
        return exec(fallbackCmd)
    }

    /**
     * Clears all role holders for a specific system role.
     */
    suspend fun clearRoleHolders(roleName: String): Boolean {
        if (!hasShizukuPermission()) return false
        val primaryCmd = arrayOf("cmd", "role", "clear-role-holders", "--user", "0", roleName, "0")
        if (exec(primaryCmd)) return true

        val fallbackCmd = arrayOf("cmd", "role", "clear-role-holders", roleName, "0")
        return exec(fallbackCmd)
    }

    /**
     * Drains an InputStream into a String, reading all available bytes.
     * Must be called before process.waitFor() to prevent pipe buffer deadlocks.
     */
    private fun drainStream(stream: java.io.InputStream): String {
        return try {
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText().trim()
            }
        } catch (e: Throwable) {
            ""
        }
    }
}

package app.defaulty.data.system

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Direct root shell executor for devices with KernelSU, Magisk, or APatch.
 *
 * Executes privileged commands via `su -c` locally on-device.
 * All execution is 100% offline — no network access required.
 */
object RootShellManager {
    private const val TAG = "RootShellManager"

    /** Timeout for root availability probe (milliseconds). */
    private const val PROBE_TIMEOUT_MS = 5_000L

    /** Timeout for command execution (milliseconds). */
    private const val EXEC_TIMEOUT_MS = 10_000L

    /**
     * Cached root availability result to avoid repeated `su` invocations.
     * Reset to null when re-probing is needed.
     */
    @Volatile
    private var cachedRootAvailable: Boolean? = null

    /**
     * Checks if superuser (`su`) binary is available and grants shell access.
     *
     * Probes multiple known paths and attempts a lightweight `su -c id` to confirm
     * that the root manager (KernelSU, Magisk, APatch) actually grants permission.
     *
     * Result is cached after the first successful probe.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        cachedRootAvailable?.let { return@withContext it }

        val result = probeRoot()
        cachedRootAvailable = result
        result
    }

    /**
     * Clears the cached root availability check.
     * Call this when the user explicitly requests a re-probe (e.g. after granting root).
     */
    fun clearCache() {
        cachedRootAvailable = null
    }

    /**
     * Probes for root access by running `su -c id`.
     * Returns true if the command succeeds with exit code 0.
     */
    private suspend fun probeRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val stdout = drainStream(process.inputStream)
                val stderr = drainStream(process.errorStream)
                val exitCode = process.waitFor()

                Log.d(TAG, "Root probe: exit=$exitCode, stdout=$stdout, stderr=$stderr")
                exitCode == 0 && stdout.contains("uid=0")
            }
            result ?: false
        } catch (e: Throwable) {
            Log.d(TAG, "Root probe failed: ${e.message}")
            false
        }
    }

    /**
     * Executes an ADB shell command with root privileges via `su -c`.
     *
     * @param command The full shell command string to execute (e.g. "cmd role add-role-holder ...").
     * @return true if the command executed successfully (exit code 0), false otherwise.
     */
    suspend fun executeCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = withTimeoutOrNull(EXEC_TIMEOUT_MS) {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

                // Drain both streams to prevent pipe buffer deadlocks
                val stdout = drainStream(process.inputStream)
                val stderr = drainStream(process.errorStream)
                val exitCode = process.waitFor()

                Log.d(TAG, "Root exec '$command': exit=$exitCode")
                if (exitCode != 0) {
                    Log.w(TAG, "Root exec stderr: $stderr")
                }
                exitCode == 0
            }
            result ?: run {
                Log.w(TAG, "Root exec timed out: $command")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Root exec failed: $command", e)
            false
        }
    }

    /**
     * Applies a default role via root shell.
     *
     * Tries `cmd role add-role-holder --user 0 <role> <package>` first,
     * falls back to `cmd role add-role-holder <role> <package> 0` for compatibility.
     */
    suspend fun applyDefaultRole(roleName: String, packageName: String): Boolean {
        // Primary command with --user flag (works on most AOSP / stock Android)
        val primaryCmd = "cmd role add-role-holder --user 0 $roleName $packageName"
        if (executeCommand(primaryCmd)) return true

        // Fallback for OEM ROMs that accept positional user argument
        val fallbackCmd = "cmd role add-role-holder $roleName $packageName 0"
        return executeCommand(fallbackCmd)
    }

    /**
     * Applies the home launcher via root shell.
     */
    suspend fun applyHomeLauncher(packageName: String): Boolean {
        return applyDefaultRole("android.app.role.HOME", packageName)
    }

    /**
     * Clears all preferred activities for a package (e.g. Media/File "Always" defaults).
     * Once cleared, Android's Intent Resolver will prompt the user with "Just once" / "Always" again.
     */
    suspend fun clearPackagePreferredActivities(packageName: String): Boolean {
        val primaryCmd = "cmd package clear-package-preferred-activities --user 0 $packageName"
        if (executeCommand(primaryCmd)) return true

        val fallbackCmd = "pm clear-package-preferred-activities $packageName"
        return executeCommand(fallbackCmd)
    }

    /**
     * Removes a role holder from a specific system role via root shell.
     */
    suspend fun removeRoleHolder(roleName: String, packageName: String): Boolean {
        val primaryCmd = "cmd role remove-role-holder --user 0 $roleName $packageName 0"
        if (executeCommand(primaryCmd)) return true

        val fallbackCmd = "cmd role remove-role-holder $roleName $packageName 0"
        return executeCommand(fallbackCmd)
    }

    /**
     * Clears all role holders for a specific system role via root shell.
     */
    suspend fun clearRoleHolders(roleName: String): Boolean {
        val primaryCmd = "cmd role clear-role-holders --user 0 $roleName 0"
        if (executeCommand(primaryCmd)) return true

        val fallbackCmd = "cmd role clear-role-holders $roleName 0"
        return executeCommand(fallbackCmd)
    }

    /**
     * Drains an InputStream into a String, reading all available bytes.
     * Must be called before process.waitFor() to prevent deadlocks.
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

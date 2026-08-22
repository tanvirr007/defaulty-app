package app.defaulty.data.system

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import android.util.Log
import app.defaulty.domain.model.LinkHandlingAppInfo

/**
 * Safe wrapper around DomainVerificationManager (API 31+).
 *
 * Reads per-app link-handling state — verified domains and whether
 * the user has enabled/disabled link opening for each app.
 *
 * All calls are treated as fallible at runtime (Product Rule 14).
 */
class DomainVerificationWrapper(private val context: Context) {

    private val tag = "DomainVerifyWrapper"

    private val manager: DomainVerificationManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(DomainVerificationManager::class.java)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w(tag, "Failed to get DomainVerificationManager", e)
        null
    }

    /**
     * Get all installed apps that have domain verification configured,
     * along with their verified domains and link-handling state.
     *
     * Never crashes due to a misbehaving third-party app (Spec Section 19).
     */
    fun getAppsWithLinkHandling(): List<LinkHandlingAppInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        val dvm = manager ?: return emptyList()
        val pm = context.packageManager
        val result = mutableListOf<LinkHandlingAppInfo>()

        try {
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            for (pkg in packages) {
                try {
                    val userState = dvm.getDomainVerificationUserState(pkg.packageName)
                        ?: continue

                    val hostMap = userState.hostToStateMap
                    if (hostMap.isEmpty()) continue

                    val verified = hostMap.entries
                        .filter { (_, state) ->
                            state == DomainVerificationUserState.DOMAIN_STATE_VERIFIED ||
                                state == DomainVerificationUserState.DOMAIN_STATE_SELECTED
                        }
                        .map { it.key }

                    val allDomains = hostMap.keys.toList()
                    val isEnabled = userState.isLinkHandlingAllowed

                    val appLabel = try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(pkg.packageName, 0)
                        ).toString()
                    } catch (_: Exception) {
                        pkg.packageName
                    }

                    val appIcon = try {
                        pm.getApplicationIcon(pkg.packageName)
                    } catch (_: Exception) {
                        null
                    }

                    if (allDomains.isNotEmpty()) {
                        result.add(
                            LinkHandlingAppInfo(
                                packageName = pkg.packageName,
                                appLabel = appLabel,
                                appIcon = appIcon,
                                verifiedDomains = verified,
                                allDomains = allDomains,
                                isLinkHandlingEnabled = isEnabled,
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip this package — never crash because another app behaves badly
                    Log.w(tag, "Skipping ${pkg.packageName}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to enumerate packages", e)
        }

        return result.sortedBy { it.appLabel.lowercase() }
    }
}

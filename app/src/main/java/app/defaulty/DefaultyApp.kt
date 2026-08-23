package app.defaulty

import android.app.Application
import android.util.Log
import app.defaulty.data.preferences.UserPreferences
import app.defaulty.data.repository.DefaultAppsRepository
import app.defaulty.data.system.DomainVerificationWrapper
import app.defaulty.data.system.RoleManagerWrapper
import app.defaulty.data.system.ShizukuManager
import rikka.shizuku.Shizuku

/**
 * Application class providing manual dependency injection container.
 * Keeps external dependencies minimal per spec (no Hilt/Dagger needed).
 *
 * Initializes global Shizuku binder listeners so binder state
 * is tracked throughout the entire application lifecycle.
 */
class DefaultyApp : Application() {

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var roleManagerWrapper: RoleManagerWrapper
        private set

    lateinit var domainVerificationWrapper: DomainVerificationWrapper
        private set

    lateinit var defaultAppsRepository: DefaultAppsRepository
        private set

    private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        roleManagerWrapper = RoleManagerWrapper(this)
        domainVerificationWrapper = DomainVerificationWrapper(this)
        defaultAppsRepository = DefaultAppsRepository(
            context = this,
            roleManagerWrapper = roleManagerWrapper,
            domainVerificationWrapper = domainVerificationWrapper,
        )

        // Register sticky Shizuku binder listeners at application startup
        // so binder state is always tracked even before any UI screen appears.
        initShizukuBinderListeners()
    }

    private fun initShizukuBinderListeners() {
        try {
            binderReceivedListener = Shizuku.OnBinderReceivedListener {
                ShizukuManager.onBinderReceived()
                Log.d("DefaultyApp", "Shizuku binder received")
            }
            binderDeadListener = Shizuku.OnBinderDeadListener {
                ShizukuManager.onBinderDead()
                Log.d("DefaultyApp", "Shizuku binder dead")
            }
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener!!)
            Shizuku.addBinderDeadListener(binderDeadListener!!)
        } catch (e: Throwable) {
            Log.w("DefaultyApp", "Failed to register Shizuku listeners", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            binderReceivedListener?.let { Shizuku.removeBinderReceivedListener(it) }
            binderDeadListener?.let { Shizuku.removeBinderDeadListener(it) }
        } catch (_: Throwable) {}
    }
}

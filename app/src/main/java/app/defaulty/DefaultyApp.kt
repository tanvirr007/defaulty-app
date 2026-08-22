package app.defaulty

import android.app.Application
import app.defaulty.data.preferences.UserPreferences
import app.defaulty.data.repository.DefaultAppsRepository
import app.defaulty.data.system.DomainVerificationWrapper
import app.defaulty.data.system.RoleManagerWrapper

/**
 * Application class providing manual dependency injection container.
 * Keeps external dependencies minimal per spec (no Hilt/Dagger needed).
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
    }
}

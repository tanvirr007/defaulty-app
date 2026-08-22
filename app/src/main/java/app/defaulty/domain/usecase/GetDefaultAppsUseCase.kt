package app.defaulty.domain.usecase

import app.defaulty.data.repository.DefaultAppsRepository
import app.defaulty.domain.model.DefaultAppInfo
import app.defaulty.domain.model.SupportedRole

/**
 * Use case for querying default app roles.
 * Delegates to [DefaultAppsRepository] with capability filtering.
 */
class GetDefaultAppsUseCase(private val repository: DefaultAppsRepository) {

    /** Get all available default app roles on this device. */
    operator fun invoke(): List<DefaultAppInfo> = repository.getAvailableDefaults()

    /** Get the default app info for a specific role. */
    fun forRole(role: SupportedRole): DefaultAppInfo? = repository.getDefaultForRole(role)
}

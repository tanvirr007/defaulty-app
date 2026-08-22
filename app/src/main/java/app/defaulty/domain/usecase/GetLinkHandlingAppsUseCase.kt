package app.defaulty.domain.usecase

import app.defaulty.data.repository.DefaultAppsRepository
import app.defaulty.domain.model.LinkHandlingAppInfo

/**
 * Use case for querying apps with link-handling configuration.
 * Delegates to [DefaultAppsRepository] which reads DomainVerificationManager.
 */
class GetLinkHandlingAppsUseCase(private val repository: DefaultAppsRepository) {

    /** Get all apps with domain verification / link handling configured. */
    operator fun invoke(): List<LinkHandlingAppInfo> = repository.getLinkHandlingApps()
}

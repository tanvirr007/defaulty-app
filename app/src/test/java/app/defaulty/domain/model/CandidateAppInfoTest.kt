package app.defaulty.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateAppInfoTest {

    @Test
    fun `candidate app info stores properties correctly`() {
        val candidate = CandidateAppInfo(
            packageName = "org.mozilla.firefox",
            appLabel = "Firefox",
            appIcon = null,
            isDefault = true,
        )

        assertEquals("org.mozilla.firefox", candidate.packageName)
        assertEquals("Firefox", candidate.appLabel)
        assertTrue(candidate.isDefault)
    }

    @Test
    fun `candidate app info non-default state`() {
        val candidate = CandidateAppInfo(
            packageName = "com.brave.browser",
            appLabel = "Brave",
            appIcon = null,
            isDefault = false,
        )

        assertFalse(candidate.isDefault)
    }
}

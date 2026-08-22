package app.defaulty.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedRoleTest {

    @Test
    fun `all roles have non-empty roleName, displayLabel, and description`() {
        SupportedRole.entries.forEach { role ->
            assertTrue("roleName for $role must not be empty", role.roleName.isNotBlank())
            assertTrue("displayLabel for $role must not be empty", role.displayLabel.isNotBlank())
            assertTrue("description for $role must not be empty", role.description.isNotBlank())
            assertNotNull("icon for $role must not be null", role.icon)
        }
    }

    @Test
    fun `fromRoleName returns correct role for known strings`() {
        assertEquals(SupportedRole.BROWSER, SupportedRole.fromRoleName("android.app.role.BROWSER"))
        assertEquals(SupportedRole.PHONE, SupportedRole.fromRoleName("android.app.role.DIALER"))
        assertEquals(SupportedRole.SMS, SupportedRole.fromRoleName("android.app.role.SMS"))
        assertEquals(SupportedRole.HOME, SupportedRole.fromRoleName("android.app.role.HOME"))
        assertEquals(SupportedRole.ASSISTANT, SupportedRole.fromRoleName("android.app.role.ASSISTANT"))
    }

    @Test
    fun `fromRoleName returns null for unknown role strings`() {
        assertNull(SupportedRole.fromRoleName("android.app.role.FUTURE_UNKNOWN_ROLE"))
    }

    @Test
    fun `fromId returns correct role for enum names`() {
        assertEquals(SupportedRole.BROWSER, SupportedRole.fromId("BROWSER"))
        assertEquals(SupportedRole.CALL_SCREENING, SupportedRole.fromId("CALL_SCREENING"))
        assertNull(SupportedRole.fromId("NON_EXISTENT_ID"))
    }

    @Test
    fun `primary roles contain browser, phone, sms, home, assistant`() {
        val primaryRoles = SupportedRole.entries.filter { it.isPrimary }
        assertTrue(primaryRoles.contains(SupportedRole.BROWSER))
        assertTrue(primaryRoles.contains(SupportedRole.PHONE))
        assertTrue(primaryRoles.contains(SupportedRole.SMS))
        assertTrue(primaryRoles.contains(SupportedRole.HOME))
        assertTrue(primaryRoles.contains(SupportedRole.ASSISTANT))
    }
}

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
            assertTrue("sortOrder for $role must be positive", role.sortOrder > 0)
        }
    }

    @Test
    fun `fromRoleName returns correct role for known strings`() {
        assertEquals(SupportedRole.BROWSER, SupportedRole.fromRoleName("android.app.role.BROWSER"))
        assertEquals(SupportedRole.PHONE, SupportedRole.fromRoleName("android.app.role.DIALER"))
        assertEquals(SupportedRole.SMS, SupportedRole.fromRoleName("android.app.role.SMS"))
        assertEquals(SupportedRole.HOME, SupportedRole.fromRoleName("android.app.role.HOME"))
        assertEquals(SupportedRole.ASSISTANT, SupportedRole.fromRoleName("android.app.role.ASSISTANT"))
        assertEquals(SupportedRole.NOTES, SupportedRole.fromRoleName("android.app.role.NOTES"))
        assertEquals(SupportedRole.WALLET, SupportedRole.fromRoleName("android.app.role.WALLET"))
        assertEquals(SupportedRole.EMERGENCY, SupportedRole.fromRoleName("android.app.role.EMERGENCY"))
    }

    @Test
    fun `fromRoleName returns null for unknown role strings`() {
        assertNull(SupportedRole.fromRoleName("android.app.role.FUTURE_UNKNOWN_ROLE"))
    }

    @Test
    fun `fromId returns correct role for enum names`() {
        assertEquals(SupportedRole.BROWSER, SupportedRole.fromId("BROWSER"))
        assertEquals(SupportedRole.CALL_SCREENING, SupportedRole.fromId("CALL_SCREENING"))
        assertEquals(SupportedRole.NOTES, SupportedRole.fromId("NOTES"))
        assertNull(SupportedRole.fromId("NON_EXISTENT_ID"))
    }

    @Test
    fun `primary roles contain browser, phone, sms, home, assistant`() {
        val primaryRoles = SupportedRole.entries.filter { it.isPrimary }
        assertEquals(5, primaryRoles.size)
        assertTrue(primaryRoles.contains(SupportedRole.BROWSER))
        assertTrue(primaryRoles.contains(SupportedRole.PHONE))
        assertTrue(primaryRoles.contains(SupportedRole.SMS))
        assertTrue(primaryRoles.contains(SupportedRole.HOME))
        assertTrue(primaryRoles.contains(SupportedRole.ASSISTANT))
    }

    @Test
    fun `getAdbCommand returns valid cmd role command format`() {
        assertEquals(
            "cmd role add-role-holder android.app.role.BROWSER com.brave.browser 0",
            SupportedRole.BROWSER.getAdbCommand("com.brave.browser"),
        )
        assertEquals(
            "cmd role add-role-holder android.app.role.DIALER com.google.android.dialer 0",
            SupportedRole.PHONE.getAdbCommand("com.google.android.dialer"),
        )
        assertEquals(
            "cmd role add-role-holder android.app.role.SMS com.google.android.apps.messaging 0",
            SupportedRole.SMS.getAdbCommand("com.google.android.apps.messaging"),
        )
        assertEquals(
            "cmd role add-role-holder android.app.role.HOME com.teslacoilsw.launcher 0",
            SupportedRole.HOME.getAdbCommand("com.teslacoilsw.launcher"),
        )
    }

    @Test
    fun `canClearHolder is false only for HOME role`() {
        org.junit.Assert.assertFalse("HOME role must not be clearable", SupportedRole.HOME.canClearHolder)
        SupportedRole.entries.filter { it != SupportedRole.HOME }.forEach { role ->
            assertTrue("$role must be clearable", role.canClearHolder)
        }
    }
}

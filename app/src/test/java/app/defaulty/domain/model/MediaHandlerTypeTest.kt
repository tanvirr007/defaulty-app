package app.defaulty.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaHandlerTypeTest {

    @Test
    fun `all media handler types have valid icons and resources`() {
        MediaHandlerType.entries.forEach { type ->
            assertTrue("id for $type must not be empty", type.id.isNotBlank())
            assertTrue("displayLabelRes for $type must be non-zero", type.displayLabelRes != 0)
            assertTrue("descriptionRes for $type must be non-zero", type.descriptionRes != 0)
            assertNotNull("icon for $type must not be null", type.icon)
            assertTrue("sortOrder for $type must be positive", type.sortOrder > 0)
        }
    }

    @Test
    fun `fromId returns correct media type for known ids`() {
        assertEquals(MediaHandlerType.VIDEO_PLAYER, MediaHandlerType.fromId("VIDEO_PLAYER"))
        assertEquals(MediaHandlerType.GALLERY, MediaHandlerType.fromId("GALLERY"))
        assertEquals(MediaHandlerType.MUSIC_PLAYER, MediaHandlerType.fromId("MUSIC_PLAYER"))
        assertEquals(MediaHandlerType.PDF_VIEWER, MediaHandlerType.fromId("PDF_VIEWER"))
        assertEquals(MediaHandlerType.EMAIL, MediaHandlerType.fromId("EMAIL"))
        assertNull(MediaHandlerType.fromId("UNKNOWN_MEDIA_ID"))
    }
}

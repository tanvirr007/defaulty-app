package app.defaulty.domain.model

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.vector.ImageVector
import app.defaulty.R

/**
 * Standard media, file, and protocol handler categories managed via Intent Resolver.
 */
enum class MediaHandlerType(
    val id: String,
    val displayLabelRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val mimeType: String?,
    val action: String,
    val sortOrder: Int,
) {
    VIDEO_PLAYER(
        id = "VIDEO_PLAYER",
        displayLabelRes = R.string.media_video_player,
        descriptionRes = R.string.media_video_player_desc,
        icon = Icons.Default.Movie,
        mimeType = "video/*",
        action = Intent.ACTION_VIEW,
        sortOrder = 1,
    ),
    GALLERY(
        id = "GALLERY",
        displayLabelRes = R.string.media_gallery,
        descriptionRes = R.string.media_gallery_desc,
        icon = Icons.Default.PhotoLibrary,
        mimeType = "image/*",
        action = Intent.ACTION_VIEW,
        sortOrder = 2,
    ),
    MUSIC_PLAYER(
        id = "MUSIC_PLAYER",
        displayLabelRes = R.string.media_music_player,
        descriptionRes = R.string.media_music_player_desc,
        icon = Icons.Default.Audiotrack,
        mimeType = "audio/*",
        action = Intent.ACTION_VIEW,
        sortOrder = 3,
    ),
    PDF_VIEWER(
        id = "PDF_VIEWER",
        displayLabelRes = R.string.media_pdf_viewer,
        descriptionRes = R.string.media_pdf_viewer_desc,
        icon = Icons.Default.PictureAsPdf,
        mimeType = "application/pdf",
        action = Intent.ACTION_VIEW,
        sortOrder = 4,
    ),
    EMAIL(
        id = "EMAIL",
        displayLabelRes = R.string.media_email,
        descriptionRes = R.string.media_email_desc,
        icon = Icons.Default.Email,
        mimeType = null,
        action = Intent.ACTION_SENDTO,
        sortOrder = 5,
    );

    companion object {
        fun fromId(id: String): MediaHandlerType? =
            entries.find { it.id == id || it.name == id }
    }
}

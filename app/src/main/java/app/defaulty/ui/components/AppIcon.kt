package app.defaulty.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Displays an app icon from a Drawable, converting to Compose-compatible
 * ImageBitmap. Falls back to the Android icon inside a tonal container if the drawable is null.
 */
@Composable
fun AppIcon(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    clipRadius: Dp = 10.dp,
) {
    if (drawable != null) {
        val imageBitmap = remember(drawable) {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(clipRadius)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(clipRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


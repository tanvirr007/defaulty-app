package app.defaulty.ui.components

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Global Toast utility that cancels any currently visible/queued toast before
 * displaying a new one.
 *
 * Prevents rapid multi-tap toast spamming across the entire application.
 */
object DefaultyToast {
    private var currentToast: Toast? = null

    /**
     * Shows a toast with the given [message], cancelling any existing toast first.
     */
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        currentToast?.cancel()
        val toast = Toast.makeText(context.applicationContext, message, duration)
        currentToast = toast
        toast.show()
    }

    /**
     * Shows a toast with the string resource [messageRes], cancelling any existing toast first.
     */
    fun show(context: Context, @StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
        show(context, context.getString(messageRes), duration)
    }
}

package mx.alxr.voicenotes.utils.extensions

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

fun Activity.hideSoftKeyboard() {
    val token = (currentFocus ?: findViewById(android.R.id.content))?.windowToken ?: return
    val manager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    manager?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
}

fun AppCompatActivity.setupToolbar(
    toolbar: Toolbar,
    homeAsUp: Boolean = true,
    showTitle: Boolean = true,
    indicator: Drawable? = null
) {
    setSupportActionBar(toolbar)

    if (indicator != null) {
        toolbar.navigationIcon = indicator
    }

    supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(homeAsUp)
        setDisplayShowTitleEnabled(showTitle)
    }

}
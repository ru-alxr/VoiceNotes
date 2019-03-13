package mx.alxr.voicenotes.utils.extensions

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import mx.alxr.voicenotes.R

fun Fragment.format(resId: Int, arg: String): String {
    return String.format(getString(resId), arg)
}

fun Fragment.showDualSelectorDialog(
    message: String,
    negativeLabel: Int,
    positiveLabel: Int,
    negative: () -> Unit = {},
    positive: () -> Unit
) {
    activity?.apply {
        AlertDialog
            .Builder(this)
            .setView(R.layout.dialog_preload_error)
            .setCancelable(false)
            .show()
            .apply {
                findViewById<TextView>(R.id.message)?.apply {
                    text = message
                }
                findViewById<TextView>(R.id.negative)?.apply {
                    setText(negativeLabel)
                    setOnClickListener {
                        dismiss()
                        negative.invoke()
                    }
                }
                findViewById<TextView>(R.id.positive)?.apply {
                    setText(positiveLabel)
                    setOnClickListener {
                        dismiss()
                        positive.invoke()
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
    }

}

fun Fragment.showTripleSelectorDialog(
    message: Int,
    firstLabel: Int,
    secondLabel: Int,
    thirdLabel: Int,
    first: () -> Unit,
    second: () -> Unit,
    third: () -> Unit = {}

) {
    activity?.apply {
        AlertDialog
            .Builder(this)
            .setView(R.layout.dialog_triple_selection)
            .setCancelable(false)
            .show()
            .apply {
                findViewById<TextView>(R.id.message)?.apply {
                    setText(message)
                }
                findViewById<TextView>(R.id.selection_one)?.apply {
                    setText(firstLabel)
                    setOnClickListener {
                        dismiss()
                        first.invoke()
                    }
                }
                findViewById<TextView>(R.id.selection_two)?.apply {
                    setText(secondLabel)
                    setOnClickListener {
                        dismiss()
                        second.invoke()
                    }
                }
                findViewById<TextView>(R.id.selection_three)?.apply {
                    setText(thirdLabel)
                    setOnClickListener {
                        dismiss()
                        third.invoke()
                    }
                }
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
    }

}
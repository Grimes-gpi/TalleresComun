package com.example.clase18_08zulema.ui

import android.content.Context
import android.graphics.Color
import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

internal object Ui {
    val ink = Color.rgb(0, 135, 178)
    val green = Color.rgb(20, 124, 105)
    val orange = Color.rgb(156, 71, 53)
    val background = Color.rgb(245, 249, 251)
    val muted = Color.rgb(98, 113, 124)
    val dark = Color.rgb(32, 42, 50)

    fun page(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 28))
        setBackgroundColor(Ui.background)
    }

    fun title(context: Context, value: String, size: Float = 26f) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(dark)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    fun body(context: Context, value: String, size: Float = 15f) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(muted)
    }

    fun button(context: Context, label: String, color: Int = ink, onClick: () -> Unit) =
        MaterialButton(context).apply {
            text = label
            backgroundTintList = ColorStateList.valueOf(color)
            setTextColor(Color.WHITE)
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    fun field(context: Context, label: String, inputType: Int): Pair<TextInputLayout, TextInputEditText> {
        val edit = TextInputEditText(context).apply { this.inputType = inputType; textSize = 16f }
        val layout = TextInputLayout(context).apply { hint = label; addView(edit) }
        return layout to edit
    }

    fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun gap(context: Context, height: Int = 12) = View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, height)) }
}

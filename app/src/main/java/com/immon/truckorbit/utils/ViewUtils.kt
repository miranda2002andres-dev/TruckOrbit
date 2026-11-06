package com.immon.truckorbit.utils

import android.content.Context
import android.view.View
import android.view.Window
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

fun View.applyWindowInsets(top: Boolean, bottom: Boolean) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v: View, insets: WindowInsetsCompat ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val paddingTop = if (top) systemBars.top else paddingTop
        val paddingBottom = if (bottom) systemBars.bottom else paddingBottom
        val paddingLeft = systemBars.left + paddingLeft
        val paddingRight = systemBars.right + paddingRight

        v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        insets
    }
}

fun MaterialToolbar.setTitle(
    context: Context,
    @StringRes title: Int,
    showBackButton: Boolean
) {
    setTitle(context, context.getString(title), showBackButton)
}

@Suppress("DEPRECATION")
fun MaterialToolbar.setTitle(
    context: Context,
    title: String,
    showBackButton: Boolean
) {
    val activity = context as AppCompatActivity
    activity.setSupportActionBar(this)
    val actionBar = context.supportActionBar

    if (actionBar != null) {
        context.apply {
            supportActionBar!!.title = title
            supportActionBar!!.setDisplayHomeAsUpEnabled(showBackButton)
            supportActionBar!!.setDisplayShowHomeEnabled(showBackButton)
        }

        if (showBackButton) {
            setNavigationOnClickListener { activity.onBackPressed() }
        }

        val horizontalPadding =
            (if (showBackButton) 12 else 28) * resources.displayMetrics.density.toInt()

        setPaddingRelative(
            horizontalPadding,
            paddingTop,
            horizontalPadding,
            paddingBottom
        )
    }
}

fun Window.setLightStatusBar(isDark: Boolean) {
    WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = !isDark
}

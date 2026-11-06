package com.immon.truckorbit.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

object DrawableUtils {

    fun Drawable?.drawableToBitmap(): Bitmap {
        if (this == null) {
            throw IllegalArgumentException("Drawable cannot be null")
        }

        val width = intrinsicWidth
        val height = intrinsicHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)

        return bitmap
    }
}

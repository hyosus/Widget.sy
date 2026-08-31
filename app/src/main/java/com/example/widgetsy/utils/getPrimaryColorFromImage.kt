package com.example.widgetsy.utils

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

fun getPrimaryColorFromImage(bitmap: Bitmap): Int {
    val palette = Palette.from(bitmap).generate()
    return palette.getDominantColor(1) // Default color if no dominant color is found
}
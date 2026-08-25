package com.example.widgetsy.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun getTextColor(bgColor: Color): Color {
    return if (bgColor.luminance() > 0.5) {
        Color.Black
    } else {
        Color.White
    }
}
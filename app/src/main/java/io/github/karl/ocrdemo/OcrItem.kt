package io.github.karl.ocrdemo

import android.graphics.Color

data class OcrItem(
    val boxes: List<List<Int>> = listOf(),
    val text: String,
    val color: Color,
    val score: Float
)

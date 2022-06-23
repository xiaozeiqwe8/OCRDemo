package io.github.karl.ocrdemo

class OcrImageListener(private val clickListener: (position: Int, ocrItem : OcrItem) -> Unit) {
    fun onClick(position: Int, ocrItem: OcrItem) = clickListener(position, ocrItem)
}
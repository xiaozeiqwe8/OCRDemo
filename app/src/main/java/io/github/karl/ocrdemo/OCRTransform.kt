package io.github.karl.ocrdemo

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool

import android.graphics.*

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.Charset
import java.security.MessageDigest

class OCRTransform(private val ocrItems: List<OcrItem>,
                   private val originWidth: Int, private val originHeight: Int) : BitmapTransformation() {
    private val mPaint: Paint = Paint()

    companion object {
        private const val ID: String = "io.github.karl.ocrdemo.OCRTransform"
        val ID_BYTES: ByteArray = ID.toByteArray(Charset.forName("UTF-8"))
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun transform (
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return paintLocation(toTransform)
    }

    private fun paintLocation(oldBitmap: Bitmap): Bitmap {
        val widthRatio: Float = 1.0f * oldBitmap.width / originWidth
        val heightRatio: Float = 1.0f * oldBitmap.height / originHeight

        val canvas = Canvas(oldBitmap)
        ocrItems.forEach {
            mPaint.reset()
            mPaint.color = it.color.toArgb()
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 4f

            val point0 = it.boxes[0]
            val point1 = it.boxes[1]
            val point2 = it.boxes[2]
            val point3 = it.boxes[3]

            canvas.drawLine(point0[0] * widthRatio,  point0[1] * heightRatio,
                point1[0] * widthRatio, point1[1] * heightRatio, mPaint)

            canvas.drawLine(point1[0] * widthRatio,  point1[1] * heightRatio,
                point2[0] * widthRatio, point2[1] * heightRatio, mPaint)

            canvas.drawLine(point2[0] * widthRatio,  point2[1] * heightRatio,
                point3[0] * widthRatio, point3[1] * heightRatio, mPaint)

            canvas.drawLine(point3[0] * widthRatio,  point3[1] * heightRatio,
                point0[0] * widthRatio, point0[1] * heightRatio, mPaint)
        }
        return oldBitmap
    }
}
package io.github.karl.ocrdemo

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

class OcrImageView(context: Context, attributeSet: AttributeSet) :
    AppCompatImageView(context, attributeSet) {

    private var originWidth: Int = 0
    private var originHeight: Int = 0
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val ocrItems: MutableList<OcrItem> = mutableListOf()
    private val ocrRegions: MutableList<Region> = mutableListOf()

    private var touchX = 0
    private var touchY = 0

    private var clickOcrItem: OcrItem? = null

    private var ocrImageListener: OcrImageListener? = null

    private fun clickOcrItem(clickOcrItem: OcrItem) {
        Log.d("CustomImageView", "clickOcrItem = $clickOcrItem")
        if(this.clickOcrItem != clickOcrItem) {
            this.clickOcrItem = clickOcrItem
        }else{
            this.clickOcrItem = null
        }
        postInvalidate()
    }

    private fun findOcrItemWithRegion(x: Int, y: Int): OcrItem? {
        for ((index, region) in ocrRegions.withIndex()) {
            if (region.contains(x, y)) {
                return ocrItems[index]
            }
        }
        return null
    }

    private fun findOcrItemPositionWithRegion(x: Int, y: Int): Int? {
        for ((index, region) in ocrRegions.withIndex()) {
            if (region.contains(x, y)) {
                return index
            }
        }
        return null
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x.toInt()
                touchY = event.y.toInt()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
                if (touchX == event.x.toInt() && touchY == event.y.toInt()) {

                    if (ocrRegions.isEmpty()) {
                        return super.onTouchEvent(event)
                    }

                    val x = event.x.toInt()
                    val y = event.y.toInt()

                    val clickOcrItem: OcrItem? = findOcrItemWithRegion(x, y)
                    if (clickOcrItem != null) {
                        clickOcrItem(clickOcrItem)
                        ocrImageListener?.onClick(findOcrItemPositionWithRegion(x, y)!!, clickOcrItem)
                        super.performClick()
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            onDrawOcrBoxes(canvas)
        }

    }

    fun setOcrImageListener(ocrImageListener: OcrImageListener){
        this.ocrImageListener = ocrImageListener
    }

    fun drawOcrBoxes(
        ocrItems: List<OcrItem>,
        originWidth: Int,
        originHeight: Int
    ) {
        this.ocrItems.clear()
        this.ocrItems.addAll(ocrItems)
        this.originWidth = originWidth
        this.originHeight = originHeight
        this.postInvalidate()
    }

    fun clearOcrBoxes() {
        this.ocrItems.clear()
        postInvalidate()
    }

    fun selectOcrBox(ocrItem: OcrItem){
        this.clickOcrItem(ocrItem)
    }

    private fun onDrawOcrBoxes(canvas: Canvas) {
        Log.d(
            "CustomImageView",
            "canvas.width = ${canvas.width}, canvas.height = ${canvas.height}, originWidth = $originWidth, originHeight = $originHeight"
        )

        if (drawable == null) {
            return
        }

        if (ocrItems.isEmpty()) {
            ocrRegions.clear()
            return
        }

        val count = canvas.save()

        val values = FloatArray(9)
        imageMatrix.getValues(values)

        val offsetX = values[2]
        val offsetY = values[5]
        val scaleX = values[0]
        val scaleY = values[4]

        //图片在控件里的大小 * 在控件里的缩放比例 / 原始图片大小
        val widthRatio: Float =
            1.0f * drawable.intrinsicWidth * scaleX / originWidth
        val heightRatio: Float =
            1.0f * drawable.intrinsicHeight * scaleY / originHeight

        ocrRegions.clear()
        ocrItems.forEach {
            mPaint.reset()
            mPaint.color = it.color.toArgb()
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 4f

            val path = Path()

            val point0 = it.boxes[0]
            val point1 = it.boxes[1]
            val point2 = it.boxes[2]
            val point3 = it.boxes[3]

            path.moveTo(
                point0[0] * widthRatio + offsetX,
                point0[1] * heightRatio + offsetY
            )
            path.lineTo(
                point1[0] * widthRatio + offsetX,
                point1[1] * heightRatio + offsetY
            )
            path.lineTo(
                point2[0] * widthRatio + offsetX,
                point2[1] * heightRatio + offsetY
            )
            path.lineTo(
                point3[0] * widthRatio + offsetX,
                point3[1] * heightRatio + offsetY
            )
            path.lineTo(
                point0[0] * widthRatio + offsetX,
                point0[1] * heightRatio + offsetY
            )

            canvas.drawPath(path, mPaint)

            val region = buildRegion(path)
            ocrRegions.add(region)

            //画clickocritem
            if(this.clickOcrItem != null && this.clickOcrItem == it){
                mPaint.style = Paint.Style.FILL
                mPaint.color = ContextCompat.getColor(this.context, R.color.select_background)
                canvas.drawPath(path, mPaint)
            }
        }

        canvas.restoreToCount(count)
    }

    private fun buildRegion(path: Path): Region {
        val pathBoundsRect = RectF()
        path.computeBounds(pathBoundsRect, false)
        return Region().apply {
            setPath(
                path, Region(
                    pathBoundsRect.left.toInt(),
                    pathBoundsRect.top.toInt(),
                    pathBoundsRect.right.toInt(),
                    pathBoundsRect.bottom.toInt()
                )
            )
        }
    }

}
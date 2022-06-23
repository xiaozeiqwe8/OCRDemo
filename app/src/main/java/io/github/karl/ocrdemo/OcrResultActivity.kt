package io.github.karl.ocrdemo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.ocr_result_layout.*

class OcrResultActivity : AppCompatActivity(R.layout.ocr_result_layout) {
    val mBottomSheetBehavior: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(bottom_sheet)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        button_1.setOnClickListener(View.OnClickListener {
            Log.d("1`111111", "534254535454")
            mBottomSheetBehavior.isFitToContents = false
            mBottomSheetBehavior.state =
                BottomSheetBehavior.STATE_HALF_EXPANDED
        })
//        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        mBottomSheetBehavior.peekHeight = 200
        mBottomSheetBehavior.isHideable = true
        mBottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                Log.d("1`111111", "newState = $newState")
//                Log.d("1`111111", "${bottomSheet.height}")
//                Log.d("1`111111", "${mBottomSheetBehavior.peekHeight}")
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        val colorR = (0..255).random()
                        val colorG = (0..255).random()
                        val colorB = (0..255).random()
                        bottomSheet.setBackgroundColor(
                            Color.argb(
                                255,
                                colorR,
                                colorG,
                                colorB
                            )
                        )
                    }
                    else -> {}
                }

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("BottomSheetBehavior", "slideOffset = $slideOffset")
            }
        })
    }


}
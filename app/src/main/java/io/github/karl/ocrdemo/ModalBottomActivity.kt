package io.github.karl.ocrdemo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.ocr_result_layout.*

class ModalBottomActivity : AppCompatActivity(R.layout.ocr_result_layout) {
    val modalBottomSheet = ModalBottomSheet()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modalBottomSheet.show(supportFragmentManager, ModalBottomSheet.TAG)

//        val bsd = BottomSheetDialog(this)
//        bsd.setContentView(R.layout.modal_buttom_sheet_content)
//        bsd.behavior.peekHeight = 200
//        bsd.show()
//        bsd.show()
        button_1.setOnClickListener {
            Log.d("1`111111", "534254535454")
            modalBottomSheet.show(supportFragmentManager,
                ModalBottomSheet.TAG
            )
//            bsd.show()
//            bsd.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }
}

class ModalBottomSheet : BottomSheetDialogFragment() {
    lateinit var behavior: BottomSheetBehavior<FrameLayout>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.modal_buttom_sheet_content,
        container,
        false
    )

    override fun onStart() {
        super.onStart()
        val bottomSheet: FrameLayout =
            dialog?.findViewById(R.id.design_bottom_sheet) ?: return
        behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.peekHeight = 400
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
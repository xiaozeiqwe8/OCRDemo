package io.github.karl.ocrdemo

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.toColor
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import id.zelory.compressor.Compressor
import id.zelory.compressor.loadBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(R.layout.activity_main) {


    private lateinit var client: OkHttpClient
    private var latestTmpUri: Uri? = null
    private var takeImageTempFile: File? = null
    private lateinit var behavior: BottomSheetBehavior<View>

    private val displayWidth: Int by lazy {
        resources.displayMetrics.widthPixels
//        1050
    }

    private val displayHeight: Int by lazy {
        resources.displayMetrics.heightPixels - behavior.peekHeight
//        1680
    }

    private val adapter =
        OcrListAdapter(OcrItemListener { position, item ->
            image_preview.selectOcrBox(ocrItem = item)
        })

    private val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val compressedImageFile = Compressor.compress(
                            applicationContext,
                            takeImageTempFile!!
                        )
                        image_preview.clearOcrBoxes()
                        adapter.submitList(mutableListOf())
                        previewPhoto(compressedImageFile)
                        postImage(compressedImageFile)
                    }
                }
            }
        }

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = createTempFile()
                val fos = tempFile.outputStream()
                val buffer = ByteArray(8 * 1024)
                var byteCount: Int
                fos.use {
                    while (inputStream?.read(buffer)
                            .also { byteCount = it!! } != -1
                    ) {
                        it.write(buffer, 0, byteCount)
                    }
                }
                inputStream?.close()

                CoroutineScope(Dispatchers.IO).launch {
                    val compressedImageFile =
                        Compressor.compress(applicationContext, tempFile)
                    image_preview.clearOcrBoxes()
                    adapter.submitList(mutableListOf())
                    previewPhoto(compressedImageFile)
                    postImage(compressedImageFile)
                }
            }
        }

//    fun cvtColor() {
//        val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.a123)
//        val src = Mat()
//        val dst = Mat()
//        Utils.bitmapToMat(bitmap, src)
//        //转换为灰度图模式
//        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY)
//
//        //把mat转换回bitmap
//        Utils.matToBitmap(dst,bitmap)
//        image_preview.setImageBitmap(bitmap)
//        src.release()
//        dst.release()
//    }
//
//    private val mLoaderCallback = object : BaseLoaderCallback(this) {
//        override fun onManagerConnected(status: Int) {
//            when(status){
//                LoaderCallbackInterface.SUCCESS -> {
//                    Log.i(
//                        "MainActivity",
//                        "OpenCV loaded successfully8888888"
//                    )
//                    cvtColor()
//                }
//                else -> {
//                    super.onManagerConnected(status)
//                }
//            }
//        }
//    }

    override fun onResume() {
        super.onResume()
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(
//                "MainActivity",
//                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
//            )
//            OpenCVLoader.initAsync(
//                OpenCVLoader.OPENCV_VERSION_3_0_0,
//                this,
//                mLoaderCallback
//            )
//        } else {
//            Log.d(
//                "MainActivity",
//                "OpenCV library found inside package. Using it!"
//            )
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindListeners()
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        recycle_view.adapter = adapter
        recycle_view.layoutManager = LinearLayoutManager(this)
        recycle_view.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )

        behavior =
            BottomSheetBehavior.from(findViewById(R.id.custom_bottom_sheet))
        behavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val bottomSheetHeightOffset =
                    bottomSheet.height * slideOffset
                val layoutParams = image_preview.layoutParams
                val width = displayWidth
                val height = displayHeight
                layoutParams.height =
                    (height - bottomSheetHeightOffset).toInt()
                image_preview.layoutParams = layoutParams
            }
        })

        image_preview.setOcrImageListener(OcrImageListener { position, item ->
            adapter.linkageClick(position)
        })

//        Toast.makeText(
//            this,
//            "displayWidth = $displayWidth, displayHeight=$displayHeight",
//            Toast.LENGTH_SHORT
//        ).show()

//        val ocrItem = OcrItem(
//            text = "dasda",
//            color = Color.argb(255, 210, 211, 212).toColor(),
//            score = 10.1f
//        )
//        val ocrItems = mutableListOf(
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,ocrItem,
//            ocrItem
//        )
//        runOnUiThread {
//            ocrResultList.clear()
//            ocrResultList.addAll(ocrItems)
//            adapter.notifyDataSetChanged()
//        }

    }

    private fun bindListeners() {
        open_take_pic.setOnClickListener { takeImage() }
        open_select_image.setOnClickListener { selectImageFromGallery() }
    }

    private fun takeImage() {
        takeImageTempFile = createTempFile()
        if (takeImageTempFile == null) {
            return
        }
        lifecycleScope.launchWhenStarted {
            getUriFromFile(takeImageTempFile!!).let { uri ->
                latestTmpUri = uri
                takeImageResult.launch(uri)
            }
        }
    }

    private fun selectImageFromGallery() =
        selectImageFromGalleryResult.launch("image/*")

    private fun createTempFile() =
        File.createTempFile("tmp_image_file", ".png", filesDir).apply {
            createNewFile()
            deleteOnExit()
        }

    private fun getUriFromFile(file: File): Uri {
        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            file
        )
    }

    private fun postImage(file: File) {

//        val inputStream = contentResolver.openInputStream(uri)
//        val picData = inputStream?.source()?.buffer()?.readByteArray()
//        val bodyData = "image_data=${String(Base64.encode(picData, Base64.URL_SAFE))}"

//        val picData = inputStream?.source()?.buffer()?.readByteString()
//        val bodyData = "image_data=${picData!!.base64Url()}"

        val picData = file.source().buffer().readByteString()
        val bodyData = "image_data=${
            URLEncoder.encode(
                picData.base64(),
                Charsets.UTF_8.name()
            )
        }"

        val requestBody =
            bodyData.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
//            .url("http://10.0.10.38:5000/ocr")
//            .url("http://10.0.10.2:8080/ocr")
            .url("http://10.0.200.20:8080/ocr")
            .post(requestBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("postImage", "onFailure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    Log.d("postImage", "onResponse: $bodyString")
                    val ocrResult =
                        Gson().fromJson(bodyString, OcrResult::class.java)
                    if (ocrResult.errId != 0) {
                        Log.d(
                            "postImage",
                            "onResponse error: errorId=${ocrResult.errId}, errorMsg = ${ocrResult.errMsg}"
                        )
                    } else {
                        val ocrItems = convertOcrItems(ocrResult.result)
                        drawOcrBoxes(file, ocrItems)
                        adapter.submitList(ocrItems)
                    }
                } else {
                    Log.d(
                        "postImage",
                        "onResponse failure: " + response.body?.string()
                    )
                }
            }
        })
    }

    private fun convertOcrItems(result: List<OcrResult.Result>): List<OcrItem> {
        val ocrItems = mutableListOf<OcrItem>()
        result.forEach {
            val colorR = (0..255).random()
            val colorG = (0..255).random()
            val colorB = (0..255).random()
            val ocrItem = OcrItem(
                boxes = it.boxes,
                text = it.text,
                score = it.score,
                color = Color.argb(
                    255,
                    colorR,
                    colorG,
                    colorB
                ).toColor()
            )
            ocrItems.add(ocrItem)
        }
        return ocrItems
    }

    private fun previewPhoto(file: File) {
        runOnUiThread {
            Glide.with(applicationContext)
                .asBitmap()
                //该分辨率在glide内部会有强制的比例限制，使得图片不一定按照此值拉伸
                .override(displayWidth, displayHeight)//960,1280
                .load(file)
                .into(image_preview)
        }
    }

    private fun drawOcrBoxes(file: File, ocrItems: List<OcrItem>) {
        val bitmap = loadBitmap(file)
        image_preview.drawOcrBoxes(
            ocrItems,
            bitmap.width,
            bitmap.height
        )
    }

}


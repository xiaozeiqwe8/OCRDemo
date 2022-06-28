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
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import android.app.Activity




class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val mockResponseJsonStr = """
        {
            "errId": 0,
            "errMsg": "",
            "result": [{
                "boxes": [[71, 1798],[264, 1806],[262, 1854],[69, 1846]],
                "text": "Authority",
                "score": 0.9999019
            }, {
                "boxes": [[75, 1742],[716, 1748],[714, 1794],[73, 1788]],
                "text": "Government Root Certification",
                "score": 0.9929104
            }, {
                "boxes": [[77, 1552],[714, 1552],[714, 1586],[77, 1586]],
                "text": "Go Daddy Root Certificate Authority-G2",
                "score": 0.99476784
            }, {
                "boxes": [[79, 1484],[470, 1490],[468, 1536],[77, 1530]],
                "text": "saacammonne",
                "score": 0.55913043
            }, {
                "boxes": [[71, 1284],[387, 1288],[385, 1328],[69, 1324]],
                "text": "GlobalSign Root CA",
                "score": 0.9982361
            }, {
                "boxes": [[73, 1222],[432, 1230],[430, 1282],[71, 1274]],
                "text": "GlobalSign nv-sa",
                "score": 0.9990079
            }, {
                "boxes": [[73, 1026],[250, 1034],[248, 1074],[71, 1066]],
                "text": "GlobalSign",
                "score": 0.99623775
            }, {
                "boxes": [[73, 966],[301, 974],[299, 1022],[71, 1014]],
                "text": "GlobalSign",
                "score": 0.98376197
            }, {
                "boxes": [[73, 768],[250, 776],[248, 816],[71, 808]],
                "text": "Gamasaspe",
                "score": 0.59074664
            }, {
                "boxes": [[73, 704],[305, 714],[303, 768],[71, 758]],
                "text": "GlobalSign",
                "score": 0.9983882
            }, {
                "boxes": [[71, 510],[252, 518],[250, 558],[69, 550]],
                "text": "GlobalSign",
                "score": 0.99850523
            }, {
                "boxes": [[73, 444],[305, 454],[303, 508],[71, 498]],
                "text": "swdsasignGd",
                "score": 0.6600375
            }, {
                "boxes": [[665, 232],[752, 232],[752, 286],[665, 286]],
                "text": "电电电",
                "score": 0.68177694
            }, {
                "boxes": [[329, 230],[418, 230],[418, 284],[329, 284]],
                "text": "杂东景",
                "score": 0.28567907
            }, {
                "boxes": [[73, 122],[450, 122],[450, 176],[73, 176]],
                "text": "个一信任的证书",
                "score": 0.9976823
            }, {
                "boxes": [[889, 32],[1038, 28],[1040, 76],[891, 80]],
                "text": "物电区A",
                "score": 0.0875141
            }, {
                "boxes": [[41, 34],[297, 28],[299, 74],[43, 80]],
                "text": "17:030Ri08",
                "score": 0.73744255
            }]
        }
    """.trimIndent()

    private lateinit var client: OkHttpClient
    private lateinit var mockWebServer: MockWebServer
    private var latestTmpUri: Uri? = null
    private var takeImageTempFile: File? = null
    private lateinit var behavior: BottomSheetBehavior<View>

    private val displayWidth: Int by lazy {
        resources.displayMetrics.widthPixels
//        1050
    }

    //获取状态栏的高度
    private fun getStatusBarHeight(activity: Activity): Int {
        val resourceId = activity.resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
        return if (resourceId > 0) {
            activity.resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private val displayHeight: Int by lazy {
        resources.displayMetrics.heightPixels - behavior.peekHeight - getStatusBarHeight(this)
//        1680
    }

    private val adapter =
        OcrListAdapter(OcrItemListener { _, item ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindListeners()
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        mockWebServer = MockWebServer()

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
                    (bottomSheet.height - behavior.peekHeight) * slideOffset
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
        val picData = file.source().buffer().readByteString()
        val bodyData = "image_data=${
            URLEncoder.encode(
                picData.base64(),
                Charsets.UTF_8.name()
            )
        }"

        mockWebServer.enqueue(
            MockResponse().setBody(mockResponseJsonStr)
        )
        val mockUrl = mockWebServer.url("/ocr")

        val url = "http://${mockUrl.host}:${mockWebServer.port}"
//        val url = "http://10.0.200.20:8080/ocr"

        val requestBody =
            bodyData.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(url)
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


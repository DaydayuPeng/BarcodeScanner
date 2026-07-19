package com.tugulu.scanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tugulu.scanner.R
import com.tugulu.scanner.TuguluApp
import com.tugulu.scanner.data.ApiException
import com.tugulu.scanner.data.ScanRecord
import com.tugulu.scanner.databinding.ActivityScannerBinding
import com.tugulu.scanner.scan.ZxingBarcodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private val app get() = TuguluApp.instance

    private lateinit var adapter: RecordAdapter
    private val records = mutableListOf<ScanRecord>()
    private var nextId = 1L

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null
    private val zxingHelper = ZxingBarcodeHelper()
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var analyzingPaused = AtomicBoolean(false)
    private val recentCodes = HashMap<String, Long>()
    private var latestFrameBytes: ByteArray? = null
    private var latestFrameWidth = 0
    private var latestFrameHeight = 0
    private var latestFrameRotation = 0
    private val frameLock = Any()
    private var statusHideAt = 0L
    private var tone: ToneGenerator? = null
    private var lastAutoFocusAt = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var photoExecutor: ExecutorService? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showStatus(getString(R.string.camera_permission_required), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!app.session.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecordAdapter { deleteRecord(it) }
        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.adapter = adapter
        refreshList()

        binding.btnLogout.setOnClickListener {
            app.session.clearAuth()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.btnAdd.setOnClickListener { addManual() }
        binding.etManual.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addManual()
                true
            } else false
        }
        binding.etManual.setOnFocusChangeListener { _, hasFocus ->
            analyzingPaused.set(hasFocus)
        }
        binding.btnSwitchCamera.setOnClickListener { switchCamera() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnSubmit.setOnClickListener { submitInbound() }
        binding.ivLastPhoto.setOnClickListener {
            records.lastOrNull()?.let { deleteRecord(it) }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        photoExecutor = Executors.newSingleThreadExecutor()
        // 仅快递一维码（对齐 floatscan / scanner.html）
        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR
                )
                .build()
        )
        tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCameraUseCases()
                showStatus("扫码已就绪（一维码）")
            } catch (e: Exception) {
                showStatus("摄像头启动失败：${e.message}", true)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        // 恢复上次镜头朝向（back/front）
        val saved = app.session.preferredCameraId
        if (saved == CameraSelector.LENS_FACING_FRONT.toString()) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        } else if (saved == CameraSelector.LENS_FACING_BACK.toString()) {
            lensFacing = CameraSelector.LENS_FACING_BACK
        }

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

        // 更高分析分辨率，利于细条码 / 远距离识别（对齐 floatscan TRY_HARDER 思路）
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
            analyzeFrame(imageProxy)
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            camera = provider.bindToLifecycle(this, selector, preview, analysis)
            requestCenterAutoFocus()
            // 有前后摄时显示切换按钮
            val hasBack = provider.hasCamera(
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            )
            val hasFront = provider.hasCamera(
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            )
            binding.btnSwitchCamera.visibility =
                if (hasBack && hasFront) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            showStatus("绑定摄像头失败：${e.message}", true)
        }
    }

    /** 周期性中心对焦，提升条码清晰度 */
    private fun requestCenterAutoFocus() {
        val cam = camera ?: return
        try {
            val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
            val point = factory.createPoint(0.5f, 0.5f)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
            lastAutoFocusAt = SystemClock.elapsedRealtime()
        } catch (_: Exception) {
            // 部分机型不支持对焦点，忽略
        }
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        app.session.preferredCameraId = lensFacing.toString()
        bindCameraUseCases()
        showStatus(
            if (lensFacing == CameraSelector.LENS_FACING_BACK) "已切换到后置摄像头"
            else "已切换到前置摄像头"
        )
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (analyzingPaused.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // 约每 2.5s 触发一次中心对焦，避免远距条码糊掉
        val now = SystemClock.elapsedRealtime()
        if (now - lastAutoFocusAt > 2500) {
            runOnUiThread { requestCenterAutoFocus() }
        }

        var nv21: ByteArray? = null
        try {
            nv21 = yuv420888ToNv21(imageProxy)
            synchronized(frameLock) {
                latestFrameBytes = nv21
                latestFrameWidth = imageProxy.width
                latestFrameHeight = imageProxy.height
                latestFrameRotation = imageProxy.imageInfo.rotationDegrees
            }
        } catch (_: Exception) {
            // ignore frame cache failure
        }

        // 优先 ZXing（floatscan 同款 TRY_HARDER / 多区域），命中则跳过本帧 ML Kit
        if (nv21 != null) {
            try {
                val zxingText = zxingHelper.decodeNv21(nv21, imageProxy.width, imageProxy.height)
                if (!zxingText.isNullOrBlank()) {
                    onDecoded(zxingText)
                    imageProxy.close()
                    return
                }
            } catch (_: Exception) {
                // ZXing 失败继续走 ML Kit
            }
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = barcodeScanner
        if (scanner == null) {
            imageProxy.close()
            return
        }
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (b in barcodes) {
                    val raw = b.rawValue ?: continue
                    onDecoded(raw)
                    break
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun onDecoded(raw: String) {
        val code = extractTrackingNo(raw)
        if (code.isEmpty()) return
        if (isDuplicate(code)) return
        // 扫码优先：先上主线程落单号，截图后台补
        runOnUiThread { addRecord(code, deferPhoto = true) }
    }

    private fun extractTrackingNo(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        if (t.matches(Regex("^[A-Za-z0-9\\-_]+$"))) return t.uppercase()
        val runs = Regex("[A-Za-z0-9]{6,}").findAll(t).map { it.value }.toList()
        if (runs.isNotEmpty()) return runs.maxBy { it.length }.uppercase()
        return t
    }

    private fun isDuplicate(code: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val it = recentCodes.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > 2000) it.remove()
        }
        val prev = recentCodes[code]
        recentCodes[code] = now
        return prev != null && now - prev < 2000
    }

    private fun addManual() {
        val value = binding.etManual.text?.toString()?.trim().orEmpty()
        if (value.isEmpty()) {
            showStatus(getString(R.string.please_input_tracking), true)
            return
        }
        binding.etManual.setText("")
        binding.etManual.clearFocus()
        analyzingPaused.set(false)
        addRecord(value, deferPhoto = true)
    }

    private fun addRecord(tracking: String, deferPhoto: Boolean) {
        val rec = ScanRecord(nextId++, tracking, null)
        records.add(rec)
        refreshList()
        showStatus("已添加：$tracking")
        beepAndVibrate()
        if (deferPhoto) schedulePhotoCapture(rec.id)
    }

    /** 空闲时补截图，避免和连续扫码抢同一帧处理时间 */
    private fun schedulePhotoCapture(recordId: Long) {
        val executor = photoExecutor ?: return
        mainHandler.postDelayed({
            executor.execute {
                val path = capturePhotoFile() ?: return@execute
                mainHandler.post {
                    val rec = records.find { it.id == recordId } ?: run {
                        File(path).delete()
                        return@post
                    }
                    rec.photoPath = path
                    adapter.notifyPhotoUpdated(recordId)
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        binding.ivLastPhoto.setImageBitmap(bmp)
                        binding.ivLastPhoto.visibility = View.VISIBLE
                    }
                }
            }
        }, 60)
    }

    private fun deleteRecord(item: ScanRecord) {
        records.removeAll { it.id == item.id }
        item.photoPath?.let { File(it).delete() }
        refreshList()
        if (records.isEmpty()) {
            binding.ivLastPhoto.visibility = View.GONE
        }
        showStatus("已删除", true)
    }

    private fun clearAll() {
        if (records.isEmpty()) return
        AlertDialog.Builder(this)
            .setMessage("确定清空所有扫描记录吗？")
            .setPositiveButton("清空") { _, _ ->
                records.forEach { it.photoPath?.let { p -> File(p).delete() } }
                records.clear()
                binding.ivLastPhoto.visibility = View.GONE
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun refreshList() {
        adapter.submit(records.asReversed())
        binding.tvCount.text = getString(R.string.record_count, records.size)
    }

    private fun submitInbound() {
        if (records.isEmpty()) {
            Toast.makeText(this, "当前没有扫描记录，无需入库。", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage("确认入库 ${records.size} 件快递吗？")
            .setPositiveButton("确认") { _, _ -> doSubmit() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doSubmit() {
        binding.btnSubmit.isEnabled = false
        showStatus("正在入库...")
        lifecycleScope.launch {
            try {
                val nos = records.map { it.trackingNo }
                val photo = records.mapNotNull { it.photoPath }.lastOrNull()?.let { File(it) }
                val imageUrl = withContext(Dispatchers.IO) {
                    if (photo != null && photo.exists()) {
                        try {
                            app.api.uploadImage(photo)
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }
                val result = withContext(Dispatchers.IO) {
                    app.api.inboundScan(nos, imageUrl)
                }
                val failMsg = if (result.failList.isNotEmpty()) {
                    "\n失败 ${result.failList.size} 件：" +
                        result.failList.joinToString("；") { "${it.trackingNo}:${it.reason}" }
                } else ""
                AlertDialog.Builder(this@ScannerActivity)
                    .setTitle("入库完成")
                    .setMessage("成功 ${result.successCount} 件$failMsg")
                    .setPositiveButton("确定", null)
                    .show()
                if (result.successCount > 0) {
                    records.forEach { it.photoPath?.let { p -> File(p).delete() } }
                    records.clear()
                    binding.ivLastPhoto.visibility = View.GONE
                    refreshList()
                    beepAndVibrate()
                }
            } catch (e: ApiException) {
                if (e.code == 401) {
                    Toast.makeText(this@ScannerActivity, "登录已失效，请重新登录", Toast.LENGTH_LONG).show()
                    app.session.clearAuth()
                    startActivity(Intent(this@ScannerActivity, LoginActivity::class.java))
                    finish()
                } else {
                    showStatus(e.message ?: "入库失败", true)
                    Toast.makeText(this@ScannerActivity, e.message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showStatus(e.message ?: "网络异常", true)
                Toast.makeText(this@ScannerActivity, e.message, Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun capturePhotoFile(): String? {
        val (bytes, w, h, rotation) = synchronized(frameLock) {
            val b = latestFrameBytes ?: return null
            Quad(b.copyOf(), latestFrameWidth, latestFrameHeight, latestFrameRotation)
        }
        return try {
            val yuv = YuvImage(bytes, ImageFormat.NV21, w, h, null)
            val baos = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, w, h), 80, baos)
            var bmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size()) ?: return null
            if (rotation != 0) {
                val m = Matrix().apply { postRotate(rotation.toFloat()) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            // 缩略图即可（预览频次低），减小编码耗时
            val maxW = 720
            if (bmp.width > maxW) {
                val scale = maxW.toFloat() / bmp.width
                bmp = Bitmap.createScaledBitmap(
                    bmp,
                    maxW,
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
            val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun beepAndVibrate() {
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {
        }
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        } catch (_: Exception) {
        }
    }

    private fun showStatus(text: String, error: Boolean = false) {
        binding.tvStatus.text = text
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(this, if (error) R.color.danger else R.color.text)
        )
        binding.tvStatus.visibility = View.VISIBLE
        statusHideAt = SystemClock.elapsedRealtime() + 1800
        binding.tvStatus.postDelayed({
            if (SystemClock.elapsedRealtime() >= statusHideAt) {
                binding.tvStatus.visibility = View.GONE
            }
        }, 1850)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        // NV21 = Y + VU interleaved; for many devices V plane comes first then U
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        cameraExecutor?.shutdown()
        photoExecutor?.shutdown()
        barcodeScanner?.close()
        tone?.release()
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

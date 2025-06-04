package com.example.cameraxapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.cameraxapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var analyzeNextFrame = false

    private lateinit var cameraExecutor: ExecutorService

    private external fun stringFromJNI(): String
    private external fun analyzeFrameNative(data: ByteArray, width: Int, height: Int): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        //viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.detectLinesButton.setOnClickListener {
            analyzeNextFrame = true
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { preview ->
                preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!analyzeNextFrame) {
                            image.close()
                            return@setAnalyzer
                        }
                        analyzeNextFrame = false

                        val cropRect = image.cropRect
                        var rotationDegrees_ma = image.imageInfo.rotationDegrees
                        val rotated = (rotationDegrees_ma == 90 || rotationDegrees_ma == 270)

                        // Inform the overlay view
                        viewBinding.overlayView.setTransformInfo(
                            cropLeft = cropRect.left,
                            cropTop = cropRect.top,
                            cropWidth = cropRect.width(),
                            cropHeight = cropRect.height(),
                            bufferWidth = image.width,
                            bufferHeight = image.height,
                            rotationDegrees = rotationDegrees_ma
                        )

                        // Calculate scale
                        val adjustedCropWidth = if (rotated) cropRect.height() else cropRect.width()
                        val adjustedCropHeight = if (rotated) cropRect.width() else cropRect.height()

                        val scaleX = viewBinding.overlayView.width.toFloat() / adjustedCropWidth
                        Log.d("setAnalyzer", "overlayView.width, {$viewBinding.overlayView.width}")
                        Log.d("setAnalyzer", "scalex, {$scaleX")
                        val scaleY = viewBinding.overlayView.height.toFloat() / adjustedCropHeight
                        Log.d("setAnalyzer", "overlayView.height, {$viewBinding.overlayView.height}")
                        Log.d("setAnalyzer", "scaleY, {$scaleY}")
                        val scale = minOf(scaleX, scaleY)

                        Log.d("setAnalyzer", "scaleX=$scaleX, scaleY=$scaleY, scale=$scale")

                        // Extract bytes
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        // Run analysis
                        runOnUiThread {
                            val resultString = analyzeFrameNative(bytes, image.width, image.height)
                            val lines = resultString.split(";")
                                .filter { it.contains(",") }
                                .mapNotNull { segment ->
                                    val parts = segment.split(",")
                                    val rho = parts.getOrNull(0)?.toFloatOrNull()
                                    val theta = parts.getOrNull(1)?.toFloatOrNull()
                                    if (rho != null && theta != null) rho to theta else null
                                }

                            viewBinding.overlayView.setLines(lines, image.width, image.height)
                            viewBinding.analysisOverlay.text = "Detected ${lines.size} lines"
                        }
                        image.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector,
                    preview, imageCapture, imageAnalysis, videoCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    companion object {
        init {
            System.loadLibrary("native-lib")
        }

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

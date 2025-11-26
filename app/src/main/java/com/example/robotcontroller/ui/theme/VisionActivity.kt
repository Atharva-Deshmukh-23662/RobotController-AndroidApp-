package com.example.robotcontroller.ui.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.example.robotcontroller.R
import com.example.robotcontroller.camera.CameraManager
import com.example.robotcontroller.vision.DetectedObject
import com.example.robotcontroller.vision.ObjectDetector
import com.example.robotcontroller.utils.CameraPermissionHelper

class VisionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: DrawingOverlay
    private lateinit var cameraManager: CameraManager
    private lateinit var objectDetector: ObjectDetector
    private lateinit var infoTextView: TextView

    private var lastDetections: List<DetectedObject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vision)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        infoTextView = findViewById(R.id.infoTextView)

        if (CameraPermissionHelper.hasCameraPermission(this)) {
            setupVision()
        } else {
            CameraPermissionHelper.requestCameraPermission(
                this,
                onPermissionGranted = { setupVision() },
                onPermissionDenied = { finish() }
            )
        }
    }

    private fun setupVision() {
        cameraManager = CameraManager(this, this)
        objectDetector = ObjectDetector(this)

        // Add detection listener
        objectDetector.addDetectionListener { detections ->
            lastDetections = detections
            overlayView.setDetections(detections)
            updateInfoText(detections)
        }

        // Start camera with detection
        cameraManager.startCamera(previewView) { imageProxy ->
            val detections = objectDetector.detectObjects(imageProxy)
            lastDetections = detections
        }
    }

    private fun updateInfoText(detections: List<DetectedObject>) {
        runOnUiThread {
            val text = buildString {
                append("Objects Detected: ${detections.size}\n")
                detections.take(3).forEach { obj ->
                    append("${obj.label}: ${String.format("%.2f", obj.confidence * 100)}%\n")
                }
            }
            infoTextView.text = text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        objectDetector.close()
        cameraManager.stopCamera()
    }
}

class DrawingOverlay(context: android.content.Context, attrs: android.util.AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var detections: List<DetectedObject> = emptyList()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 30f
    }

    init {
        holder.addCallback(this)
    }

    fun setDetections(detections: List<DetectedObject>) {
        this.detections = detections
        draw()
    }

    private fun draw() {
        val canvas: Canvas?
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

                detections.forEach { obj ->
                    val rect = Rect(
                        obj.boundingBox.left.toInt(),
                        obj.boundingBox.top.toInt(),
                        obj.boundingBox.right.toInt(),
                        obj.boundingBox.bottom.toInt()
                    )
                    canvas.drawRect(rect, paint)
                    canvas.drawText(
                        "${obj.label} ${String.format("%.0f", obj.confidence * 100)}%",
                        rect.left.toFloat(),
                        (rect.top - 10).toFloat(),
                        textPaint
                    )
                }

                holder.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}
}

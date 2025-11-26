package com.example.robotcontroller.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.min

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val trackingId: Int = -1
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val width: Float = right - left,
    val height: Float = bottom - top,
    val centerX: Float = left + width / 2,
    val centerY: Float = top + height / 2
)

class ObjectDetector(context: Context) {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) // Real-time streaming
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val detectionListeners = mutableListOf<(List<DetectedObject>) -> Unit>()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun detectObjects(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        try {
            val task = detector.process(inputImage)

            task.addOnSuccessListener { detections ->
                val results = mutableListOf<DetectedObject>()

                for (detection in detections) {
                    val bounds = detection.boundingBox
                    val labelObj = detection.labels.firstOrNull()

                    val label = labelObj?.text ?: "Unknown"
                    val confidence = labelObj?.confidence ?: 0.5f

                    val detectedObject = DetectedObject(
                        label = label,
                        confidence = confidence,
                        boundingBox = BoundingBox(
                            left = bounds.left.toFloat(),
                            top = bounds.top.toFloat(),
                            right = bounds.right.toFloat(),
                            bottom = bounds.bottom.toFloat()
                        ),
                        trackingId = detection.trackingId ?: -1
                    )

                    results.add(detectedObject)
                }

                notifyListeners(results)
                imageProxy.close()
            }.addOnFailureListener { e ->
                Log.e("ObjectDetector", "Detection failed", e)
                imageProxy.close()
            }
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Detection failed", e)
            imageProxy.close()
        }
    }
    fun addDetectionListener(listener: (List<DetectedObject>) -> Unit) {
        detectionListeners.add(listener)
    }

    fun removeDetectionListener(listener: (List<DetectedObject>) -> Unit) {
        detectionListeners.remove(listener)
    }

    private fun notifyListeners(detections: List<DetectedObject>) {
        detectionListeners.forEach { it(detections) }
    }

    fun close() {
        detector.close()
    }
}

package com.example.robotcontroller.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(private val context: Context) {

    // You can expose the controller if you need to bind it to a LifecycleOwner from outside
    val controller: LifecycleCameraController = LifecycleCameraController(context)

    // This function already correctly uses the cameraController
    fun captureImage(onImageCaptured: (Bitmap?) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = image.toBitmap()
                    image.close()
                    onImageCaptured(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    // It's good practice to handle the error case
                    onImageCaptured(null)
                }
            }
        )
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        // This conversion logic seems fine, but be aware of different image formats (YUV_420_888 is common)
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            width,
            height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // Corrected suspend function
    suspend fun takePicture(): Bitmap {
        // Use the 'controller' property which is a LifecycleCameraController
        return suspendCancellableCoroutine { continuation ->
            controller.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        try {
                            // Using your existing toBitmap() conversion
                            val bitmap = imageProxy.toBitmap()
                            imageProxy.close()
                            continuation.resume(bitmap)
                        } catch (e: Exception) {
                            imageProxy.close()
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}

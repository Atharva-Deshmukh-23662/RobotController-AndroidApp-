package com.example.robotcontroller.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class CameraManager(private val context: Context) {

    private var cameraController: LifecycleCameraController = LifecycleCameraController(context)

    fun captureImage(onImageCaptured: (Bitmap?) -> Unit) {
        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = image.toBitmap()
                    image.close()
                    onImageCaptured(bitmap)
                }
            }
        )
    }
}
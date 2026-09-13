package com.example.attendanceapp.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraCaptureView(
    modifier: Modifier = Modifier,
    initialLensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    canSwitchCamera: Boolean = true,
    guideText: String = "Position face inside the oval",
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(initialLensFacing) }
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
            } catch (e: Exception) {
                Log.e("CameraCaptureView", "Use case binding failed", e)
                onError("Failed to initialize camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Face framing oval overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val ovalWidth = canvasWidth * 0.70f
            val ovalHeight = ovalWidth * 1.35f

            val left = (canvasWidth - ovalWidth) / 2f
            val top = (canvasHeight - ovalHeight) / 2.5f

            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Guide text
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp, start = 20.dp, end = 20.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape
        ) {
            Text(
                text = guideText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canSwitchCamera) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }

            // Shutter Button
            IconButton(
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    capture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                try {
                                    val bitmap = imageProxyToBitmap(imageProxy, lensFacing)
                                    ContextCompat.getMainExecutor(context).execute {
                                        onImageCaptured(bitmap)
                                    }
                                } catch (e: Exception) {
                                    Log.e("CameraCaptureView", "Error processing captured photo", e)
                                    ContextCompat.getMainExecutor(context).execute {
                                        onError("Failed to process photo: ${e.message}")
                                    }
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraCaptureView", "Photo capture failed", exception)
                                ContextCompat.getMainExecutor(context).execute {
                                    onError("Failed to take photo: ${exception.message}")
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .background(Color.White, CircleShape)
            ) {}

            // Placeholder to keep shutter centered
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy, lensFacing: Int): Bitmap {
    val rawBitmap = imageProxy.toBitmap()
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees

    val matrix = Matrix().apply {
        if (rotationDegrees != 0) {
            postRotate(rotationDegrees.toFloat())
        }
        // Mirror if front camera so it looks natural
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
        }
    }

    return Bitmap.createBitmap(
        rawBitmap,
        0,
        0,
        rawBitmap.width,
        rawBitmap.height,
        matrix,
        true
    )
}

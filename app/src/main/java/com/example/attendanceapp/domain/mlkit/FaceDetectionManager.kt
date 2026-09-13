package com.example.attendanceapp.domain.mlkit

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

interface FaceDetectionManager {
    suspend fun detectFaces(bitmap: Bitmap): List<Face>
    fun close()
}

class FaceDetectionManagerMLKitImpl: FaceDetectionManager {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    override suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (continuation.isActive) {
                        continuation.resume(faces)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        }
    }

    override fun close() {
        detector.close()
    }

    companion object {
        const val TAG = "FaceDetectionManagerMLKitImpl"
    }
}
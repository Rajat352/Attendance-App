package com.example.attendanceapp.domain.mlkit

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FaceDetectionManager {
    suspend fun detectFaces(bitmap: Bitmap)
}

class FaceDetectionManagerMLKitImpl: FaceDetectionManager {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    override suspend fun detectFaces(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "face detected: $faces")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "An error occurred while detecting face", e)
                }
        }
    }

    companion object {
        const val TAG = "FaceDetectionManagerMLKitImpl"
    }
}
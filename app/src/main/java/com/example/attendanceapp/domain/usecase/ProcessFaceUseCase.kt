package com.example.attendanceapp.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.attendanceapp.domain.facenet.FaceRecognitionManager
import com.example.attendanceapp.domain.mlkit.FaceDetectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProcessedFace(
    val fullBitmap: Bitmap,
    val croppedFaceBitmap: Bitmap,
    val embedding: List<Float>
)

sealed class FaceProcessingException(message: String) : Exception(message) {
    data object NoFaceDetected : FaceProcessingException("No face detected. Please position your face clearly inside the frame.")
    data class MultipleFacesDetected(val count: Int) : FaceProcessingException("Multiple faces ($count) detected. Please ensure only one person is in the frame.")
}

class ProcessFaceUseCase @Inject constructor(
    private val faceDetectionManager: FaceDetectionManager,
    private val faceRecognitionManager: FaceRecognitionManager
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<ProcessedFace> = withContext(Dispatchers.Default) {
        try {
            val faces = faceDetectionManager.detectFaces(bitmap)
            when {
                faces.isEmpty() -> {
                    Result.failure(FaceProcessingException.NoFaceDetected)
                }
                faces.size > 1 -> {
                    Result.failure(FaceProcessingException.MultipleFacesDetected(faces.size))
                }
                else -> {
                    val face = faces.first()
                    val cropped = cropFace(bitmap, face.boundingBox)
                    val embedding = faceRecognitionManager.extractEmbedding(cropped)
                    Result.success(
                        ProcessedFace(
                            fullBitmap = bitmap,
                            croppedFaceBitmap = cropped,
                            embedding = embedding
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while processing face", e)
            Result.failure(e)
        }
    }

    private fun cropFace(rawBitmap: Bitmap, boundingBox: Rect): Bitmap {
        val widthMargin = (boundingBox.width() * 0.15f).toInt()
        val heightMargin = (boundingBox.height() * 0.15f).toInt()

        val left = (boundingBox.left - widthMargin).coerceIn(0, rawBitmap.width - 1)
        val top = (boundingBox.top - heightMargin).coerceIn(0, rawBitmap.height - 1)
        val right = (boundingBox.right + widthMargin).coerceIn(left + 1, rawBitmap.width)
        val bottom = (boundingBox.bottom + heightMargin).coerceIn(top + 1, rawBitmap.height)

        return Bitmap.createBitmap(rawBitmap, left, top, right - left, bottom - top)
    }

    companion object {
        private const val TAG = "ProcessFaceUseCase"
    }
}

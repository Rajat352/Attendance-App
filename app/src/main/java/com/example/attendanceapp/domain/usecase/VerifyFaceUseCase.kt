package com.example.attendanceapp.domain.usecase

import com.example.attendanceapp.domain.facenet.FaceRecognitionManager
import com.example.attendanceapp.domain.facenet.Models
import javax.inject.Inject

data class VerificationResult(
    val isMatch: Boolean,
    val similarityScore: Float,
    val threshold: Float
)

class VerifyFaceUseCase @Inject constructor(
    private val faceRecognitionManager: FaceRecognitionManager
) {
    operator fun invoke(
        capturedEmbedding: List<Float>,
        enrolledEmbedding: List<Float>,
        threshold: Float = Models.FACENET.threshold
    ): VerificationResult {
        val score = faceRecognitionManager.calculateCosineSimilarity(capturedEmbedding, enrolledEmbedding)
        return VerificationResult(
            isMatch = score >= threshold,
            similarityScore = score,
            threshold = threshold
        )
    }
}

package com.example.attendanceapp.domain.facenet

object Models {
    val FaceNet: ModelInfo = ModelInfo(
        "FaceNet",
        "facenet.tflite",
        0.4f,
        160,
        128
    )
}

data class ModelInfo (
    val name: String,
    val assetsFileName: String,
    val threshold: Float,
    val inputDims: Int,
    val outputDims: Int
)
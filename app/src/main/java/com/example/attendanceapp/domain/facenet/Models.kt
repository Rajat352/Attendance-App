package com.example.attendanceapp.domain.facenet

enum class Models(
    val modelName: String,
    val assetsFileName: String,
    val threshold: Float,
    val inputDims: Int,
    val outputDims: Int
) {
    FACENET(
        "FaceNet",
        "facenet.tflite",
        0.4f,
        160,
        128
    )
}
package com.example.attendanceapp.domain.facenet

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

interface FaceRecognitionManager {
    fun extractEmbedding(bitmap: Bitmap): FloatArray
    fun calculateCosineSimilarity(e1: FloatArray, e2: FloatArray): Float
    fun close()
}

class FaceRecognitionManagerFaceNetImpl(
    val context: Context,
    val model: ModelInfo
): FaceRecognitionManager {

    private var interpreter: Interpreter? = null

    init {
        initInterpreter()
    }

    private fun initInterpreter() {
        val assetDescriptor = context.assets.openFd(model.assetsFileName)
        val inputStream = FileInputStream(assetDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetDescriptor.startOffset
        val declaredLength = assetDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val interpreterOptions = Interpreter.Options().apply {
                numThreads = 4
        }

        interpreter = Interpreter(modelBuffer, interpreterOptions)
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputSize = model.inputDims
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resized.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            // MobileFaceNet mean subtraction & scaling
            byteBuffer.putFloat((r - 127.5f) / 128.0f)
            byteBuffer.putFloat((g - 127.5f) / 128.0f)
            byteBuffer.putFloat((b - 127.5f) / 128.0f)
        }
        return byteBuffer
    }

    override fun extractEmbedding(bitmap: Bitmap): FloatArray {
        val input = preprocessBitmap(bitmap)
        val output = Array(1) { FloatArray(model.outputDims) }

        interpreter?.run(input, output) ?: error("Interpreter not initialized")

        val rawEmbedding = output[0]
        return l2Normalize(rawEmbedding)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in vector) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    override fun calculateCosineSimilarity(e1: FloatArray, e2: FloatArray): Float {
        require(e1.size == e2.size) { "Embedding size mismatch: ${e1.size} vs ${e2.size}" }
        var dot = 0f
        for (i in e1.indices) {
            dot += e1[i] * e2[i]
        }
        return dot
    }

    override fun close() {
        interpreter?.close()
    }

}
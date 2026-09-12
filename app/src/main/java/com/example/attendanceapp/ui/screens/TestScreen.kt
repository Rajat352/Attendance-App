package com.example.attendanceapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.attendanceapp.domain.facenet.FaceRecognitionManagerFaceNetImpl
import com.example.attendanceapp.domain.facenet.Models
import com.example.attendanceapp.domain.mlkit.FaceDetectionManagerMLKitImpl
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

@Composable
fun TestScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val detector = remember { FaceDetectionManagerMLKitImpl() }
    val mlKitDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
        )
    }
    val engine = remember { FaceRecognitionManagerFaceNetImpl(context, Models.FaceNet) }

    DisposableEffect(Unit) {
        onDispose {
            engine.close()
            mlKitDetector.close()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                isPermanentlyDenied = activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            }
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var face1Bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var face2Bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var embedding1 by remember { mutableStateOf<FloatArray?>(null) }
    var embedding2 by remember { mutableStateOf<FloatArray?>(null) }

    var isProcessing1 by remember { mutableStateOf(false) }
    var isProcessing2 by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Capture two photos to test face verification") }
    var similarityScore by remember { mutableStateOf<Float?>(null) }

    val captureSlot1 = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        Log.d("TestScreen", "captured image 1")
        if (bitmap != null) {
            scope.launch {
                isProcessing1 = true
                statusMessage = "Analyzing Photo 1..."
                withContext(Dispatchers.Default) {
                    detector.detectFaces(bitmap)
                }
                val faces = detectFacesInternal(mlKitDetector, bitmap)
                if (faces.isNotEmpty()) {
                    val cropped = cropFace(bitmap, faces.first().boundingBox)
                    face1Bitmap = cropped
                    embedding1 = withContext(Dispatchers.Default) {
                        engine.extractEmbedding(cropped)
                    }
                    statusMessage = "Photo 1 ready (${faces.size} face detected)"
                } else {
                    face1Bitmap = bitmap
                    embedding1 = null
                    statusMessage = "No face detected in Photo 1. Please retake."
                }
                similarityScore = null
                isProcessing1 = false
            }
        }
    }

    val captureSlot2 = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        Log.d("TestScreen", "captured image 2")
        if (bitmap != null) {
            scope.launch {
                isProcessing2 = true
                statusMessage = "Analyzing Photo 2..."
                withContext(Dispatchers.Default) {
                    detector.detectFaces(bitmap)
                }
                val faces = detectFacesInternal(mlKitDetector, bitmap)
                if (faces.isNotEmpty()) {
                    val cropped = cropFace(bitmap, faces.first().boundingBox)
                    face2Bitmap = cropped
                    embedding2 = withContext(Dispatchers.Default) {
                        engine.extractEmbedding(cropped)
                    }
                    statusMessage = "Photo 2 ready (${faces.size} face detected)"
                } else {
                    face2Bitmap = bitmap
                    embedding2 = null
                    statusMessage = "No face detected in Photo 2. Please retake."
                }
                similarityScore = null
                isProcessing2 = false
            }
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPermanentlyDenied)
                    "Camera permission was permanently denied. Please enable it in system settings."
                else
                    "Camera access is needed to capture photos and verify face embeddings.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant Camera Permission")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Face Recognition PoC",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Model: ${Models.FaceNet.name} (${Models.FaceNet.outputDims}-d) | Threshold: ${Models.FaceNet.threshold}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FaceSlotView(
                title = "Face 1",
                bitmap = face1Bitmap,
                hasEmbedding = embedding1 != null,
                isProcessing = isProcessing1,
                onCaptureClick = { captureSlot1.launch() }
            )

            FaceSlotView(
                title = "Face 2",
                bitmap = face2Bitmap,
                hasEmbedding = embedding2 != null,
                isProcessing = isProcessing2,
                onCaptureClick = { captureSlot2.launch() }
            )
        }

        Button(
            onClick = {
                val e1 = embedding1
                val e2 = embedding2
                if (e1 != null && e2 != null) {
                    scope.launch {
                        val score = withContext(Dispatchers.Default) {
                            engine.calculateCosineSimilarity(e1, e2)
                        }
                        similarityScore = score
                        val matched = score >= Models.FaceNet.threshold
                        statusMessage = if (matched) "Faces MATCH!" else "Faces DO NOT MATCH."
                    }
                }
            },
            enabled = embedding1 != null && embedding2 != null && !isProcessing1 && !isProcessing2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compare Similarity")
        }

        if (face1Bitmap != null || face2Bitmap != null) {
            OutlinedButton(
                onClick = {
                    face1Bitmap = null
                    face2Bitmap = null
                    embedding1 = null
                    embedding2 = null
                    similarityScore = null
                    statusMessage = "Cleared. Capture two photos to compare."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear / Reset")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Status: $statusMessage",
                    style = MaterialTheme.typography.bodyMedium
                )

                similarityScore?.let { score ->
                    val isMatch = score >= Models.FaceNet.threshold
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cosine Similarity: ${String.format(Locale.US, "%.4f", score)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMatch) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isMatch) "MATCH (Same Person)" else "NO MATCH (Different Person)",
                            color = if (isMatch) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "Decision rule: score >= ${Models.FaceNet.threshold} (Higher means more similar)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceSlotView(
    title: String,
    bitmap: Bitmap?,
    hasEmbedding: Boolean,
    isProcessing: Boolean,
    onCaptureClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold)

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "No Photo",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = when {
                isProcessing -> "Processing..."
                hasEmbedding -> "Embedding ready"
                bitmap != null -> "No face detected"
                else -> "Empty"
            },
            fontSize = 12.sp,
            color = when {
                hasEmbedding -> Color(0xFF2E7D32)
                bitmap != null && !hasEmbedding -> Color(0xFFC62828)
                else -> Color.Gray
            }
        )

        Button(
            onClick = onCaptureClick,
            enabled = !isProcessing
        ) {
            Text(if (bitmap == null) "Take Photo" else "Retake")
        }
    }
}

private suspend fun detectFacesInternal(
    detector: FaceDetector,
    bitmap: Bitmap
): List<Face> = suspendCancellableCoroutine { continuation ->
    val image = InputImage.fromBitmap(bitmap, 0)
    detector.process(image)
        .addOnSuccessListener { faces ->
            if (continuation.isActive) {
                continuation.resume(faces)
            }
        }
        .addOnFailureListener { error ->
            Log.e("TestScreen", "Detection error", error)
            if (continuation.isActive) {
                continuation.resume(emptyList())
            }
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
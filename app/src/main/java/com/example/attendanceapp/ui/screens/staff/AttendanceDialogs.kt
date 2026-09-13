package com.example.attendanceapp.ui.screens.staff

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.domain.facenet.Models
import com.example.attendanceapp.ui.screens.admin.formatCoordinate
import com.example.attendanceapp.ui.screens.admin.formatDateTime

@Composable
fun AttendanceFeedbackDialogs(
    stage: AttendanceStage,
    staffId: Int,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    when (stage) {
        AttendanceStage.ProcessingSelfie,
        AttendanceStage.VerifyingFace,
        AttendanceStage.FetchingLocation,
        AttendanceStage.SubmittingAttendance -> {
            AttendanceProgressDialog(stage = stage)
        }
        is AttendanceStage.Success -> {
            SuccessAttendanceDialog(
                stage = stage,
                staffId = staffId,
                onDismiss = onDismiss
            )
        }
        is AttendanceStage.Failure -> {
            FailureAttendanceDialog(
                failure = stage,
                onRetry = onRetry,
                onDismiss = onDismiss
            )
        }
        AttendanceStage.Idle -> { /* Idle */ }
    }
}

@Composable
fun AttendanceProgressDialog(stage: AttendanceStage) {
    val message = when (stage) {
        AttendanceStage.ProcessingSelfie -> "Analyzing selfie..."
        AttendanceStage.VerifyingFace -> "Matching face with enrolled profile..."
        AttendanceStage.FetchingLocation -> "Getting current GPS coordinates..."
        AttendanceStage.SubmittingAttendance -> "Recording attendance..."
        AttendanceStage.Idle,
        is AttendanceStage.Success,
        is AttendanceStage.Failure -> "Processing..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SuccessAttendanceDialog(
    stage: AttendanceStage.Success,
    staffId: Int,
    onDismiss: () -> Unit
) {
    val (dateStr, timeStr) = remember(stage.timestamp) {
        formatDateTime(stage.timestamp)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Attendance Recorded!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selfie preview thumbnail
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = stage.selfie.asImageBitmap(),
                        contentDescription = "Recorded Selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailRow(label = "Staff ID:", value = "$staffId")
                        DetailRow(label = "Date:", value = dateStr)
                        DetailRow(label = "Time:", value = timeStr)
                        DetailRow(label = "Latitude:", value = formatCoordinate(stage.latitude))
                        DetailRow(label = "Longitude:", value = formatCoordinate(stage.longitude))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun FailureAttendanceDialog(
    failure: AttendanceStage.Failure,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMismatch = failure.reason == FailureReason.FACE_MISMATCH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMismatch) "Face Verification Failed" else "Attendance Not Recorded",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = failure.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isMismatch && failure.similarityScore != null) {
                    val scorePercent = (failure.similarityScore * 100).toInt()
                    val reqPercent = (Models.FACENET.threshold * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Similarity: $scorePercent% (Threshold required: $reqPercent%)\nAttendance was NOT recorded.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

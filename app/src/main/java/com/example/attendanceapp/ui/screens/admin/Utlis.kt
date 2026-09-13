package com.example.attendanceapp.ui.screens.admin

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Format timestamp safely handling both seconds and milliseconds
fun formatDateTime(timestamp: Long): Pair<String, String> {
    val millis = if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
    return Pair(date, time)
}

fun formatCoordinate(value: Double): String {
    return String.format(Locale.US, "%.5f", value)
}

fun resolveImageUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        val base = "http://192.168.1.7:8000".trimEnd('/')
        if (trimmed.startsWith("/")) "$base$trimmed" else "$base/$trimmed"
    }
}

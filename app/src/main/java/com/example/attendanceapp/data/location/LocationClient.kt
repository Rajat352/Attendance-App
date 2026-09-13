package com.example.attendanceapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double
)

interface LocationClient {
    suspend fun getCurrentLocation(): Result<LocationCoordinates>
}

class DefaultLocationClient @Inject constructor(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    override suspend fun getCurrentLocation(): Result<LocationCoordinates> = withContext(Dispatchers.IO) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return@withContext Result.failure(SecurityException("Location permission not granted. Please allow location access."))
        }

        try {
            // First try high accuracy current location
            val currentLocation = withTimeoutOrNull(5000L) {
                fetchCurrentLocation()
            }

            if (currentLocation != null) {
                return@withContext Result.success(LocationCoordinates(currentLocation.latitude, currentLocation.longitude))
            }

            // Fallback to last known location
            val lastLocation = fetchLastLocation()
            if (lastLocation != null) {
                return@withContext Result.success(LocationCoordinates(lastLocation.latitude, lastLocation.longitude))
            }

            Result.failure(Exception("Unable to retrieve location coordinates. Please check your GPS settings."))
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while getting current location")
            Result.failure(e)
        }
    }

    private suspend fun fetchCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation { cts.cancel() }

        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        } catch (e: SecurityException) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    private suspend fun fetchLastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        } catch (e: SecurityException) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    companion object {
        private const val TAG = "LocationClient"
    }
}

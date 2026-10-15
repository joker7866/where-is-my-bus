package com.wheresmybus.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Thin wrapper over the platform LocationManager (no Play Services dependency).
 * Emits the device's last known fix immediately, then live updates as they arrive.
 */
object DeviceLocation {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun updates(context: Context): Flow<Location> = callbackFlow {
        if (!hasPermission(context)) {
            close()
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { location -> trySend(location) }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { locationManager.isProviderEnabled(it) }

        providers.forEach { provider ->
            locationManager.getLastKnownLocation(provider)?.let { trySend(it) }
            locationManager.requestLocationUpdates(provider, 5_000L, 25f, listener, Looper.getMainLooper())
        }

        awaitClose { locationManager.removeUpdates(listener) }
    }
}

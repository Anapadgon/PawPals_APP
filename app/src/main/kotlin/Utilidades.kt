package com.pawpals.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Distancia en km (Haversine). */
object UtilidadesGeograficas {
    fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthKm * c
    }
}

private fun Location.toPair(): Pair<Double, Double> = latitude to longitude

@Singleton
class ControladorUbicacion @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun tienePermiso(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun ultimaUbicacionConocidaOFresca(): Result<Pair<Double, Double>> {
        if (!tienePermiso()) {
            return Result.failure(SecurityException("Sin permiso de ubicación"))
        }
        if (!isLocationEnabled()) {
            return Result.failure(IllegalStateException("Activa la ubicación del sistema"))
        }
        lastLocationSafe()?.let { return Result.success(it) }
        val fresh = withTimeoutOrNull(8_000) { currentLocationSafe() }
        return fresh?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No se pudo obtener la ubicación. Comprueba GPS o emulador."))
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastLocationSafe(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.toPair())
                }
                .addOnFailureListener { cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocationSafe(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val cancel = CancellationTokenSource()
            cont.invokeOnCancellation { cancel.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token)
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.toPair())
                }
                .addOnFailureListener { cont.resume(null) }
        }
}

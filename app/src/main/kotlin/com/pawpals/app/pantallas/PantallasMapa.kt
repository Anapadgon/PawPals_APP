package com.pawpals.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
// mapa para ver amigos paseando y marcar el propio paseo
fun PantallaMapa(
    miUid: String,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaMapa = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var permisoConcedido by remember { mutableStateOf(viewModel.tienePermisoUbicacion()) }
    var mostrarDialogoUbicacion by remember { mutableStateOf(false) }
    var permisoDenegado by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val ahora = viewModel.tienePermisoUbicacion()
                permisoConcedido = ahora
                if (ahora) {
                    viewModel.cargarCercanos(miUid)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val lanzadorUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val concedido = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permisoConcedido = concedido
        permisoDenegado = !concedido
        if (concedido) viewModel.cargarCercanos(miUid)
    }

    LaunchedEffect(miUid, permisoConcedido) {
        if (permisoConcedido) {
            viewModel.cargarCercanos(miUid)
        }
    }
    LaunchedEffect(estado.snack) {
        if (estado.snack != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.limpiarMensajeSnack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Paseos en vivo",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
            ),
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            when {
                !permisoConcedido -> LocationPermissionCTA {
                    mostrarDialogoUbicacion = true
                }

                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                estado.miUbicacion != null && BuildConfig.MAPS_API_KEY.isNotBlank() -> {
                    val yo = estado.miUbicacion!!
                    val cam = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(yo, 14f)
                    }
                    LaunchedEffect(yo.latitude, yo.longitude) {
                        cam.animate(CameraUpdateFactory.newLatLngZoom(yo, 14f))
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cam,
                        properties = MapProperties(isMyLocationEnabled = false),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true
                        ),
                    ) {
                        val marcadorYo = remember(yo.latitude, yo.longitude) {
                            MarkerState(position = yo)
                        }
                        Marker(
                            state = marcadorYo,
                            title = "Tú",
                        )
                        estado.amigosPaseando.forEach { f ->
                            val lat = f.latitud ?: return@forEach
                            val lng = f.longitud ?: return@forEach
                            key(f.uid) {
                                val posicion = LatLng(lat, lng)
                                val marcadorAmigo = remember { MarkerState(position = posicion) }
                                LaunchedEffect(lat, lng) {
                                    marcadorAmigo.position = posicion
                                }
                                Marker(
                                    state = marcadorAmigo,
                                    title = f.nombreVisible,
                                    snippet = f.zona,
                                )
                            }
                        }
                    }
                }

                estado.miUbicacion != null && BuildConfig.MAPS_API_KEY.isBlank() -> {
                    VistaPaseosSinMapa(
                        estado = estado,
                        onRetry = { viewModel.cargarCercanos(miUid) },
                    )
                }

                else -> {
                    LocationUnavailableCTA(
                        mensaje = estado.mensaje
                            ?: "No pudimos obtener tu ubicación. Revisa permisos y GPS.",
                        onRetry = { viewModel.cargarCercanos(miUid) },
                    )
                }
            }

            if (permisoConcedido && !estado.cargando && estado.miUbicacion != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        "${estado.amigosPaseando.size} amigos paseando",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        estado.snack?.let {
            Text(
                it,
                color = CoralPrimary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (estado.amigosPaseando.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SuccessSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                    )
                    Text(
                        text = estado.amigosPaseando.take(2)
                            .joinToString(" y ") { it.nombreVisible.ifBlank { "Un amigo" } } +
                                " están paseando ahora",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (permisoConcedido && estado.miUbicacion != null) {
                    viewModel.alternarPaseo(miUid)
                } else {
                    permisoDenegado = true
                }
            },
            enabled = permisoConcedido && estado.miUbicacion != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (estado.yoPaseando) CoralPrimary else SuccessGreen,
                contentColor = OnCoral,
            ),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                when {
                    !permisoConcedido && permisoDenegado -> "Necesitas activar el GPS en los ajustes"
                    estado.yoPaseando -> "Finalizar paseo"
                    else -> "Iniciar paseo"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (mostrarDialogoUbicacion) {
        DialogoPermisoUbicacion(
            onDismiss = { mostrarDialogoUbicacion = false },
            onConfirm = {
                mostrarDialogoUbicacion = false
                lanzadorUbicacion.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            },
        )
    }
}

@Composable
private fun DialogoPermisoUbicacion(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = CoralPrimary,
            )
        },
        title = { Text("Permiso de ubicación") },
        text = {
            Text(
                "PawPals necesita acceder a tu ubicación para poder mostrarte a otros perros cercanos " +
                        "cuando actives el Modo Paseo. Tu ubicación no se compartirá si no lo activas.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Permitir", color = CoralPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ahora no", color = TextSecondary) }
        },
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.LocationPermissionCTA(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = CoralPrimary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Para ver paseos en vivo necesitamos tu ubicación",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Solo la usaremos para emparejarte con amigos cercanos.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnCoral
            ),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Activar ubicación") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VistaPaseosSinMapa(
    estado: EstadoUiMapa,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "El mapa no puede cargarse sin Maps SDK.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Define MAPS_API_KEY en local.properties (igual que en build.gradle), habilita " +
                    "\"Maps SDK for Android\" en Google Cloud y reconstruye la app.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        estado.miUbicacion?.let { yo ->
            Spacer(Modifier.height(12.dp))
            Text(
                "Tu ubicación: ${"%.5f".format(yo.latitude)}, ${"%.5f".format(yo.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        if (estado.amigosPaseando.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Amigos paseando cerca",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            estado.amigosPaseando.forEach { f ->
                val lat = f.latitud
                val lng = f.longitud
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            f.nombreVisible.ifBlank { "Amigo" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        if (f.zona.isNotBlank()) {
                            Text(
                                f.zona,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        if (lat != null && lng != null) {
                            Text(
                                "${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnCoral
            ),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Actualizar lista") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.LocationUnavailableCTA(
    mensaje: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnCoral
            ),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Reintentar") }
    }
}

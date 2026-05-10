package com.pawpals.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// pantalla principal para descubrir perros y hacer swipe
fun PantallaExplorar(
    miUid: String,
    onOpenCoincidencia: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaExplorar = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarAvisoNotificaciones by remember { mutableStateOf(false) }
    var refrescando by remember { mutableStateOf(false) }
    val estadoRefresco = rememberPullToRefreshState()

    LaunchedEffect(miUid) {
        viewModel.cargar(miUid)
    }
    LaunchedEffect(estado.cargando) {
        if (!estado.cargando) refrescando = false
    }
    LaunchedEffect(estado.ultimoUidCoincidencia) {
        estado.ultimoUidCoincidencia?.let {
            onOpenCoincidencia(it)
            viewModel.limpiarNavegacionCoincidencia()
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val alturaFoto = (maxHeight - 285.dp).coerceIn(320.dp, 460.dp)
        val alturaEstadoVacio = (maxHeight - 160.dp).coerceIn(360.dp, 520.dp)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        isRefreshing = refrescando,
                        state = estadoRefresco,
                        onRefresh = {
                            refrescando = true
                            viewModel.cargar(miUid)
                        },
                    )
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Explorar",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        ),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp),
                    ) {
                        IconButton(onClick = { mostrarAvisoNotificaciones = true }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                when {
                    estado.cargando && !refrescando -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    !estado.hayMas -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.height(alturaEstadoVacio),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyExplorarState()
                        }
                    }

                    else -> estado.actual?.let { card ->
                        VistaTarjetaExplorar(card, alturaFoto = alturaFoto)

                        estado.error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ActionCircle(
                                    icon = Icons.Filled.Close,
                                    tint = TextSecondary,
                                    background = Color.White,
                                    border = OutlineSoft,
                                    bigger = true,
                                    onClick = { viewModel.rechazar(miUid) },
                                )
                                ActionCircle(
                                    icon = Icons.Filled.Star,
                                    tint = InfoBlue,
                                    background = InfoSoft,
                                    border = InfoSoft,
                                    bigger = true,
                                    onClick = { viewModel.superMeGusta(miUid) },
                                )
                                ActionCircle(
                                    icon = Icons.Filled.Favorite,
                                    tint = OnCoral,
                                    background = CoralPrimary,
                                    border = CoralPrimary,
                                    bigger = true,
                                    onClick = { viewModel.meGusta(miUid) },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = refrescando,
                state = estadoRefresco,
            )
        }
    }

    if (mostrarAvisoNotificaciones) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoNotificaciones = false },
            confirmButton = {
                TextButton(onClick = { mostrarAvisoNotificaciones = false }) { Text("Cerrar") }
            },
            title = { Text("Sin notificaciones nuevas") },
            text = {
                Text(
                    "Cuando alguien haga match contigo o te escriba, te avisaremos aquí. " +
                            "Asegúrate de tener las notificaciones activadas en Ajustes.",
                )
            },
        )
    }
}

@Composable
// boton redondo para descartar, super like o like
private fun ActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    bigger: Boolean = false,
) {
    val size = if (bigger) 64.dp else 56.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
// tarjeta principal con foto, datos del perro y distancia aproximada
private fun VistaTarjetaExplorar(card: TarjetaExplorar, alturaFoto: androidx.compose.ui.unit.Dp) {
    val perro: PerfilPerro? = card.perro
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column {
            // zona de foto con fondo de reserva
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaFoto)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(PeachLight, PeachSecondary.copy(alpha = 0.7f)),
                        ),
                    ),
            ) {
                // chips con los rasgos del perro
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (perro != null) {
                        InsigniaAtributo(
                            texto = perro.energia.label,
                            colorAcento = AcentosPaw.colorEnergia,
                            fondoAcento = AcentosPaw.fondoEnergia,
                        )
                        InsigniaAtributo(
                            texto = perro.sociabilidad.label,
                            colorAcento = AcentosPaw.colorSocial,
                            fondoAcento = AcentosPaw.fondoSocial,
                        )
                    }
                }
                val fotoTarjeta = perro?.urlFoto?.takeIf { it.isNotBlank() }
                    ?: card.usuario.urlFoto?.takeIf { it.isNotBlank() }
                if (fotoTarjeta != null) {
                    coil.compose.AsyncImage(
                        model = fotoTarjeta,
                        contentDescription = perro?.nombre ?: card.usuario.nombreVisible,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Pets,
                            contentDescription = null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = buildString {
                        append(perro?.nombre ?: card.usuario.nombreVisible.ifBlank { "Dueño" })
                        if (perro != null) {
                            append(" · ${perro.raza} · ${perro.edadAnios} años")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = perro?.biografia?.takeIf { it.isNotBlank() }
                        ?: card.usuario.sobreMi.takeIf { it.isNotBlank() }
                        ?: "Le encanta pasear y hacer nuevos amigos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    val distancia = card.distanciaMetros?.let { meters ->
                        if (meters < 1000) "A ${meters.toInt()}m"
                        else "A ${"%.1f".format(meters / 1000)} km"
                    }
                    val zona = card.usuario.zona.takeIf { it.isNotBlank() }
                    Text(
                        text = listOfNotNull(distancia, zona).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralPrimary,
                    )
                }
            }
        }
    }
}

@Composable
// mensaje que se muestra cuando ya no quedan perfiles para descubrir
private fun EmptyExplorarState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = PeachPale,
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Pets,
                    contentDescription = null,
                    tint = CoralPrimary,
                    modifier = Modifier.size(54.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Todavía no hay más peludos cerca",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Vuelve más tarde o amplía tu zona en ajustes.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

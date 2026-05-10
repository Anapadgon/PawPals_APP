package com.pawpals.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
// lista de conversaciones aceptadas con sus datos principales
fun PantallaListaConversaciones(
    miUid: String,
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaListaConversaciones = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(miUid) {
        viewModel.observar(miUid)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Mensajes",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
            ),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar conversación…", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SurfaceMuted,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = CoralPrimary,
                cursorColor = CoralPrimary,
            ),
        )
        Spacer(Modifier.height(12.dp))

        val filtered = estado.entradasAceptadas.filter { entry ->
            if (query.isBlank()) true else {
                entry.nombreDueno.contains(query, ignoreCase = true) ||
                        entry.nombrePerro.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            ListaConversacionesVacia()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.coincidencia.id }) { entry ->
                    FilaListaConversaciones(
                        entry = entry,
                        marcaTemporal = formatRelative(
                            entry.ultimoMensajeEn.takeIf { it > 0 } ?: entry.coincidencia.creadoEn,
                        ),
                        onClick = { onOpenThread(entry.otroUid) },
                    )
                    HorizontalDivider(color = OutlineSoft.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun FilaListaConversaciones(
    entry: EntradaListaConversaciones,
    marcaTemporal: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AvatarConversacion(
                urlFoto = entry.urlFotoPerro ?: entry.urlFotoDueno,
                nombreRespaldo = entry.nombrePerro.ifBlank { entry.nombreDueno.ifBlank { entry.otroUid } },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            // mostramos primero el perro, luego el dueno y nunca un uid feo
            val title = entry.nombrePerro.ifBlank {
                entry.nombreDueno.ifBlank { "Amigo" }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            val subtitle = when {
                entry.ultimoMensaje.isNotBlank() -> {
                    val prefijo = if (entry.ultimoRemitente == entry.otroUid) "" else "Tú: "
                    prefijo + entry.ultimoMensaje
                }

                entry.nombreDueno.isNotBlank() -> "Con ${entry.nombreDueno}"
                else -> "Toca para abrir la conversación"
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.tieneMensajeNuevo) TextPrimary else TextSecondary,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                marcaTemporal,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (entry.tieneMensajeNuevo) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CoralPrimary,
                ) {
                    Text(
                        "Nuevo",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarConversacion(urlFoto: String?, nombreRespaldo: String) {
    if (!urlFoto.isNullOrBlank()) {
        coil.compose.AsyncImage(
            model = urlFoto,
            contentDescription = nombreRespaldo,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else {
        AvatarPaw(nombre = nombreRespaldo, tamano = 52.dp)
    }
}

// hora si es de hoy, dia y mes si es anterior
private fun formatRelative(ts: Long): String {
    if (ts <= 0) return ""
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    val sameDay = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "dd/MM"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        .format(java.util.Date(ts))
}

@Composable
private fun ListaConversacionesVacia() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = PeachPale,
            modifier = Modifier.size(112.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Pets,
                    contentDescription = null,
                    tint = CoralPrimary,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Aún no tienes conversaciones",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Haz un match en Explorar y rompe el hielo con un hola.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

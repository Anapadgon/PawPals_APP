package com.pawpals.app

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
// pantalla cuando dos usuarios se gustan mutuamente
fun PantallaResultadoCoincidencia(
    otroUid: String,
    onSendMessage: () -> Unit,
    onKeepExploring: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaResultadoCoincidencia = hiltViewModel(),
) {
    LaunchedEffect(otroUid) { viewModel.cargar(otroUid) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val ctaName =
        estado.nombrePerro.ifBlank { estado.nombreOtroUsuario.ifBlank { "tu nuevo amigo" } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CoralGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-24).dp)) {
                AvatarCoincidencia(urlFoto = estado.urlFotoUsuario, fallbackAlpha = 1f)
                AvatarCoincidencia(urlFoto = estado.urlFotoPerro, fallbackAlpha = 0.6f)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "¡Es un Match!",
                color = OnCoral,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("Acabáis de gustaros mutuamente.")
                    if (estado.nombrePerro.isNotBlank()) {
                        append("\n¡Ya podéis quedar para pasear con ${estado.nombrePerro}!")
                    } else {
                        append("\n¡Podéis quedar para un paseo!")
                    }
                },
                color = OnCoral.copy(alpha = 0.95f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BotonPrimarioPaw(
                texto = "Enviar mensaje a $ctaName",
                onClick = onSendMessage,
            )
            BotonContornoPaw(
                texto = "Seguir explorando",
                onClick = onKeepExploring,
            )
        }
    }
}

@Composable
private fun AvatarCoincidencia(urlFoto: String?, fallbackAlpha: Float) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(OnCoral)
            .border(3.dp, CoralPrimary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!urlFoto.isNullOrBlank()) {
            AsyncImage(
                model = urlFoto,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = CoralPrimary.copy(alpha = fallbackAlpha),
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

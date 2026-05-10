package com.pawpals.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
// aviso para cuentas bloqueadas con opcion de contactar soporte
fun PantallaBloqueado(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaBloqueado = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Cuenta restringida",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu acceso a PawPals ha sido suspendido por nuestro equipo de moderación.\n" +
                    "Si crees que es un error, cuéntanos qué ocurre y lo revisaremos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        if (estado.enviado) {
            Surface(
                color = SuccessSoft,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Hemos recibido tu mensaje. Te contestaremos al correo con el que te registraste.",
                    modifier = Modifier.padding(16.dp),
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(20.dp))
            BotonContornoPaw(
                texto = "Enviar otro mensaje",
                onClick = { viewModel.reiniciarEnviado() },
                colorBorde = CoralPrimary,
                colorContenido = CoralPrimary,
            )
        } else {
            Surface(
                color = PeachPale,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Explícanos la situación. Nuestro equipo revisará tu caso y te responderá " +
                            "al correo que usaste para registrarte.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            CampoContornoPaw(
                valor = estado.mensaje,
                alCambiarValor = viewModel::alCambiarMensaje,
                etiqueta = "Tu mensaje al equipo de soporte",
                unaLinea = false,
                marcador = "Cuenta qué ha pasado (mín. 10 caracteres)",
            )
            estado.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (estado.enviando) {
                CircularProgressIndicator()
            } else {
                BotonPrimarioPaw(
                    texto = "Enviar mensaje a soporte",
                    onClick = { viewModel.enviarTicket() },
                    habilitado = estado.mensaje.trim().length >= 10,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        BotonContornoPaw(
            texto = "Cerrar sesión",
            onClick = onSignOut,
            colorBorde = CoralPrimary,
            colorContenido = CoralPrimary,
        )
        Spacer(Modifier.height(32.dp))
    }
}

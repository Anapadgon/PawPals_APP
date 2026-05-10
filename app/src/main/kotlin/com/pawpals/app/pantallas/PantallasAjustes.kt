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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
// ajustes de cuenta, notificaciones, privacidad y acciones sensibles
fun PantallaAjustes(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaAjustes = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarDialogoCorreo by remember { mutableStateOf(false) }
    var mostrarDialogoContrasena by remember { mutableStateOf(false) }
    var mostrarAvisoUbicacion by remember { mutableStateOf(false) }
    var valorUbicacionPendiente by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(estado.info, estado.error) {
        if (estado.info != null || estado.error != null) {
            kotlinx.coroutines.delay(3800)
            viewModel.limpiarRetroalimentacion()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        BarraSuperiorPaw(titulo = "Ajustes", alVolver = onBack)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SettingsTituloSeccion("Notificaciones")
            FilaInterruptor(
                icon = Icons.Filled.Chat,
                iconBg = InfoSoft,
                iconTint = InfoBlue,
                titulo = "Mensajes",
                subtitulo = "Nuevos mensajes de amigos",
                checked = estado.notificarMensajes,
                onToggle = viewModel::establecerNotificarMensajes,
            )
            FilaInterruptor(
                icon = Icons.Filled.LocationOn,
                iconBg = SuccessSoft,
                iconTint = SuccessGreen,
                titulo = "Paseos",
                subtitulo = "Cuando un amigo sale a pasear",
                checked = estado.notificarPaseos,
                onToggle = viewModel::establecerNotificarPaseos,
            )
            FilaInterruptor(
                icon = Icons.Filled.Favorite,
                iconBg = PeachPale,
                iconTint = CoralPrimary,
                titulo = "Matches",
                subtitulo = "Nuevas conexiones",
                checked = estado.notificarCoincidencias,
                onToggle = viewModel::establecerNotificarCoincidencias,
            )
            Spacer(Modifier.height(16.dp))

            SettingsTituloSeccion("Privacidad")
            FilaInterruptor(
                icon = Icons.Filled.LocationOn,
                iconBg = InfoSoft,
                iconTint = InfoBlue,
                titulo = "Ubicación solo en paseo",
                subtitulo = "Oculta tu ubicación cuando no estás paseando",
                checked = estado.ubicacionSoloDurantePaseo,
                onToggle = { nuevoValor ->
                    // si el usuario quiere compartir siempre, antes se muestra un aviso
                    if (!nuevoValor && estado.ubicacionSoloDurantePaseo) {
                        valorUbicacionPendiente = nuevoValor
                        mostrarAvisoUbicacion = true
                    } else {
                        viewModel.establecerPrivacidadUbicacion(nuevoValor)
                    }
                },
            )
            Spacer(Modifier.height(16.dp))

            SettingsTituloSeccion("Cuenta")
            FilaNavegacion(
                icon = Icons.Filled.Email,
                iconBg = PeachPale,
                iconTint = CoralPrimary,
                titulo = "Cambiar correo electrónico",
                subtitulo = estado.correo.orEmpty().ifBlank { "Sin sesión" },
                onClick = { mostrarDialogoCorreo = true },
            )
            FilaNavegacion(
                icon = Icons.Filled.Lock,
                iconBg = PeachPale,
                iconTint = CoralPrimary,
                titulo = "Cambiar contraseña",
                subtitulo = "Con tu contraseña actual o por correo",
                onClick = { mostrarDialogoContrasena = true },
            )
            FilaNavegacion(
                icon = Icons.AutoMirrored.Filled.Logout,
                iconBg = ErrorSoft,
                iconTint = Error,
                titulo = "Cerrar sesión",
                subtitulo = null,
                colorTitulo = Error,
                onClick = { viewModel.cerrarSesion() },
                hideArrow = true,
            )
            FilaNavegacion(
                icon = Icons.Filled.Delete,
                iconBg = ErrorSoft,
                iconTint = Error,
                titulo = "Eliminar cuenta permanentemente",
                subtitulo = "Borra tu perfil y todos tus datos",
                colorTitulo = Error,
                onClick = { mostrarDialogoEliminar = true },
            )

            Spacer(Modifier.height(12.dp))
            if (estado.ocupado) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = CoralPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Procesando…", color = TextSecondary)
                }
            }
            estado.info?.let {
                BannerRetroalimentacion(texto = it, color = SuccessGreen, bg = SuccessSoft)
            }
            estado.error?.let {
                BannerRetroalimentacion(texto = it, color = Error, bg = ErrorSoft)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (mostrarDialogoCorreo) {
        DialogoCambiarCorreo(
            correoActual = estado.correo.orEmpty(),
            onDismiss = { mostrarDialogoCorreo = false },
            onConfirm = { nuevoCorreo ->
                mostrarDialogoCorreo = false
                viewModel.cambiarCorreo(nuevoCorreo)
            },
        )
    }
    if (mostrarDialogoContrasena) {
        DialogoCambiarContrasena(
            onDismiss = { mostrarDialogoContrasena = false },
            onSendReset = {
                mostrarDialogoContrasena = false
                viewModel.enviarRestablecimientoContrasena()
            },
            onChange = { current, new ->
                mostrarDialogoContrasena = false
                viewModel.cambiarContrasena(current, new)
            },
        )
    }
    if (mostrarAvisoUbicacion) {
        DialogoAvisoPrivacidadUbicacion(
            onCancel = { mostrarAvisoUbicacion = false },
            onConfirm = {
                mostrarAvisoUbicacion = false
                viewModel.establecerPrivacidadUbicacion(valorUbicacionPendiente)
            },
        )
    }
    if (mostrarDialogoEliminar) {
        DialogoEliminarCuenta(
            onDismiss = { mostrarDialogoEliminar = false },
            onConfirm = { password ->
                mostrarDialogoEliminar = false
                viewModel.eliminarCuenta(password)
            },
        )
    }
}

@Composable
private fun SettingsTituloSeccion(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun FilaInterruptor(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaldosaIcono(icon, iconBg, iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(subtitulo, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CoralPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun FilaNavegacion(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    titulo: String,
    subtitulo: String?,
    colorTitulo: Color = TextPrimary,
    hideArrow: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaldosaIcono(icon, iconBg, iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorTitulo,
            )
            if (subtitulo != null) {
                Text(subtitulo, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!hideArrow) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
            )
        }
    }
}

@Composable
private fun BaldosaIcono(
    icon: ImageVector,
    background: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BannerRetroalimentacion(texto: String, color: Color, bg: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            texto,
            color = color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DialogoCambiarCorreo(
    correoActual: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nuevoCorreo by remember { mutableStateOf("") }
    val valid = nuevoCorreo.contains("@") && nuevoCorreo.length >= 5 && nuevoCorreo != correoActual
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar correo electrónico") },
        text = {
            Column {
                Text(
                    "Te enviaremos un correo al nuevo correo. " +
                            "Hasta que confirmes el enlace, tu correo actual seguirá siendo válido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nuevoCorreo,
                    onValueChange = { nuevoCorreo = it },
                    label = { Text("Nuevo correo") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(nuevoCorreo.trim()) }) {
                Text("Enviar", color = CoralPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
}

@Composable
private fun DialogoCambiarContrasena(
    onDismiss: () -> Unit,
    onSendReset: () -> Unit,
    onChange: (actual: String, nueva: String) -> Unit,
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    val valid = actual.length >= 6 && nueva.length >= 6 && actual != nueva
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar contraseña") },
        text = {
            Column {
                Text(
                    "Puedes cambiarla introduciendo la contraseña actual o, si la has olvidado, " +
                            "recibir un correo con el enlace de restablecimiento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = actual,
                    onValueChange = { actual = it },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nueva,
                    onValueChange = { nueva = it },
                    label = { Text("Nueva contraseña (min. 6)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onSendReset) {
                    Text("¿Olvidaste la actual? Enviar correo", color = CoralPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onChange(actual, nueva) }) {
                Text("Cambiar", color = CoralPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
}

@Composable
private fun DialogoAvisoPrivacidadUbicacion(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Compartir ubicación siempre") },
        text = {
            Column {
                Text(
                    "Si desactivas esta opción, tu ubicación aproximada será visible para otros " +
                            "usuarios de PawPals aunque no estés paseando.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Qué se comparte:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "• Tu latitud y longitud en el mapa de amigos cercanos.\n" +
                            "• Actualizaciones cuando tu ubicación cambia de forma significativa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Recomendado: mantén activa la privacidad para que tu ubicación solo se " +
                            "comparta cuando inicies un paseo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Compartir siempre", color = Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Mantener privado", color = CoralPrimary)
            }
        },
    )
}

@Composable
private fun DialogoEliminarCuenta(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var contrasena by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val requiredPhrase = "ELIMINAR"
    val valid =
        contrasena.length >= 6 && confirmation.trim().equals(requiredPhrase, ignoreCase = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Estás seguro?", color = Error) },
        text = {
            Column {
                Text(
                    "Se borrarán todos tus datos, los de tu perro y tus chats. " +
                            "Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Escribe \"$requiredPhrase\" para confirmar:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(requiredPhrase) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(contrasena) }) {
                Text("Eliminar cuenta permanentemente", color = Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
}

package com.pawpals.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdministracion(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaAdministracion = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Usuarios", "Estadísticas", "Moderación")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Panel administradoristrador") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Salir")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (tab) {
                0 -> AdminUsersTab(estado, viewModel)
                1 -> EstadisticasAdministracionTab(estado, viewModel)
                2 -> AdminModerationTab(estado, viewModel)
            }
        }
    }
}

@Composable
private fun AdminUsersTab(estado: EstadoUiAdministracion, vm: ModeloVistaAdministracion) {
    if (estado.cargando) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(estado.usuarios, key = { it.uid }) { u ->
            Column {
                Text(u.nombreVisible.ifBlank { u.correo }, style = MaterialTheme.typography.titleMedium)
                Text(u.correo, style = MaterialTheme.typography.bodySmall)
                Text("Rol: ${u.rol.name} · Bloqueado: ${u.bloqueado}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                BotonPrimarioPaw(
                    texto = if (u.bloqueado) "Desbanear" else "Banear",
                    onClick = { vm.establecerBloqueado(u.uid, !u.bloqueado) },
                )
                Spacer(Modifier.height(8.dp))
                BotonPrimarioPaw(
                    texto = "Eliminar perfil de la base de datos",
                    onClick = { vm.eliminarUsuario(u.uid) },
                )
            }
        }
    }
}

@Composable
private fun EstadisticasAdministracionTab(estado: EstadoUiAdministracion, vm: ModeloVistaAdministracion) {
    Column(Modifier.padding(16.dp)) {
        if (estado.cargando) {
            CircularProgressIndicator()
        }
        val estadisticas = estado.estadisticas
        if (estadisticas != null) {
            TituloSeccion("Totales (demo en cliente)")
            Text("Usuarios: ${estadisticas.numeroUsuarios}")
            Text("Perros: ${estadisticas.numeroPerros}")
            Text("Coincidencias: ${estadisticas.numeroCoincidencias}")
            Text("Reportes abiertos: ${estadisticas.reportesAbiertos}")
        } else {
            Text("Sin datos")
        }

        Spacer(Modifier.height(24.dp))
        TituloSeccion("Datos de prueba")
        Text(
            "Crea un conjunto de perfiles ficticios con sus perros para poder " +
                "probar el listado \"Explorar\", los coincidencias y los conversaciones sin registrar " +
                "cuentas nuevas. Se colocan cerca de tu ubicación actual.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        BotonPrimarioPaw(
            texto = if (estado.sembrando) "Procesando…" else "Crear perfiles de prueba",
            onClick = { vm.sembrarPerfilesDemo() },
            habilitado = !estado.sembrando,
        )
        Spacer(Modifier.height(8.dp))
        BotonContornoPaw(
            texto = "Eliminar perfiles de prueba",
            onClick = { vm.limpiarPerfilesDemo() },
            habilitado = !estado.sembrando,
            colorBorde = CoralPrimary,
            colorContenido = CoralPrimary,
        )
        estado.mensaje?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = CoralPrimary)
        }
    }
}

@Composable
private fun AdminModerationTab(estado: EstadoUiAdministracion, vm: ModeloVistaAdministracion) {
    if (estado.reportes.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = PeachPale,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Sin reportes pendientes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Cuando un usuario envíe un reporte o un ticket de soporte aparecerá aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(estado.reportes, key = { it.id }) { r ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceMuted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Gavel, null, tint = CoralPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Reporte · ${r.tipoObjetivo.name.lowercase()}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoralPrimary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Objetivo: ${r.idObjetivo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        r.motivo.ifBlank { "(Sin motivo)" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonContornoPaw(
                            texto = "Revisado",
                            onClick = { vm.resolverReporte(r.id, false) },
                            colorBorde = CoralPrimary,
                            colorContenido = CoralPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        BotonPrimarioPaw(
                            texto = "Acción tomada",
                            onClick = { vm.resolverReporte(r.id, true) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Maqueta 11 — Ajustes: notificaciones, privacidad y cuenta.
 * Los cambios de correo/contraseña se aplican vía Supabase Auth.
 */
@Composable
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
                titulo = "Coincidencias",
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
                    // Si el usuario intenta DESACTIVAR la privacidad (= compartir siempre),
                    // enseñamos un aviso con la información de qué se comparte.
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
                titulo = "Eliminar cuenta",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Text(titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    val valid = contrasena.length >= 6 && confirmation.trim().equals(requiredPhrase, ignoreCase = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar cuenta", color = Error) },
        text = {
            Column {
                Text(
                    "Esta acción es permanente. Se borrarán tu perfil, el perfil de tu " +
                        "perro y el acceso a PawPals. Tus mensajes y coincidencias quedarán " +
                        "inaccesibles para otros usuarios.",
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
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                    ),
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
                Text("Eliminar cuenta", color = Error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
}

/** Botón con estilo primary reutilizado de design system (no usado aquí, pero expuesto por ergonomía). */
@Composable
@Suppress("UnusedPrivateMember")
private fun CtaPrincipal(texto: String, onClick: () -> Unit, habilitado: Boolean = true) {
    BotonPrimarioPaw(texto = texto, onClick = onClick, habilitado = habilitado)
}

/**
 * Splash/Intro (maqueta 01): gradiente coral + logo + CTAs.
 * Se muestra solo la primera vez (flag en DataStore).
 */
@Composable
fun PantallaBienvenida(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CoralGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(36.dp),
                color = OnCoral,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.pawpals_logo),
                        contentDescription = "PawPals",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(28.dp)),
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Pawpals",
                color = OnCoral,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Encuentra compañero de paseo",
                color = OnCoral.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            PuntosProgresoPaw(
                total = 3,
                actual = 0,
                colorActivo = OnCoral,
                colorInactivo = OnCoral.copy(alpha = 0.4f),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OnCoral,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)),
            ) {
                BotonPrimarioPawOnGradient(onClick = onContinue)
            }
            BotonContornoPaw(
                texto = "Saber más",
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun BotonPrimarioPawOnGradient(onClick: () -> Unit) {
    // Botón blanco sobre fondo coral (reutiliza BotonPrimarioPaw con inversión de colores local)
    BotonPrimarioPaw(
        texto = "Comenzar",
        onClick = onClick,
        modifier = Modifier,
    )
}

/**
 * Usuario bloqueado por moderación (documento `usuarios/{uid}.bloqueado == true`).
 *
 * Ofrece un formulario para contactar con los administradoristradores: el mensaje se
 * guarda en `supportTickets` y cualquier administrador puede leerlo desde el panel.
 */
@Composable
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

/**
 * Maqueta 08 — Conversación individual con burbujas (incoming gris, outgoing coral).
 * Incluye menú contextual (reportar / deshacer coincidencia / añadir como amigo) y un
 * banner para aceptar/rechazar solicitudes de amistad entrantes.
 */
@Composable
fun PantallaConversacion(
    miUid: String,
    onBack: () -> Unit,
    onOpenReport: (tipoObjetivo: String, idObjetivo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaConversacion = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var borrador by remember { mutableStateOf("") }
    var mostrarDialogoDeshacer by remember { mutableStateOf(false) }
    var mostrarDialogoPerfil by remember { mutableStateOf(false) }
    val estadoLista = rememberLazyListState()

    LaunchedEffect(miUid) { viewModel.observar(miUid) }
    LaunchedEffect(estado.mensajes.size) {
        if (estado.mensajes.isNotEmpty()) {
            estadoLista.animateScrollToItem(estado.mensajes.lastIndex)
        }
    }
    LaunchedEffect(estado.coincidenciaDeshecha) {
        if (estado.coincidenciaDeshecha) onBack()
    }
    LaunchedEffect(estado.info, estado.error) {
        if (estado.info != null || estado.error != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.limpiarInfo()
        }
    }

    Column(modifier.fillMaxSize()) {
        CabeceraConversacion(
            otroUid = viewModel.otroUidVistaPrevia,
            nombreDueno = estado.nombreVisibleOtro,
            nombrePerro = estado.nombrePerroOtro,
            urlFoto = estado.urlFotoPerroOtro ?: estado.urlFotoUsuarioOtro,
            puedeAnadirAmigo = !estado.yaSonAmigos &&
                (estado.solicitudPendiente == null ||
                    estado.solicitudPendiente?.estado == EstadoSolicitudAmistad.RECHAZADA),
            onBack = onBack,
            alReportar = { onOpenReport("usuario", viewModel.otroUidVistaPrevia) },
            alPulsarAnadirAmigo = { viewModel.solicitarAmistad(miUid) },
            alDeshacerCoincidencia = { mostrarDialogoDeshacer = true },
            alAbrirPerfil = { mostrarDialogoPerfil = true },
        )

        // Banner solicitud pendiente: la aceptamos desde aquí sin salir del conversación.
        val req = estado.solicitudPendiente
        if (req != null && req.estado == EstadoSolicitudAmistad.PENDIENTE) {
            val soyDestinatario = req.uidDestino == miUid
            if (soyDestinatario) {
                IncomingSolicitudAmistadBanner(
                    onAccept = { viewModel.aceptarSolicitudAmistad() },
                    onReject = { viewModel.rechazarSolicitudAmistad() },
                )
            } else {
                Surface(color = PeachPale, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Solicitud de amistad enviada, esperando respuesta…",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralPrimary,
                    )
                }
            }
        }

        LazyColumn(
            state = estadoLista,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            items(estado.mensajes, key = { it.id }) { msg ->
                val mio = msg.uidRemitente == miUid
                BurbujaMensaje(
                    texto = msg.texto,
                    marcaTemporal = formatTime(msg.marcaTemporal),
                    mio = mio,
                )
            }
        }
        estado.info?.let {
            Text(
                it,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        estado.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = borrador,
                onValueChange = { borrador = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje…", color = TextSecondary) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceMuted,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = CoralPrimary,
                    cursorColor = CoralPrimary,
                ),
            )
            Surface(
                shape = CircleShape,
                color = CoralPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .clickable(enabled = borrador.isNotBlank() && !estado.enviando) {
                        viewModel.enviar(miUid, borrador)
                        borrador = ""
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = OnCoral,
                    )
                }
            }
        }
    }

    if (mostrarDialogoPerfil) {
        DialogoVistaPreviaPerfil(
            usuario = estado.perfilUsuarioOtro,
            perro = estado.perfilPerroOtro,
            nombreRespaldo = estado.nombrePerroOtro.ifBlank { estado.nombreVisibleOtro },
            onDismiss = { mostrarDialogoPerfil = false },
        )
    }

    if (mostrarDialogoDeshacer) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDeshacer = false },
            title = { Text("¿Deshacer coincidencia?") },
            text = {
                Text(
                    "Esta conversación desaparecerá de tus conversaciones y " +
                        "podréis volver a encontraros en Explorar.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoDeshacer = false
                    viewModel.deshacerCoincidencia(miUid)
                }) { Text("Deshacer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDeshacer = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun IncomingSolicitudAmistadBanner(
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(color = PeachPale, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Te ha enviado una solicitud de amistad",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CoralPrimary,
            )
            Text(
                "Si la aceptas, podréis ver la ubicación del otro cuando paseéis.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReject) {
                    Text("Rechazar", color = TextSecondary)
                }
                TextButton(onClick = onAccept) {
                    Text("Aceptar", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CabeceraConversacion(
    otroUid: String,
    nombreDueno: String,
    nombrePerro: String,
    urlFoto: String?,
    puedeAnadirAmigo: Boolean,
    onBack: () -> Unit,
    alReportar: () -> Unit,
    alPulsarAnadirAmigo: () -> Unit,
    alDeshacerCoincidencia: () -> Unit,
    alAbrirPerfil: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Row(
                // Todo el bloque avatar + nombre es una zona táctil para abrir
                // la vista previa del perfil del otro usuario.
                modifier = Modifier
                    .weight(1f)
                    .clickable { alAbrirPerfil() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    if (!urlFoto.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = urlFoto,
                            contentDescription = nombrePerro.ifBlank { nombreDueno },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    } else {
                        AvatarPaw(nombre = nombrePerro.ifBlank { nombreDueno.ifBlank { otroUid } }, tamano = 44.dp)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        nombrePerro.ifBlank { nombreDueno.ifBlank { "Amigo" } },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                    )
                    Text(
                        if (nombreDueno.isNotBlank()) "Con $nombreDueno · Ver perfil" else "Ver perfil",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false },
                ) {
                    if (puedeAnadirAmigo) {
                        DropdownMenuItem(
                            text = { Text("Añadir como amigo") },
                            onClick = {
                                menuAbierto = false
                                alPulsarAnadirAmigo()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Deshacer coincidencia") },
                        onClick = {
                            menuAbierto = false
                            alDeshacerCoincidencia()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Reportar usuario") },
                        onClick = {
                            menuAbierto = false
                            alReportar()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(
    texto: String,
    marcaTemporal: String,
    mio: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mio) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (mio) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Surface(
                color = if (mio) CoralPrimary else SurfaceMuted,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (mio) 20.dp else 4.dp,
                    bottomEnd = if (mio) 4.dp else 20.dp,
                ),
            ) {
                Text(
                    text = texto,
                    color = if (mio) OnCoral else TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                marcaTemporal,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Popup con vista previa del perfil del otro usuario (foto de persona + foto
 * del perro, datos básicos y contadores). Se abre al pulsar la cabecera del
 * conversación. No permite editar nada — es una "tarjeta de presentación" para saber
 * con quién se está hablando.
 */
@Composable
private fun DialogoVistaPreviaPerfil(
    usuario: PerfilUsuario?,
    perro: PerfilPerro?,
    nombreRespaldo: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = CoralPrimary) }
        },
        title = {
            Text(
                perro?.nombre?.takeIf { it.isNotBlank() }
                    ?: usuario?.nombreVisible?.takeIf { it.isNotBlank() }
                    ?: nombreRespaldo.ifBlank { "Perfil" },
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (usuario == null && perro == null) {
                    Text(
                        "Cargando perfil…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    return@Column
                }

                HeroFotoPerfil(
                    fotoPrincipal = perro?.urlFoto,
                    fotoSecundaria = usuario?.urlFoto,
                    nombreRespaldo = nombreRespaldo,
                )
                Spacer(Modifier.height(14.dp))

                perro?.let { d ->
                    Text(
                        d.nombre.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = TextPrimary,
                    )
                    Text(
                        buildString {
                            if (d.raza.isNotBlank()) append(d.raza)
                            if (d.edadAnios > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("${d.edadAnios} años")
                            }
                        }.ifBlank { "Sobre el perro" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsigniaAtributo(
                            texto = d.energia.label,
                            colorAcento = CoralPrimary,
                            fondoAcento = PeachPale,
                        )
                        InsigniaAtributo(
                            texto = d.sociabilidad.label,
                            colorAcento = InfoBlue,
                            fondoAcento = InfoSoft,
                        )
                    }
                    if (d.biografia.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            d.biografia,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                usuario?.let { u ->
                    ProfileTituloSeccion("Sobre el dueño")
                    Text(
                        u.nombreVisible.ifBlank { "Dueño" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = TextPrimary,
                    )
                    if (u.zona.isNotBlank()) {
                        Text(
                            u.zona,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    if (u.sobreMi.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            u.sobreMi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TarjetaEstadisticaPerfil(
                            etiqueta = "Amigos",
                            valor = u.numeroAmigos,
                            colorAcento = CoralPrimary,
                            fondoAcento = PeachPale,
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaEstadisticaPerfil(
                            etiqueta = "Paseos",
                            valor = u.numeroPaseos,
                            colorAcento = SuccessGreen,
                            fondoAcento = SuccessSoft,
                            modifier = Modifier.weight(1f),
                        )
                        TarjetaEstadisticaPerfil(
                            etiqueta = "Coincidencias",
                            valor = u.numeroCoincidencias,
                            colorAcento = InfoBlue,
                            fondoAcento = InfoSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun HeroFotoPerfil(
    fotoPrincipal: String?,
    fotoSecundaria: String?,
    nombreRespaldo: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FotoHeroPerfil(
            urlFoto = fotoPrincipal ?: fotoSecundaria,
            nombreRespaldo = nombreRespaldo,
            modifier = Modifier
                .weight(1.4f)
                .aspectRatio(1f),
        )
        if (!fotoSecundaria.isNullOrBlank() && fotoPrincipal != null && fotoSecundaria != fotoPrincipal) {
            FotoHeroPerfil(
                urlFoto = fotoSecundaria,
                nombreRespaldo = nombreRespaldo,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun FotoHeroPerfil(
    urlFoto: String?,
    nombreRespaldo: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        if (!urlFoto.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = urlFoto,
                contentDescription = nombreRespaldo,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            AvatarPaw(nombre = nombreRespaldo.ifBlank { "?" }, tamano = 72.dp)
        }
    }
}

@Composable
private fun ProfileTituloSeccion(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun TarjetaEstadisticaPerfil(
    etiqueta: String,
    valor: Int,
    colorAcento: Color,
    fondoAcento: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = fondoAcento,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                valor.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = colorAcento,
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun formatTime(ts: Long): String {
    if (ts <= 0) return ""
    return timeFormatter.format(Date(ts))
}

/**
 * Explorar perfiles (maqueta 05). Tarjeta estilo Tinder con acciones rechazar/super/me gusta.
 * Tras un me gusta se lanza una navegación al celebración de coincidencia (06).
 */
@Composable
fun PantallaExplorar(
    miUid: String,
    onOpenCoincidencia: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaExplorar = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarAvisoNotificaciones by remember { mutableStateOf(false) }

    LaunchedEffect(miUid) {
        viewModel.cargar(miUid)
    }
    LaunchedEffect(estado.ultimoUidCoincidencia) {
        estado.ultimoUidCoincidencia?.let {
            onOpenCoincidencia(it)
            viewModel.limpiarNavegacionCoincidencia()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                estado.cargando -> CircularProgressIndicator()
                !estado.hayMas -> EmptyExplorarState()
                else -> estado.actual?.let { card ->
                    VistaTarjetaExplorar(card)
                }
            }
        }

        estado.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionCircle(
                icon = Icons.Filled.Close,
                tint = TextSecondary,
                background = Color.White,
                border = OutlineSoft,
                habilitado = estado.hayMas,
                onClick = { viewModel.rechazar(miUid) },
            )
            ActionCircle(
                icon = Icons.Filled.Star,
                tint = InfoBlue,
                background = InfoSoft,
                border = InfoSoft,
                habilitado = estado.hayMas,
                bigger = true,
                onClick = { viewModel.superMeGusta(miUid) },
            )
            ActionCircle(
                icon = Icons.Filled.Favorite,
                tint = OnCoral,
                background = CoralPrimary,
                border = CoralPrimary,
                habilitado = estado.hayMas,
                onClick = { viewModel.meGusta(miUid) },
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
                    "Cuando alguien haga coincidencia contigo o te escriba, te avisaremos aquí. " +
                        "Asegúrate de tener las notificaciones activadas en Ajustes.",
                )
            },
        )
    }
}

@Composable
private fun ActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    habilitado: Boolean = true,
    bigger: Boolean = false,
) {
    val size = if (bigger) 64.dp else 56.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, border, CircleShape)
            .clickable(enabled = habilitado) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun VistaTarjetaExplorar(card: TarjetaExplorar) {
    val perro: PerfilPerro? = card.perro
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column {
            // Área de foto (placeholder gradiente con icono pata)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(PeachLight, PeachSecondary.copy(alpha = 0.7f)),
                        ),
                    ),
            ) {
                // Chips de atributos superpuestos
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

/**
 * Login y registro unificados en una sola pantalla con tabs (maqueta 02).
 * Mantiene la navegación externa por compatibilidad (onGoRegister) pero ya no es imprescindible.
 */
@Composable
fun PantallaInicioSesion(
    onGoRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaAutenticacion = hiltViewModel(),
) {
    var tab by remember { mutableStateOf(AuthTab.LOGIN) }
    var method by remember { mutableStateOf(AuthMethod.EMAIL) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    var correo by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Cabecera con gradiente coral
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(CoralGradientSoft),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OnCoral.copy(alpha = 0.95f),
                    modifier = Modifier.size(60.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.pawpals_logo),
                            contentDescription = "PawPals",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (tab == AuthTab.LOGIN) "Bienvenido" else "Crear cuenta",
                    color = OnCoral,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                    ),
                )
                Text(
                    text = if (tab == AuthTab.LOGIN)
                        "Inicia sesión para encontrar amigos de paseo"
                    else
                        "Regístrate con tu correo para empezar a pasear",
                    color = OnCoral.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Column(Modifier.padding(24.dp)) {
            AuthTabSwitcher(actual = tab, onSelect = { tab = it })
            Spacer(Modifier.height(20.dp))

            AuthMethodRow(
                method = method,
                onSelect = { method = it },
            )
            Spacer(Modifier.height(16.dp))

            when (method) {
                AuthMethod.EMAIL -> {
                    CampoContornoPaw(
                        valor = correo,
                        alCambiarValor = { correo = it },
                        etiqueta = "Correo electrónico",
                        keyboardType = KeyboardType.Email,
                        marcador = "usuario@correo.com",
                    )
                }
                AuthMethod.PHONE -> {
                    CampoContornoPaw(
                        valor = phone,
                        alCambiarValor = { phone = it },
                        etiqueta = "Número de teléfono",
                        keyboardType = KeyboardType.Phone,
                        marcador = "+34 600 00 00 00",
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SMS no disponible en la demo: usa el acceso por correo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            CampoContornoPaw(
                valor = contrasena,
                alCambiarValor = { contrasena = it },
                etiqueta = "Contraseña",
                visualTransformation = PasswordVisualTransformation(),
            )

            if (tab == AuthTab.LOGIN) {
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        "¿Olvidaste tu contraseña?",
                        color = CoralPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }

            estado.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            estado.info?.let { info ->
                Text(info, color = CoralPrimary)
                Spacer(Modifier.height(8.dp))
            }

            if (estado.cargando) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                BotonPrimarioPaw(
                    texto = if (tab == AuthTab.LOGIN) "Iniciar sesión" else "Crear cuenta",
                    onClick = {
                        if (method != AuthMethod.EMAIL) return@BotonPrimarioPaw
                        if (tab == AuthTab.LOGIN) {
                            viewModel.iniciarSesion(correo, contrasena)
                        } else {
                            viewModel.registrar(correo, contrasena)
                        }
                    },
                    habilitado = method == AuthMethod.EMAIL &&
                        correo.isNotBlank() && contrasena.length >= 6,
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        tab = if (tab == AuthTab.LOGIN) AuthTab.REGISTER else AuthTab.LOGIN
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (tab == AuthTab.LOGIN)
                        "¿No tienes cuenta? "
                    else
                        "¿Ya tienes cuenta? ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (tab == AuthTab.LOGIN) "Crear cuenta" else "Iniciar sesión",
                    color = CoralPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Demo de administración: ${BuildConfig.ADMIN_EMAIL}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showResetDialog) {
        PasswordResetDialog(
            initialEmail = correo,
            cargando = estado.cargando,
            onDismiss = {
                showResetDialog = false
                viewModel.limpiarRetroalimentacion()
            },
            onSend = { addr -> viewModel.restablecerContrasena(addr) },
        )
    }
}

@Composable
private fun PasswordResetDialog(
    initialEmail: String,
    cargando: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var addr by remember { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restablecer contraseña") },
        text = {
            Column {
                Text(
                    "Introduce tu correo y te enviaremos un enlace para restablecer la contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                CampoContornoPaw(
                    valor = addr,
                    alCambiarValor = { addr = it },
                    etiqueta = "Correo electrónico",
                    keyboardType = KeyboardType.Email,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !cargando && addr.isNotBlank(),
                onClick = {
                    onSend(addr.trim())
                    onDismiss()
                },
            ) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private enum class AuthTab { LOGIN, REGISTER }
private enum class AuthMethod { EMAIL, PHONE }

@Composable
private fun AuthTabSwitcher(actual: AuthTab, onSelect: (AuthTab) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceMuted,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TabPill(
                texto = "Iniciar sesión",
                seleccionado = actual == AuthTab.LOGIN,
                onClick = { onSelect(AuthTab.LOGIN) },
                modifier = Modifier.weight(1f),
            )
            TabPill(
                texto = "Crear cuenta",
                seleccionado = actual == AuthTab.REGISTER,
                onClick = { onSelect(AuthTab.REGISTER) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabPill(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (seleccionado) Color.White else Color.Transparent
    val textColor = if (seleccionado) CoralPrimary else TextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun AuthMethodRow(method: AuthMethod, onSelect: (AuthMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MethodCard(
            icon = { Icon(Icons.Filled.Email, null, tint = CoralPrimary) },
            titulo = "Correo electrónico",
            subtitulo = "Recibirás un correo de verificación",
            seleccionado = method == AuthMethod.EMAIL,
            onClick = { onSelect(AuthMethod.EMAIL) },
        )
        MethodCard(
            icon = { Icon(Icons.Filled.Phone, null, tint = CoralPrimary) },
            titulo = "Teléfono",
            subtitulo = "Recibirás un SMS de verificación",
            seleccionado = method == AuthMethod.PHONE,
            onClick = { onSelect(AuthMethod.PHONE) },
        )
    }
}

@Composable
private fun MethodCard(
    icon: @Composable () -> Unit,
    titulo: String,
    subtitulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (seleccionado) PeachPale else Color.White,
        border = BorderStroke(1.5.dp, if (seleccionado) CoralPrimary else OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    color = if (seleccionado) CoralPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    subtitulo,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Maqueta 07 — Lista de conversaciones (conversaciones) con búsqueda.
 */
@Composable
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
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
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
                        marcaTemporal = formatRelative(entry.coincidencia.creadoEn),
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
            // Línea principal: nombre del perro; si aún no está cargado, el
            // del dueño. Si tampoco lo tenemos, un placeholder amable en vez
            // del uid técnico.
            val title = entry.nombrePerro.ifBlank {
                entry.nombreDueno.ifBlank { "Amigo" }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            val subtitle = buildString {
                if (entry.nombreDueno.isNotBlank()) append("Con ${entry.nombreDueno}")
                else append("Toca para abrir la conversación")
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            marcaTemporal,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
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

/** Formateo ligero del timestamp del coincidencia (hh:mm si es hoy, dd/MM si es antes). */
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
            "Haz una coincidencia en Explorar y rompe el hielo con un hola.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

/**
 * Maqueta 09 — Mapa de paseos en vivo. Muestra amigos paseando y permite iniciar paseo propio.
 * Gestiona por sí misma la solicitud de permisos y el estado de carga con timeout.
 */
@Composable
fun PantallaMapa(
    miUid: String,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaMapa = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var permisoConcedido by remember { mutableStateOf(viewModel.tienePermisoUbicacion()) }

    val lanzadorUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val concedido = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permisoConcedido = concedido
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
                    lanzadorUbicacion.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
                estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                estado.miUbicacion != null && BuildConfig.MAPS_API_KEY.isNotBlank() -> {
                    val cam = rememberCameraPositionState()
                    LaunchedEffect(estado.miUbicacion) {
                        estado.miUbicacion?.let { cam.animate(CameraUpdateFactory.newLatLngZoom(it, 14f)) }
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cam,
                    ) {
                        val yo = estado.miUbicacion ?: return@GoogleMap
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
                estado.miUbicacion == null -> {
                    LocationUnavailableCTA(
                        mensaje = estado.mensaje ?: "No se pudo obtener la ubicación.",
                        onRetry = { viewModel.cargarCercanos(miUid) },
                    )
                }
                else -> {
                    Text(
                        "Configura MAPS_API_KEY en local.properties para ver el mapa interactivo.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
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
            onClick = { viewModel.alternarPaseo(miUid) },
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
                if (estado.yoPaseando) "Finalizar paseo" else "Iniciar paseo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
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
            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary, contentColor = OnCoral),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Activar ubicación") }
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
            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary, contentColor = OnCoral),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Reintentar") }
    }
}

/**
 * Maqueta 10 — Perfil del usuario con cabecera en gradiente, estadísticas, perro y toggles.
 */
@Composable
fun PantallaPerfil(
    miUid: String,
    onOpenSettings: () -> Unit,
    onOpenReport: (tipoObjetivo: String, idObjetivo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaPerfil = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(miUid) { viewModel.observar(miUid) }
    LaunchedEffect(estado.mensaje) {
        if (estado.mensaje != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.limpiarMensaje()
        }
    }

    val pickUserPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.actualizarFotoUsuario(miUid, uri) }
    val pickDogPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.actualizarFotoPerro(miUid, uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeader(
            nombreVisible = estado.nombreVisible.ifBlank { "Usuario" },
            zona = estado.zona,
            nombrePerro = estado.nombrePerro,
            urlFoto = estado.usuario?.urlFoto,
            onEdit = { viewModel.alternarEdicion() },
            onSettings = onOpenSettings,
            onChangePhoto = {
                pickUserPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(56.dp))
            FilaEstadisticas(
                amigos = estado.usuario?.numeroAmigos ?: 0,
                paseos = estado.usuario?.numeroPaseos ?: 0,
                coincidencias = estado.usuario?.numeroCoincidencias ?: 0,
            )
            Spacer(Modifier.height(20.dp))

            if (estado.editando) {
                EditingForm(perfilUi = estado, viewModel = viewModel, uid = miUid)
            } else {
                MyDogCard(
                    nombrePerro = estado.nombrePerro,
                    raza = estado.razaPerro,
                    edad = estado.edadPerro,
                    etiquetaEnergia = estado.energia.label,
                    etiquetaSociabilidad = estado.sociabilidad.label,
                    urlFoto = estado.perro?.urlFoto,
                    onChangePhoto = {
                        pickDogPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    "Notificaciones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                PrefToggle(
                    label = "Mensajes",
                    checked = estado.notificarMensajes,
                    onToggle = { viewModel.alternarNotificarMensajes() },
                )
                PrefToggle(
                    label = "Paseos",
                    checked = estado.notificarPaseos,
                    onToggle = { viewModel.alternarNotificarPaseos() },
                )
                Spacer(Modifier.height(20.dp))

                BotonPrimarioPaw(
                    texto = "Reportar un problema",
                    onClick = { onOpenReport("usuario", miUid) },
                )
            }

            estado.mensaje?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = CoralPrimary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    nombreVisible: String,
    zona: String,
    nombrePerro: String,
    urlFoto: String?,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onChangePhoto: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(CoralGradientSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = OnCoral)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OnCoral.copy(alpha = 0.9f),
                modifier = Modifier.clickable { onEdit() },
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Edit, null, tint = CoralPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Editar",
                        color = CoralPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 48.dp),
        ) {
            Text(
                nombreVisible,
                color = OnCoral,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = OnCoral, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(
                    buildString {
                        append(zona.ifBlank { "Ubicación pendiente" })
                        if (nombrePerro.isNotBlank()) append(" · Dueño/a de $nombrePerro")
                    },
                    color = OnCoral.copy(alpha = 0.95f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        // Avatar overlay (tap para cambiar foto).
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 24.dp, y = 40.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Color.White, CircleShape)
                .clickable { onChangePhoto() },
            contentAlignment = Alignment.Center,
        ) {
            if (!urlFoto.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = urlFoto,
                    contentDescription = nombreVisible,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.Pets, null, tint = CoralPrimary, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun FilaEstadisticas(amigos: Int, paseos: Int, coincidencias: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatItem(valor = amigos.toString(), etiqueta = "Amigos", modifier = Modifier.weight(1f))
        StatItem(valor = paseos.toString(), etiqueta = "Paseos", modifier = Modifier.weight(1f))
        StatItem(valor = coincidencias.toString(), etiqueta = "Coincidencias", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SurfaceMuted,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun MyDogCard(
    nombrePerro: String,
    raza: String,
    edad: String,
    etiquetaEnergia: String,
    etiquetaSociabilidad: String,
    urlFoto: String?,
    onChangePhoto: () -> Unit,
) {
    Text(
        "Mi perro",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = TextPrimary,
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMuted),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PeachPale)
                    .clickable { onChangePhoto() },
                contentAlignment = Alignment.Center,
            ) {
                if (!urlFoto.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = urlFoto,
                        contentDescription = nombrePerro,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.Pets, null, tint = CoralPrimary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nombrePerro.ifBlank { "Aún sin perro" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    buildString {
                        if (raza.isNotBlank()) append(raza)
                        if (edad.isNotBlank()) append(" · $edad años")
                    }.ifBlank { "Completa su perfil para conectar con otros" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (nombrePerro.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InsigniaAtributo(
                            texto = etiquetaEnergia,
                            colorAcento = AcentosPaw.colorEnergia,
                            fondoAcento = AcentosPaw.fondoEnergia,
                        )
                        InsigniaAtributo(
                            texto = etiquetaSociabilidad,
                            colorAcento = AcentosPaw.colorSocial,
                            fondoAcento = AcentosPaw.fondoSocial,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (label.contains("Mensaje")) InfoSoft else SuccessSoft,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (label.contains("Mensaje")) {
                        Icons.Filled.Edit
                    } else {
                        Icons.Filled.LocationOn
                    },
                    contentDescription = null,
                    tint = if (label.contains("Mensaje")) InfoBlue else SuccessGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
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
private fun EditingForm(
    perfilUi: EstadoUiPerfil,
    viewModel: ModeloVistaPerfil,
    uid: String,
) {
    Column {
        Text(
            "Edita tu perfil",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        CampoContornoPaw(
            valor = perfilUi.nombreVisible,
            alCambiarValor = viewModel::alCambiarNombreVisible,
            etiqueta = "Nombre",
        )
        Spacer(Modifier.height(8.dp))
        CampoContornoPaw(
            valor = perfilUi.zona,
            alCambiarValor = viewModel::alCambiarZona,
            etiqueta = "Zona / Barrio",
        )
        Spacer(Modifier.height(8.dp))
        CampoContornoPaw(
            valor = perfilUi.sobreMi,
            alCambiarValor = viewModel::alCambiarSobreMi,
            etiqueta = "Sobre ti",
            unaLinea = false,
        )
        Spacer(Modifier.height(12.dp))
        Text("Tu perro", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        CampoContornoPaw(
            valor = perfilUi.nombrePerro,
            alCambiarValor = viewModel::alCambiarNombrePerro,
            etiqueta = "Nombre del perro",
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoContornoPaw(
                valor = perfilUi.razaPerro,
                alCambiarValor = viewModel::alCambiarRazaPerro,
                etiqueta = "Raza",
                modifier = Modifier.weight(1f),
            )
            CampoContornoPaw(
                valor = perfilUi.edadPerro,
                alCambiarValor = viewModel::alCambiarEdadPerro,
                etiqueta = "Edad",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        CampoContornoPaw(
            valor = perfilUi.bioPerro,
            alCambiarValor = viewModel::alCambiarBioPerro,
            etiqueta = "Descripción del perro",
            unaLinea = false,
        )
        Spacer(Modifier.height(16.dp))
        BotonPrimarioPaw(
            texto = if (perfilUi.guardandoPerfil) "Guardando…" else "Guardar cambios",
            onClick = { viewModel.guardarPerfil(uid) },
            habilitado = !perfilUi.guardandoPerfil,
        )
        Spacer(Modifier.height(10.dp))
        BotonContornoPaw(
            texto = "Cancelar",
            onClick = { viewModel.alternarEdicion() },
            colorBorde = CoralPrimary,
            colorContenido = CoralPrimary,
            habilitado = !perfilUi.guardandoPerfil,
        )
    }
}

/**
 * Registro → redirige al PantallaInicioSesion (la maqueta 02 unifica login + registro con tabs).
 * Se mantiene la ruta "registro" por compatibilidad con navegación existente.
 */
@Composable
fun PantallaRegistro(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaInicioSesion(onGoRegister = onBack, modifier = modifier)
}

@Composable
fun PantallaReporte(
    miUid: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaReporte = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var motivo by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        BarraSuperiorPaw(titulo = "Reportar contenido", alVolver = onBack)
        Text(
            text = viewModel.etiquetaObjetivo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        CampoContornoPaw(
            valor = motivo,
            alCambiarValor = { motivo = it },
            etiqueta = "Describe el motivo",
            unaLinea = false,
        )
        Spacer(Modifier.height(16.dp))
        if (estado.completado) {
            Text("Gracias, el equipo revisará el reporte.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            BotonPrimarioPaw(texto = "Volver", onClick = onDone)
        } else {
            BotonPrimarioPaw(
                texto = if (estado.enviando) "Enviando…" else "Enviar reporte",
                habilitado = !estado.enviando && motivo.isNotBlank(),
                onClick = { viewModel.enviar(miUid, motivo) },
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Pantalla de celebración de coincidencia (maqueta 06).
 * Carga el dueño y el perro vía [ModeloVistaResultadoCoincidencia] y compone el botón
 * principal con el nombre del perro ("Enviar mensaje a <Perro>").
 */
@Composable
fun PantallaResultadoCoincidencia(
    otroUid: String,
    onSendMessage: () -> Unit,
    onKeepExploring: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaResultadoCoincidencia = hiltViewModel(),
) {
    LaunchedEffect(otroUid) { viewModel.cargar(otroUid) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val ctaName = estado.nombrePerro.ifBlank { estado.nombreOtroUsuario.ifBlank { "tu nuevo amigo" } }

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
                "¡Es un Coincidencia!",
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
                modifier = Modifier.size(100.dp).clip(CircleShape),
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

/**
 * Maqueta 03 — Onboarding del perfil humano.
 */
@Composable
fun OnboardingHumanScreen(
    onNext: () -> Unit,
    viewModel: ModeloVistaConfiguracionPerfil,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()

    // PhotoPicker nativo (Android 13+) con fallback automático a intent clásico.
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.establecerFotoHumano(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        OnboardingTopBar(onBack = onBack, onSignOut = onSignOut)
        Spacer(Modifier.height(12.dp))
        CabeceraPaso(actual = 0, total = 4)
        Spacer(Modifier.height(20.dp))
        Text(
            "Cuéntanos sobre ti",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            "Tu perfil ayuda a otros dueños a conocerte",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SelectorFotoPaw(
                etiqueta = if (s.uriFotoHumano == null) "Subir\nfoto" else "Cambiar\nfoto",
                modeloImagen = s.uriFotoHumano,
                onClick = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
        Spacer(Modifier.height(24.dp))

        CampoContornoPaw(
            valor = s.nombreHumano,
            alCambiarValor = viewModel::establecerNombreHumano,
            etiqueta = "Tu nombre",
            marcador = "María García",
        )
        Spacer(Modifier.height(10.dp))
        CampoContornoPaw(
            valor = s.zona,
            alCambiarValor = viewModel::establecerZona,
            etiqueta = "Zona / Barrio",
            marcador = "Malasaña, Madrid",
        )
        Spacer(Modifier.height(10.dp))
        CampoContornoPaw(
            valor = s.sobreMi,
            alCambiarValor = viewModel::establecerSobreMi,
            etiqueta = "Sobre ti",
            unaLinea = false,
            marcador = "Dueña de Luna desde hace 3 años…",
        )
        Spacer(Modifier.height(32.dp))
        BotonPrimarioPaw(
            texto = "Siguiente",
            onClick = {
                viewModel.pasoSiguiente()
                onNext()
            },
            habilitado = viewModel.puedeContinuarHumano(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Maqueta 04 — Onboarding del perfil del perro (chips de energía y sociabilidad).
 */
@Composable
fun OnboardingDogScreen(
    miUid: String,
    onFinish: () -> Unit,
    viewModel: ModeloVistaConfiguracionPerfil,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.establecerFotoPerroOnboarding(uri) }
    LaunchedEffect(s.completado) {
        if (s.completado) onFinish()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        OnboardingTopBar(onBack = onBack, onSignOut = onSignOut)
        Spacer(Modifier.height(12.dp))
        CabeceraPaso(actual = 1, total = 4)
        Spacer(Modifier.height(20.dp))
        Text(
            "Ahora tu peludo",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            "Encontraremos el compañero ideal para él",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SelectorFotoPaw(
                etiqueta = if (s.fotoPerroUri == null) "Foto" else "Cambiar",
                modeloImagen = s.fotoPerroUri,
                onClick = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
        Spacer(Modifier.height(24.dp))

        CampoContornoPaw(
            valor = s.nombrePerro,
            alCambiarValor = viewModel::establecerNombrePerroOnboarding,
            etiqueta = "Nombre del perro",
            marcador = "Luna",
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoContornoPaw(
                valor = s.razaPerro,
                alCambiarValor = viewModel::establecerRazaPerro,
                etiqueta = "Raza",
                marcador = "Labrador",
                modifier = Modifier.weight(1f),
            )
            CampoContornoPaw(
                valor = s.edadPerro,
                alCambiarValor = viewModel::establecerEdadPerro,
                etiqueta = "Edad",
                marcador = "3 años",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(
            "Nivel de energía",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        ChipsEnergia(actual = s.energia, onSelect = viewModel::establecerEnergia)

        Spacer(Modifier.height(16.dp))
        Text(
            "Sociabilidad",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        ChipsSociabilidad(actual = s.sociabilidad, onSelect = viewModel::establecerSociabilidad)

        Spacer(Modifier.height(24.dp))
        s.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        BotonPrimarioPaw(
            texto = if (s.guardando) "Guardando…" else "Siguiente",
            onClick = { viewModel.finalizarOnboarding(miUid) },
            habilitado = viewModel.puedeFinalizarOnboarding() && !s.guardando,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ChipsEnergia(actual: NivelEnergia, onSelect: (NivelEnergia) -> Unit) {
    FlowRowCompat {
        NivelEnergia.values().forEach { nivel ->
            ChipPaw(
                texto = nivel.label,
                seleccionado = actual == nivel,
                onClick = { onSelect(nivel) },
                iconoInicio = {
                    Icon(Icons.Filled.Bolt, null, tint = CoralPrimary)
                },
            )
        }
    }
}

@Composable
private fun ChipsSociabilidad(actual: Sociabilidad, onSelect: (Sociabilidad) -> Unit) {
    FlowRowCompat {
        Sociabilidad.values().forEach { soc ->
            ChipPaw(
                texto = soc.label,
                seleccionado = actual == soc,
                onClick = { onSelect(soc) },
            )
        }
    }
}

/** FlowRow estable de Compose (evita depender de accompanist). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(contenido: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { contenido() }
}

@Composable
private fun OnboardingTopBar(
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
) {
    if (onBack == null && onSignOut == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextSecondary,
                )
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        if (onSignOut != null) {
            TextButton(onClick = onSignOut) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = TextSecondary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "Cerrar sesión",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CabeceraPaso(actual: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until total) {
            val activo = i <= actual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (activo) CoralPrimary else OutlineSoft),
            )
        }
    }
}

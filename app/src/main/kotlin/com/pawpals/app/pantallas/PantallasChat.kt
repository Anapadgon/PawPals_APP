package com.pawpals.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
// chat entre dos usuarios, con acciones de amistad y reporte
fun PantallaConversacion(
    miUid: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaConversacion = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var borrador by remember { mutableStateOf("") }
    var mostrarDialogoDeshacer by remember { mutableStateOf(false) }
    var mostrarDialogoPerfil by remember { mutableStateOf(false) }
    var mostrarDialogoReporte by remember { mutableStateOf(false) }
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
                    estado.solicitudPendiente?.estado != EstadoSolicitudAmistad.PENDIENTE,
            yaSonAmigos = estado.yaSonAmigos,
            onBack = onBack,
            alReportar = { mostrarDialogoReporte = true },
            alPulsarAnadirAmigo = { viewModel.solicitarAmistad(miUid) },
            alEliminarAmigo = { viewModel.eliminarAmigo(miUid) },
            alDeshacerCoincidencia = { mostrarDialogoDeshacer = true },
            alAbrirPerfil = { mostrarDialogoPerfil = true },
        )

        // si llega una solicitud, se puede responder sin salir del chat
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
    if (mostrarDialogoReporte) {
        DialogoReportarUsuario(
            onDismiss = { mostrarDialogoReporte = false },
            onConfirm = { motivo ->
                mostrarDialogoReporte = false
                viewModel.reportarUsuario(miUid, motivo)
            },
        )
    }

    if (mostrarDialogoDeshacer) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDeshacer = false },
            title = { Text("¿Deshacer Match?") },
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
private fun DialogoReportarUsuario(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val motivos = listOf("Perfil falso", "Comportamiento inadecuado", "Spam")
    var motivoElegido by remember { mutableStateOf(motivos.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                motivos.forEach { motivo ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { motivoElegido = motivo },
                        shape = RoundedCornerShape(12.dp),
                        color = if (motivoElegido == motivo) PeachPale else SurfaceMuted,
                    ) {
                        Text(
                            motivo,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (motivoElegido == motivo) CoralPrimary else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(motivoElegido) }) {
                Text("Enviar", color = CoralPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        },
    )
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
    yaSonAmigos: Boolean,
    onBack: () -> Unit,
    alReportar: () -> Unit,
    alPulsarAnadirAmigo: () -> Unit,
    alEliminarAmigo: () -> Unit,
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
                // el avatar y el nombre abren la ficha del perfil
                // la vista previa del perfil del otro usuario
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
                        AvatarPaw(
                            nombre = nombrePerro.ifBlank { nombreDueno.ifBlank { otroUid } },
                            tamano = 44.dp
                        )
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
                    if (yaSonAmigos) {
                        DropdownMenuItem(
                            text = { Text("Deshacer Amistad") },
                            onClick = {
                                menuAbierto = false
                                alEliminarAmigo()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Deshacer Match") },
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

// vista rapida del perfil de la otra persona dentro del chat
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

package com.pawpals.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
// perfil propio con datos del usuario, perro y estadisticas
fun PantallaPerfil(
    miUid: String,
    onOpenSettings: () -> Unit,
    onOpenReport: (tipoObjetivo: String, idObjetivo: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaPerfil = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mostrarOrigenUsuario by remember { mutableStateOf(false) }
    var mostrarOrigenPerro by remember { mutableStateOf(false) }
    var uriCamaraUsuario by remember { mutableStateOf<android.net.Uri?>(null) }
    var uriCamaraPerro by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(miUid) { viewModel.observar(miUid) }
    LaunchedEffect(estado.mensaje) {
        if (estado.mensaje != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.limpiarMensaje()
        }
    }

    val seleccionarFotoUsuario = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.actualizarFotoUsuario(miUid, uri) }
    val seleccionarFotoPerro = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.actualizarFotoPerro(miUid, uri) }
    val takeUserPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { tomada ->
        if (tomada) uriCamaraUsuario?.let { viewModel.actualizarFotoUsuario(miUid, it) }
    }
    val takeDogPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { tomada ->
        if (tomada) uriCamaraPerro?.let { viewModel.actualizarFotoPerro(miUid, it) }
    }
    val permisoCamaraUsuario = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            val uri = crearUriFotoTemporal(context)
            uriCamaraUsuario = uri
            takeUserPhoto.launch(uri)
        }
    }
    val permisoCamaraPerro = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            val uri = crearUriFotoTemporal(context)
            uriCamaraPerro = uri
            takeDogPhoto.launch(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CabeceraPerfil(
            nombreVisible = estado.nombreVisible.ifBlank { "Usuario" },
            zona = estado.zona,
            nombrePerro = estado.nombrePerro,
            urlFoto = estado.usuario?.urlFoto,
            onEdit = { viewModel.alternarEdicion() },
            onSettings = onOpenSettings,
            onChangePhoto = {
                mostrarOrigenUsuario = true
            },
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(56.dp))
            FilaEstadisticas(
                amigos = estado.numeroAmigos,
                paseos = estado.usuario?.numeroPaseos ?: 0,
                coincidencias = estado.usuario?.numeroCoincidencias ?: 0,
            )
            Spacer(Modifier.height(20.dp))

            if (estado.editando) {
                EditingForm(perfilUi = estado, viewModel = viewModel, uid = miUid)
            } else {
                TarjetaMiPerro(
                    nombrePerro = estado.nombrePerro,
                    raza = estado.razaPerro,
                    edad = estado.edadPerro,
                    etiquetaEnergia = estado.energia.label,
                    etiquetaSociabilidad = estado.sociabilidad.label,
                    urlFoto = estado.perro?.urlFoto,
                    onChangePhoto = {
                        mostrarOrigenPerro = true
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

    if (mostrarOrigenUsuario) {
        DialogoOrigenFotoPaw(
            onDismiss = { mostrarOrigenUsuario = false },
            onCamara = {
                mostrarOrigenUsuario = false
                permisoCamaraUsuario.launch(Manifest.permission.CAMERA)
            },
            onGaleria = {
                mostrarOrigenUsuario = false
                seleccionarFotoUsuario.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
    if (mostrarOrigenPerro) {
        DialogoOrigenFotoPaw(
            onDismiss = { mostrarOrigenPerro = false },
            onCamara = {
                mostrarOrigenPerro = false
                permisoCamaraPerro.launch(Manifest.permission.CAMERA)
            },
            onGaleria = {
                mostrarOrigenPerro = false
                seleccionarFotoPerro.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
private fun CabeceraPerfil(
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
                    Icon(
                        Icons.Filled.Edit,
                        null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(16.dp)
                    )
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
        // avatar encima de la cabecera para cambiar foto
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
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
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
        ItemEstadistica(
            valor = amigos.toString(),
            etiqueta = "Amigos",
            modifier = Modifier.weight(1f)
        )
        ItemEstadistica(
            valor = paseos.toString(),
            etiqueta = "Paseos",
            modifier = Modifier.weight(1f)
        )
        ItemEstadistica(
            valor = coincidencias.toString(),
            etiqueta = "Matches",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ItemEstadistica(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
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
private fun TarjetaMiPerro(
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
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
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

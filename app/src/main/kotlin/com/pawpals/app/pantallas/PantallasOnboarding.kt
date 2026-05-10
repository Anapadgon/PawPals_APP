package com.pawpals.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
// primera pantalla para usuarios sin sesion cuando aun no han visto la intro
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
                text = "PawPals",
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
    // boton blanco sobre el fondo coral
    BotonPrimarioPaw(
        texto = "Comenzar",
        onClick = onClick,
        modifier = Modifier,
    )
}

@Composable
// primer paso del onboarding, datos de la persona
fun PantallaOnboardingHumano(
    onNext: () -> Unit,
    viewModel: ModeloVistaConfiguracionPerfil,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mostrarOrigenFoto by remember { mutableStateOf(false) }
    var uriCamara by remember { mutableStateOf<android.net.Uri?>(null) }

    // selector de foto del sistema
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.establecerFotoHumano(uri) }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { tomada ->
        if (tomada) uriCamara?.let { viewModel.establecerFotoHumano(it) }
    }
    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            val uri = crearUriFotoTemporal(context)
            uriCamara = uri
            takePhoto.launch(uri)
        }
    }

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
                    mostrarOrigenFoto = true
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

    if (mostrarOrigenFoto) {
        DialogoOrigenFotoPaw(
            onDismiss = { mostrarOrigenFoto = false },
            onCamara = {
                mostrarOrigenFoto = false
                permisoCamara.launch(Manifest.permission.CAMERA)
            },
            onGaleria = {
                mostrarOrigenFoto = false
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
// segundo paso del onboarding, datos del perro
fun PantallaOnboardingPerro(
    miUid: String,
    onFinish: () -> Unit,
    viewModel: ModeloVistaConfiguracionPerfil,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mostrarOrigenFoto by remember { mutableStateOf(false) }
    var uriCamara by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.establecerFotoPerroOnboarding(uri) }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { tomada ->
        if (tomada) uriCamara?.let { viewModel.establecerFotoPerroOnboarding(it) }
    }
    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            val uri = crearUriFotoTemporal(context)
            uriCamara = uri
            takePhoto.launch(uri)
        }
    }
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
                    mostrarOrigenFoto = true
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

    if (mostrarOrigenFoto) {
        DialogoOrigenFotoPaw(
            onDismiss = { mostrarOrigenFoto = false },
            onCamara = {
                mostrarOrigenFoto = false
                permisoCamara.launch(Manifest.permission.CAMERA)
            },
            onGaleria = {
                mostrarOrigenFoto = false
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
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

// coloca chips en varias lineas sin usar otra libreria
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

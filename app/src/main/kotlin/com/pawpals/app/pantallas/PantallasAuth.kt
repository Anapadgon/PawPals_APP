package com.pawpals.app

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// login y registro, los terminos se muestran aqui porque se aceptan antes de crear cuenta
@Composable
// formulario de acceso y enlace al registro
fun PantallaInicioSesion(
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaAutenticacion = hiltViewModel(),
) {
    var tab by remember { mutableStateOf(AuthTab.LOGIN) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var aceptaLegal by remember { mutableStateOf(false) }
    var textoLegalAbierto by remember { mutableStateOf<TextoLegal?>(null) }

    LaunchedEffect(estado.cambiarAPestanaInicioSesion) {
        if (estado.cambiarAPestanaInicioSesion) {
            tab = AuthTab.LOGIN
            viewModel.consumirCambioPestanaTrasRegistro()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // cabecera con el color principal de la app
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

            CampoContornoPaw(
                valor = correo,
                alCambiarValor = { correo = it },
                etiqueta = "Correo electrónico",
                keyboardType = KeyboardType.Email,
                marcador = "usuario@correo.com",
            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(
                        checked = aceptaLegal,
                        onCheckedChange = { aceptaLegal = it },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Confirmo que soy mayor de 18 años y acepto los",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Row {
                            Text(
                                "Términos y Condiciones",
                                modifier = Modifier.clickable {
                                    textoLegalAbierto = TextoLegal.TERMINOS
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CoralPrimary,
                            )
                            Text(
                                " y la ",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                "Política de Privacidad",
                                modifier = Modifier.clickable {
                                    textoLegalAbierto = TextoLegal.PRIVACIDAD
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CoralPrimary,
                            )
                        }
                    }
                }
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
                    texto = if (tab == AuthTab.LOGIN) "Iniciar sesión" else "Registrarse",
                    onClick = {
                        if (tab == AuthTab.LOGIN) {
                            viewModel.iniciarSesion(correo, contrasena)
                        } else {
                            viewModel.registrar(correo, contrasena)
                        }
                    },
                    habilitado = correo.isNotBlank() && contrasena.length >= 6 &&
                            (tab == AuthTab.LOGIN || aceptaLegal),
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
    textoLegalAbierto?.let { texto ->
        DialogoTextoLegal(
            textoLegal = texto,
            onDismiss = { textoLegalAbierto = null },
        )
    }
}

private enum class TextoLegal { TERMINOS, PRIVACIDAD }

@Composable
private fun DialogoTextoLegal(
    textoLegal: TextoLegal,
    onDismiss: () -> Unit,
) {
    val titulo = when (textoLegal) {
        TextoLegal.TERMINOS -> "Términos y Condiciones"
        TextoLegal.PRIVACIDAD -> "Política de Privacidad"
    }
    val texto = when (textoLegal) {
        TextoLegal.TERMINOS ->
            "Para usar PawPals debes ser mayor de 18 años y crear un perfil real para ti y tu perro. " +
                    "La app sirve para conocer otros dueños, hacer matches, chatear y organizar paseos de forma respetuosa. " +
                    "No está permitido usar perfiles falsos, molestar a otros usuarios ni compartir contenido ofensivo. " +
                    "Si alguien incumple estas normas, se puede reportar desde el chat para que el equipo lo revise."

        TextoLegal.PRIVACIDAD ->
            "PawPals guarda los datos necesarios para que funcione la cuenta: correo, nombre visible, datos del perro, " +
                    "matches, mensajes y ajustes de privacidad. La ubicación solo se solicita para el Modo Paseo y para " +
                    "mostrar perros o amigos cercanos cuando activas esa función. Si no das permiso de ubicación, puedes " +
                    "seguir usando la app, pero no se mostrarán paseos cercanos ni tu posición en el mapa."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", color = CoralPrimary) }
        },
    )
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

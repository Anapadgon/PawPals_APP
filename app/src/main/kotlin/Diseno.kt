package com.pawpals.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage

// --- Paleta ---
val CoralPrimary = Color(0xFFF4724A)
val CoralDark = Color(0xFFE05A33)
val PeachSecondary = Color(0xFFF89B6E)
val PeachLight = Color(0xFFFBBC96)
val PeachPale = Color(0xFFFFE6D3)
val SurfaceLight = Color(0xFFFFFDF8)
val SurfaceMuted = Color(0xFFFFF3EC)
val SurfaceAlt = Color(0xFFF6F6F8)
val OnCoral = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1F1A18)
val TextSecondary = Color(0xFF7A6F6A)
val OutlineSoft = Color(0xFFEADACF)
val SuccessGreen = Color(0xFF3DA26E)
val SuccessSoft = Color(0xFFD9F1E3)
val InfoBlue = Color(0xFF5AA2E5)
val InfoSoft = Color(0xFFDEEAF6)
val Error = Color(0xFFD94A4A)
val ErrorSoft = Color(0xFFFADCDC)

val CoralGradient = Brush.verticalGradient(
    colors = listOf(CoralPrimary, PeachSecondary, PeachLight),
)
val CoralGradientSoft = Brush.verticalGradient(
    colors = listOf(CoralPrimary, PeachSecondary),
)

// --- Tipografía ---
val PawpalsTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val LightColors = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = OnCoral,
    primaryContainer = PeachPale,
    onPrimaryContainer = TextPrimary,
    secondary = PeachSecondary,
    onSecondary = OnCoral,
    secondaryContainer = PeachLight,
    tertiary = SuccessGreen,
    onTertiary = OnCoral,
    tertiaryContainer = SuccessSoft,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceMuted,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    error = Error,
    errorContainer = ErrorSoft,
)

private val PawShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TemaPawpals(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = PawpalsTypography,
        shapes = PawShapes,
        content = contenido,
    )
}

@Composable
fun BotonPrimarioPaw(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CoralPrimary,
            contentColor = OnCoral,
            disabledContainerColor = PeachSecondary.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
        )
    }
}

@Composable
fun BotonContornoPaw(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    colorBorde: Color = OnCoral,
    colorContenido: Color = OnCoral,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, colorBorde),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorContenido),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
        )
    }
}

@Composable
fun CampoContornoPaw(
    valor: String,
    alCambiarValor: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    unaLinea: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    marcador: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val capitalization = when (keyboardType) {
        KeyboardType.Email, KeyboardType.Password, KeyboardType.Phone,
        KeyboardType.Number, KeyboardType.Uri -> KeyboardCapitalization.None
        else -> KeyboardCapitalization.Sentences
    }
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiarValor,
        label = { Text(etiqueta) },
        placeholder = marcador?.let { { Text(it, color = TextSecondary) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = unaLinea,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoralPrimary,
            unfocusedBorderColor = OutlineSoft,
            focusedLabelColor = CoralPrimary,
            cursorColor = CoralPrimary,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = SurfaceMuted,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiorPaw(
    titulo: String,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = { acciones?.invoke() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun ChipPaw(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconoInicio: (@Composable () -> Unit)? = null,
    fondoSeleccionado: Color = PeachPale,
    bordeSeleccionado: Color = CoralPrimary,
) {
    val bg = if (seleccionado) fondoSeleccionado else SurfaceAlt
    val border = if (seleccionado) bordeSeleccionado else OutlineSoft
    val textColor = if (seleccionado) CoralPrimary else TextPrimary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() },
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.5.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            iconoInicio?.invoke()
            Text(
                texto,
                color = textColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
fun InsigniaAtributo(
    texto: String,
    colorAcento: Color,
    fondoAcento: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = fondoAcento,
    ) {
        Text(
            text = texto,
            color = colorAcento,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun AvatarPaw(
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
    fondo: Color = PeachLight,
    primerPlano: Color = CoralPrimary,
) {
    val inicial = nombre.trim().firstOrNull()?.uppercase().orEmpty()
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        if (inicial.isNotEmpty()) {
            Text(
                text = inicial,
                color = primerPlano,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = primerPlano,
            )
        }
    }
}

@Composable
fun SelectorFotoPaw(
    etiqueta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    modeloImagen: Any? = null,
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(PeachPale)
            .border(
                width = 2.dp,
                color = CoralPrimary.copy(alpha = 0.5f),
                shape = CircleShape,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (modeloImagen != null) {
            AsyncImage(
                model = modeloImagen,
                contentDescription = etiqueta,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = etiqueta,
                color = CoralPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PuntosProgresoPaw(
    total: Int,
    actual: Int,
    modifier: Modifier = Modifier,
    colorActivo: Color = CoralPrimary,
    colorInactivo: Color = OutlineSoft,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0 until total) {
            val activo = i == actual
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .size(if (activo) 22.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (activo) colorActivo else colorInactivo),
            )
        }
    }
}

object AcentosPaw {
    val colorEnergia = CoralPrimary
    val fondoEnergia = PeachPale
    val colorSocial = SuccessGreen
    val fondoSocial = SuccessSoft
}

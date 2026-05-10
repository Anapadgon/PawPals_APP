package com.pawpals.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// colores base de pawpals usados por todas las pantallas
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

// tipografia general para mantener el mismo estilo en la app
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

// colores agrupados para etiquetas de energia y sociabilidad
object AcentosPaw {
    val colorEnergia = CoralPrimary
    val fondoEnergia = PeachPale
    val colorSocial = SuccessGreen
    val fondoSocial = SuccessSoft
}

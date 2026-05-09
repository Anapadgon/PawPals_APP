package com.pawpals.app

// datos minimos que llegan desde supabase auth
data class CuentaAuth(
    val uid: String,
    val correo: String?,
)

data class ResultadoRegistroCorreo(
    val correo: String,
    val sesionActiva: Boolean,
)

data class EstadisticasAdministracion(
    val numeroUsuarios: Int,
    val numeroPerros: Int,
    val numeroCoincidencias: Int,
    val reportesAbiertos: Int,
)

// rol usado para separar usuario normal y panel de administracion
enum class RolUsuario {
    USUARIO,
    ADMINISTRADOR,
}

data class PerfilUsuario(
    val uid: String,
    val correo: String,
    val nombreVisible: String,
    val zona: String,
    val sobreMi: String,
    val rol: RolUsuario,
    val bloqueado: Boolean,
    val latitud: Double?,
    val longitud: Double?,
    val tokenFcm: String?,
    val urlFoto: String?,
    val numeroAmigos: Int = 0,
    val numeroPaseos: Int = 0,
    val numeroCoincidencias: Int = 0,
    val paseando: Boolean = false,
) {
    // se mantiene este alias porque varias pantallas hablan de ciudad
    val ciudad: String get() = zona
}

// valores que se guardan en base de datos y tambien se muestran en la ui
enum class NivelEnergia(val label: String) {
    TRANQUILO("Tranquilo"),
    MODERADO("Moderado"),
    ALTA("Alta energía");

    companion object {
        fun fromRaw(raw: String?): NivelEnergia = when (raw) {
            "tranquilo" -> TRANQUILO
            "alta" -> ALTA
            else -> MODERADO
        }
    }
}

enum class Sociabilidad(val label: String) {
    MUY_SOCIABLE("Muy sociable"),
    SELECTIVO("Selectivo"),
    TIMIDO("Tímido");

    companion object {
        fun fromRaw(raw: String?): Sociabilidad = when (raw) {
            "selectivo" -> SELECTIVO
            "timido" -> TIMIDO
            else -> MUY_SOCIABLE
        }
    }
}

data class PerfilPerro(
    val id: String,
    val uidDueno: String,
    val nombre: String,
    val raza: String,
    val edadAnios: Int,
    val biografia: String,
    val urlFoto: String?,
    val energia: NivelEnergia = NivelEnergia.MODERADO,
    val sociabilidad: Sociabilidad = Sociabilidad.MUY_SOCIABLE,
)

// estado de una coincidencia entre dos usuarios
enum class EstadoCoincidencia {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
}

data class Coincidencia(
    val id: String,
    val usuarioA: String,
    val usuarioB: String,
    val uidIniciador: String,
    val estado: EstadoCoincidencia,
    val creadoEn: Long,
)

data class MensajeConversacion(
    val id: String,
    val uidRemitente: String,
    val texto: String,
    val marcaTemporal: Long,
)

enum class TipoObjetivoReporte {
    USUARIO,
    MENSAJE,
    PERRO,
}

enum class EstadoReporte {
    ABIERTO,
    REVISADO,
    ACCION_TOMADA,
}

data class ReporteContenido(
    val id: String,
    val tipoObjetivo: TipoObjetivoReporte,
    val idObjetivo: String,
    val uidReportante: String,
    val motivo: String,
    val estado: EstadoReporte,
    val creadoEn: Long,
)

enum class EstadoSolicitudAmistad {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
}

data class SolicitudAmistad(
    val id: String,
    val uidOrigen: String,
    val uidDestino: String,
    val estado: EstadoSolicitudAmistad,
    val creadoEn: Long,
)

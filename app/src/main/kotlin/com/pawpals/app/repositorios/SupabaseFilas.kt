package com.pawpals.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// compara ids aunque vengan con pequeñas diferencias de formato
internal fun idsAuthCoinciden(uidParametro: String, uidDeSesion: String): Boolean {
    val a = uidParametro.trim()
    val b = uidDeSesion.trim()
    if (a.equals(b, ignoreCase = true)) return true
    return a.replace("-", "").equals(b.replace("-", ""), ignoreCase = true)
}

// id estable a partir de dos uids; ordenado=true cuando el id no debe depender del emisor
internal fun idCompuesto(a: String, b: String, ordenado: Boolean = true): String =
    (if (ordenado) listOf(a, b).sorted() else listOf(a, b)).joinToString("__")

// supabase no notifica cambios en tiempo real, asi que se relee cada cierto tiempo
internal fun <T> observarConPolling(
    periodoMs: Long = 2000L,
    valorEnError: T,
    cargar: suspend () -> T,
): Flow<T> = flow {
    while (true) {
        emit(runCatching { cargar() }.getOrElse { valorEnError })
        delay(periodoMs)
    }
}
    .flowOn(Dispatchers.IO)
    .distinctUntilChanged()

// suma o resta un contador de la fila del usuario; -1 con coerceAtLeast(0) evita negativos
internal suspend fun SupabaseClient.cambiarContadorUsuario(uid: String, campo: String, delta: Int) {
    val u = postgrest.from(Tablas.USUARIOS).select {
        filter { eq("id", uid) }
        limit(1)
    }.decodeList<FilaUsuario>().firstOrNull() ?: return
    val actual = when (campo) {
        "numero_amigos" -> u.numeroAmigos
        "numero_coincidencias" -> u.numeroCoincidencias
        "numero_paseos" -> u.numeroPaseos
        else -> return
    }
    val nuevo = (actual + delta).coerceAtLeast(0)
    postgrest.from(Tablas.USUARIOS).update({
        set(campo, nuevo)
    }) {
        filter { eq("id", uid) }
    }
}

internal object Tablas {
    const val USUARIOS = "usuarios"
    const val PERROS = "perros"
    const val COINCIDENCIAS = "coincidencias"
    const val DESLIZAMIENTOS = "deslizamientos"
    const val SOLICITUDES_AMISTAD = "solicitudes_amistad"
    const val AMIGOS = "amigos"
    const val TICKETS_SOPORTE = "tickets_soporte"
    const val CONVERSACIONES = "conversaciones"
    const val MENSAJES = "mensajes"
    const val REPORTES = "reportes"
    const val BUCKET_MEDIOS = "medios"
}

@Serializable
internal data class FilaUsuario(
    val id: String,
    val correo: String = "",
    @SerialName("nombre_visible") val nombreVisible: String = "",
    val zona: String = "",
    @SerialName("sobre_mi") val sobreMi: String = "",
    val rol: String = "usuario",
    val bloqueado: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null,
    @SerialName("ubicacion_actualizada_en") val ubicacionActualizadaEn: Long? = null,
    @SerialName("token_fcm") val tokenFcm: String? = null,
    @SerialName("url_foto") val urlFoto: String? = null,
    @SerialName("numero_amigos") val numeroAmigos: Int = 0,
    @SerialName("numero_paseos") val numeroPaseos: Int = 0,
    @SerialName("numero_coincidencias") val numeroCoincidencias: Int = 0,
    val paseando: Boolean = false,
    @SerialName("creado_en") val creadoEn: Long? = null,
    @SerialName("es_demo") val esDemo: Boolean = false,
)

internal fun FilaUsuario.aPerfilUsuario(): PerfilUsuario = PerfilUsuario(
    uid = id,
    correo = correo,
    nombreVisible = nombreVisible,
    zona = zona,
    sobreMi = sobreMi,
    rol = when (rol) {
        "administrador" -> RolUsuario.ADMINISTRADOR
        else -> RolUsuario.USUARIO
    },
    bloqueado = bloqueado,
    latitud = latitud,
    longitud = longitud,
    tokenFcm = tokenFcm,
    urlFoto = urlFoto,
    numeroAmigos = numeroAmigos,
    numeroPaseos = numeroPaseos,
    numeroCoincidencias = numeroCoincidencias,
    paseando = paseando,
)

@Serializable
internal data class FilaPerro(
    val id: String,
    @SerialName("uid_dueno") val uidDueno: String,
    val nombre: String = "",
    val raza: String = "",
    @SerialName("edad_anios") val edadAnios: Int = 1,
    val biografia: String = "",
    @SerialName("url_foto") val urlFoto: String? = null,
    val energia: String = "moderado",
    val sociabilidad: String = "muy_sociable",
    @SerialName("actualizado_en") val actualizadoEn: Long? = null,
    @SerialName("es_demo") val esDemo: Boolean = false,
)

internal fun FilaPerro.aPerfilPerro(): PerfilPerro = PerfilPerro(
    id = id,
    uidDueno = uidDueno,
    nombre = nombre,
    raza = raza,
    edadAnios = edadAnios.coerceAtLeast(1),
    biografia = biografia,
    urlFoto = urlFoto,
    energia = NivelEnergia.fromRaw(energia),
    sociabilidad = Sociabilidad.fromRaw(sociabilidad),
)

@Serializable
internal data class FilaCoincidencia(
    val id: String,
    @SerialName("usuario_a") val usuarioA: String,
    @SerialName("usuario_b") val usuarioB: String,
    @SerialName("usuario_menor") val usuarioMenor: String,
    @SerialName("usuario_mayor") val usuarioMayor: String,
    val participantes: List<String>,
    val iniciador: String,
    val estado: String,
    @SerialName("creado_en") val creadoEn: Long,
)

internal fun FilaCoincidencia.aCoincidencia(): Coincidencia = Coincidencia(
    id = id,
    usuarioA = usuarioA,
    usuarioB = usuarioB,
    uidIniciador = iniciador,
    estado = when (estado) {
        "aceptada" -> EstadoCoincidencia.ACEPTADA
        "rechazada" -> EstadoCoincidencia.RECHAZADA
        else -> EstadoCoincidencia.PENDIENTE
    },
    creadoEn = creadoEn,
)

@Serializable
internal data class FilaDeslizamiento(
    val id: String,
    @SerialName("uid_origen") val uidOrigen: String,
    @SerialName("uid_destino") val uidDestino: String,
    val accion: String,
    @SerialName("creado_en") val creadoEn: Long,
)

@Serializable
internal data class FilaConversacion(
    val id: String,
    val participantes: List<String> = emptyList(),
    @SerialName("creado_en") val creadoEn: Long? = null,
    @SerialName("ultimo_mensaje") val ultimoMensaje: String? = null,
    @SerialName("ultimo_mensaje_en") val ultimoMensajeEn: Long? = null,
    @SerialName("ultimo_remitente") val ultimoRemitente: String? = null,
)

@Serializable
internal data class FilaMensaje(
    val id: String,
    @SerialName("conversacion_id") val conversacionId: String,
    @SerialName("uid_remitente") val uidRemitente: String,
    val texto: String,
    @SerialName("marca_temporal") val marcaTemporal: Long,
)

internal fun FilaMensaje.aMensaje(): MensajeConversacion = MensajeConversacion(
    id = id,
    uidRemitente = uidRemitente,
    texto = texto,
    marcaTemporal = marcaTemporal,
)

@Serializable
internal data class FilaMensajeInsert(
    val id: String,
    @SerialName("conversacion_id") val conversacionId: String,
    @SerialName("uid_remitente") val uidRemitente: String,
    val texto: String,
    @SerialName("marca_temporal") val marcaTemporal: Long,
)

@Serializable
internal data class FilaTicketInsert(
    val uid: String,
    val correo: String,
    val mensaje: String,
    val estado: String = "open",
    @SerialName("creado_en") val creadoEn: Long,
)

@Serializable
internal data class FilaReporte(
    val id: String,
    @SerialName("tipo_objetivo") val tipoObjetivo: String,
    @SerialName("id_objetivo") val idObjetivo: String,
    @SerialName("uid_reportante") val uidReportante: String,
    val motivo: String,
    val estado: String,
    @SerialName("creado_en") val creadoEn: Long,
)

internal fun FilaReporte.aReporte(): ReporteContenido = ReporteContenido(
    id = id,
    tipoObjetivo = when (tipoObjetivo) {
        "mensaje" -> TipoObjetivoReporte.MENSAJE
        "perro" -> TipoObjetivoReporte.PERRO
        else -> TipoObjetivoReporte.USUARIO
    },
    idObjetivo = idObjetivo,
    uidReportante = uidReportante,
    motivo = motivo,
    estado = when (estado) {
        "revisado" -> EstadoReporte.REVISADO
        "accion_tomada" -> EstadoReporte.ACCION_TOMADA
        else -> EstadoReporte.ABIERTO
    },
    creadoEn = creadoEn,
)

@Serializable
internal data class FilaSolicitud(
    val id: String,
    @SerialName("uid_origen") val uidOrigen: String,
    @SerialName("uid_destino") val uidDestino: String,
    val estado: String,
    @SerialName("creado_en") val creadoEn: Long,
)

internal fun FilaSolicitud.aSolicitud(): SolicitudAmistad = SolicitudAmistad(
    id = id,
    uidOrigen = uidOrigen,
    uidDestino = uidDestino,
    estado = when (estado) {
        "aceptada" -> EstadoSolicitudAmistad.ACEPTADA
        "rechazada" -> EstadoSolicitudAmistad.RECHAZADA
        else -> EstadoSolicitudAmistad.PENDIENTE
    },
    creadoEn = creadoEn,
)

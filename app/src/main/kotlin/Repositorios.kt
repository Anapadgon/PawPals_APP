package com.pawpals.app

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Compara uids de Supabase Auth (p. ej. con o sin normalizar guiones). */
private fun idsAuthCoinciden(uidParametro: String, uidDeSesion: String): Boolean {
    val a = uidParametro.trim()
    val b = uidDeSesion.trim()
    if (a.equals(b, ignoreCase = true)) return true
    return a.replace("-", "").equals(b.replace("-", ""), ignoreCase = true)
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
        "administrador", "administradoristrador" -> RolUsuario.ADMINISTRADOR
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

// --- Contratos ---

interface RepositorioAutenticacion {
    val estadoAutenticacion: Flow<CuentaAuth?>
    fun cuentaActual(): CuentaAuth?
    suspend fun iniciarSesionCorreo(correo: String, contrasena: String): Result<Unit>
    suspend fun registrarCorreo(correo: String, contrasena: String): Result<ResultadoRegistroCorreo>
    suspend fun actualizarCorreo(nuevoCorreo: String): Result<Unit>
    suspend fun enviarCorreoRestablecerContrasena(): Result<Unit>
    suspend fun enviarCorreoRestablecerContrasena(correo: String): Result<Unit>
    suspend fun cambiarContrasena(contrasenaActual: String, nuevaContrasena: String): Result<Unit>
    suspend fun eliminarCuenta(contrasenaActual: String): Result<Unit>
    suspend fun cerrarSesion()
}

interface RepositorioUsuario {
    fun observarUsuario(uid: String): Flow<PerfilUsuario?>
    suspend fun asegurarDocumentoUsuario(uid: String, correo: String): Result<Unit>
    suspend fun actualizarPerfil(
        uid: String,
        nombreVisible: String,
        zona: String,
        sobreMi: String,
        urlFoto: String?,
    ): Result<Unit>
    suspend fun actualizarUbicacion(uid: String, lat: Double, lng: Double): Result<Unit>
    suspend fun establecerPaseando(uid: String, paseando: Boolean): Result<Unit>
    suspend fun actualizarTokenFcm(uid: String, token: String): Result<Unit>
    suspend fun obtenerTodosUsuarios(): Result<List<PerfilUsuario>>
    suspend fun establecerBloqueado(uid: String, bloqueado: Boolean): Result<Unit>
    suspend fun eliminarPerfilUsuario(uid: String): Result<Unit>
    suspend fun incrementarNumeroPaseos(uid: String): Result<Unit>
}

interface RepositorioPerro {
    fun observarPerroDeDueno(uidDueno: String): Flow<PerfilPerro?>
    suspend fun guardarPerro(
        uidDueno: String,
        nombre: String,
        raza: String,
        edadAnios: Int,
        biografia: String,
        urlFoto: String?,
        energia: NivelEnergia,
        sociabilidad: Sociabilidad,
    ): Result<String>
    suspend fun obtenerTodosPerros(): Result<List<PerfilPerro>>
    suspend fun eliminarPerro(idPerro: String): Result<Unit>
    suspend fun eliminarPerroDeDueno(uidDueno: String): Result<Unit>
}

interface RepositorioCoincidencia {
    fun observarCoincidenciasDeUsuario(uid: String): Flow<List<Coincidencia>>
    suspend fun enviarSolicitudCoincidencia(uidOrigen: String, uidDestino: String): Result<Unit>
    suspend fun responderCoincidencia(idCoincidencia: String, aceptar: Boolean): Result<Unit>
    suspend fun eliminarCoincidencia(idCoincidencia: String): Result<Unit>
    suspend fun buscarCoincidencia(miUid: String, otroUid: String): Result<Coincidencia?>
}

interface RepositorioConversacion {
    fun observarMensajes(idConversacion: String): Flow<List<MensajeConversacion>>
    suspend fun enviarMensaje(idConversacion: String, uidRemitente: String, texto: String): Result<Unit>
    companion object {
        fun idConversacionFor(usuarioA: String, usuarioB: String): String =
            listOf(usuarioA, usuarioB).sorted().joinToString("__")
    }
}

enum class AccionDeslizamiento { ME_GUSTA, SUPER_ME_GUSTA, DESCARTAR }

sealed interface ResultadoDeslizamiento {
    data object Guardado : ResultadoDeslizamiento
    data class CoincidenciaCreada(val idCoincidencia: String) : ResultadoDeslizamiento
}

interface RepositorioDeslizamiento {
    suspend fun registrarDeslizamiento(
        uidOrigen: String,
        uidDestino: String,
        accion: AccionDeslizamiento,
    ): Result<ResultadoDeslizamiento>
    suspend fun cargarUidsDeslizados(miUid: String): Result<Set<String>>
    suspend fun limpiarEntre(aUid: String, bUid: String): Result<Unit>
}

interface RepositorioAmistad {
    fun observarSolicitudesEntrantes(miUid: String): Flow<List<SolicitudAmistad>>
    fun observarSolicitudEntre(miUid: String, otroUid: String): Flow<SolicitudAmistad?>
    fun observarUidsAmigos(miUid: String): Flow<Set<String>>
    suspend fun enviarSolicitudAmistad(uidOrigen: String, uidDestino: String): Result<Unit>
    suspend fun aceptarSolicitudAmistad(idSolicitud: String): Result<Unit>
    suspend fun rechazarSolicitudAmistad(idSolicitud: String): Result<Unit>
    suspend fun eliminarAmigo(miUid: String, otroUid: String): Result<Unit>
    suspend fun sonAmigos(miUid: String, otroUid: String): Result<Boolean>
}

interface RepositorioModeracion {
    suspend fun enviarReporte(
        uidReportante: String,
        tipoObjetivo: TipoObjetivoReporte,
        idObjetivo: String,
        motivo: String,
    ): Result<Unit>
    fun observarReportesAbiertos(): Flow<List<ReporteContenido>>
    suspend fun actualizarEstadoReporte(idReporte: String, estado: EstadoReporte): Result<Unit>
}

interface RepositorioAlmacenamiento {
    suspend fun subirFotoPerfil(uid: String, origen: Uri): Result<String>
    suspend fun subirFotoPerro(uidDueno: String, origen: Uri): Result<String>
}

interface RepositorioSoporte {
    suspend fun enviarTicket(uid: String, correo: String, mensaje: String): Result<Unit>
}

interface RepositorioDatosDemo {
    suspend fun sembrarPerfilesDemo(centerLat: Double?, centerLng: Double?): Result<Int>
    suspend fun limpiarPerfilesDemo(): Result<Int>
    companion object {
        const val DEMO_PREFIX = "demo_"
    }
}

interface RepositorioEstadisticasAdministracion {
    suspend fun cargarEstadisticas(): Result<EstadisticasAdministracion>
}

// --- Implementaciones Supabase ---

@Singleton
class RepositorioAutenticacionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioAutenticacion {

    private val auth: Auth get() = cliente.auth

    override val estadoAutenticacion: Flow<CuentaAuth?> =
        auth.sessionStatus.map { st ->
            when (st) {
                is SessionStatus.Authenticated ->
                    st.session.user?.let { CuentaAuth(uid = it.id, correo = it.email) }
                is SessionStatus.NotAuthenticated -> null
                is SessionStatus.RefreshFailure ->
                    auth.currentUserOrNull()?.let { CuentaAuth(uid = it.id, correo = it.email) }
                SessionStatus.Initializing -> null
            }
        }.distinctUntilChanged()

    override fun cuentaActual(): CuentaAuth? =
        auth.currentUserOrNull()?.let { CuentaAuth(uid = it.id, correo = it.email) }

    override suspend fun iniciarSesionCorreo(correo: String, contrasena: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            email = correo
            password = contrasena
        }
    }

    override suspend fun registrarCorreo(correo: String, contrasena: String): Result<ResultadoRegistroCorreo> =
        runCatching {
            val correoTrim = correo.trim()
            auth.signUpWith(Email) {
                email = correoTrim
                password = contrasena
            }
            // Tras signUp, si «Confirm email» está desactivado en Supabase hay sesión al instante.
            val sesionActiva = auth.currentSessionOrNull() != null
            ResultadoRegistroCorreo(correo = correoTrim, sesionActiva = sesionActiva)
        }

    override suspend fun actualizarCorreo(nuevoCorreo: String): Result<Unit> = runCatching {
        auth.updateUser { email = nuevoCorreo.trim() }
        Unit
    }

    override suspend fun enviarCorreoRestablecerContrasena(): Result<Unit> = runCatching {
        val correo = auth.currentUserOrNull()?.email ?: error("No hay un correo asociado")
        auth.resetPasswordForEmail(correo)
        Unit
    }

    override suspend fun enviarCorreoRestablecerContrasena(correo: String): Result<Unit> = runCatching {
        require(correo.isNotBlank()) { "Introduce un correo válido" }
        auth.resetPasswordForEmail(correo.trim())
        Unit
    }

    override suspend fun cambiarContrasena(
        contrasenaActual: String,
        nuevaContrasena: String,
    ): Result<Unit> = runCatching {
        val email = auth.currentUserOrNull()?.email ?: error("No hay sesión activa")
        auth.signInWith(Email) {
            this.email = email
            password = contrasenaActual
        }
        auth.updateUser { password = nuevaContrasena }
        Unit
    }

    override suspend fun eliminarCuenta(contrasenaActual: String): Result<Unit> = runCatching {
        val email = auth.currentUserOrNull()?.email ?: error("No hay sesión activa")
        auth.signInWith(Email) {
            this.email = email
            password = contrasenaActual
        }
        cliente.postgrest.rpc("eliminar_cuenta_auth") {}
        runCatching { auth.signOut(SignOutScope.GLOBAL) }
        Unit
    }

    override suspend fun cerrarSesion() {
        auth.signOut(SignOutScope.GLOBAL)
    }
}

@Singleton
class RepositorioUsuarioSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioUsuario {

    override fun observarUsuario(uid: String): Flow<PerfilUsuario?> = flow {
        while (true) {
            emit(cargarUsuario(uid))
            delay(2000)
        }
    }.distinctUntilChanged()

    private suspend fun cargarUsuario(uid: String): PerfilUsuario? {
        val rows = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>()
        return rows.firstOrNull()?.aPerfilUsuario()
    }

    override suspend fun asegurarDocumentoUsuario(uid: String, correo: String): Result<Unit> = runCatching {
        runCatching { cliente.auth.refreshCurrentSession() }
        val authUid = cliente.auth.currentUserOrNull()?.id
            ?: error(
                "No hay sesión activa con el servidor. Cierra sesión y vuelve a entrar.",
            )
        require(idsAuthCoinciden(uid, authUid)) {
            "La cuenta activa no coincide con el perfil que estás configurando."
        }
        val rol = if (correo.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)) {
            RolUsuario.ADMINISTRADOR.name.lowercase()
        } else {
            RolUsuario.USUARIO.name.lowercase()
        }
        val existe = cargarUsuario(uid) != null
        val ahora = System.currentTimeMillis()
        if (!existe) {
            val nombreProvisional = correo.substringBefore("@").trim().ifBlank { "Usuario" }
            val fila = FilaUsuario(
                id = uid.trim(),
                correo = correo,
                nombreVisible = nombreProvisional,
                zona = "",
                sobreMi = "",
                rol = rol,
                bloqueado = false,
                creadoEn = ahora,
            )
            runCatching {
                cliente.postgrest.rpc(
                    "pawpals_asegurar_mi_usuario",
                    buildJsonObject {
                        put("p_correo", correo)
                        put("p_nombre_visible", nombreProvisional)
                    },
                )
            }
            if (cargarUsuario(uid) == null) {
                runCatching {
                    cliente.postgrest.from(Tablas.USUARIOS).insert(fila)
                }
            }
            if (cargarUsuario(uid) == null) {
                error(
                    "Falta crear tu ficha en la base de datos. En Supabase → SQL ejecuta el archivo " +
                        "supabase/pawpals_rpc_asegurar_usuario.sql del proyecto y vuelve a intentarlo.",
                )
            }
            // RPC inserta con rol «usuario»; si es admin de demo, se ajusta aquí.
            if (correo.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)) {
                cliente.postgrest.from(Tablas.USUARIOS).update({
                    set("rol", rol)
                }) {
                    filter { eq("id", uid) }
                }
            }
        } else if (correo.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)) {
            cliente.postgrest.from(Tablas.USUARIOS).update({
                set("rol", rol)
            }) {
                filter { eq("id", uid) }
            }
        }
        Unit
    }

    override suspend fun actualizarPerfil(
        uid: String,
        nombreVisible: String,
        zona: String,
        sobreMi: String,
        urlFoto: String?,
    ): Result<Unit> = runCatching {
        val id = uid.trim()
        val rpcOk = runCatching {
            cliente.postgrest.rpc(
                "pawpals_actualizar_mi_perfil",
                buildJsonObject {
                    put("p_nombre_visible", nombreVisible)
                    put("p_zona", zona)
                    put("p_sobre_mi", sobreMi)
                    if (urlFoto != null) {
                        put("p_url_foto", urlFoto)
                    }
                },
            )
        }.isSuccess
        if (!rpcOk) {
            cliente.postgrest.from(Tablas.USUARIOS).update({
                set("nombre_visible", nombreVisible)
                set("zona", zona)
                set("sobre_mi", sobreMi)
                if (urlFoto != null) {
                    set("url_foto", urlFoto)
                }
            }) {
                filter { eq("id", id) }
            }
        }
        Unit
    }

    override suspend fun actualizarUbicacion(uid: String, lat: Double, lng: Double): Result<Unit> = runCatching {
        val ahora = System.currentTimeMillis()
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("latitud", lat)
            set("longitud", lng)
            set("ubicacion_actualizada_en", ahora)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun establecerPaseando(uid: String, paseando: Boolean): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("paseando", paseando)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun actualizarTokenFcm(uid: String, token: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("token_fcm", token)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun obtenerTodosUsuarios(): Result<List<PerfilUsuario>> = runCatching {
        cliente.postgrest.from(Tablas.USUARIOS).select {
            limit(200)
        }.decodeList<FilaUsuario>().map { it.aPerfilUsuario() }
    }

    override suspend fun establecerBloqueado(uid: String, bloqueado: Boolean): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("bloqueado", bloqueado)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun eliminarPerfilUsuario(uid: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.USUARIOS).delete {
            filter { eq("id", uid) }
        }
        Unit
    }

    override suspend fun incrementarNumeroPaseos(uid: String): Result<Unit> = runCatching {
        val u = cargarUsuario(uid) ?: return@runCatching
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("numero_paseos", u.numeroPaseos + 1)
        }) {
            filter { eq("id", uid) }
        }
        Unit
    }
}

@Singleton
class RepositorioPerroSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioPerro {

    override fun observarPerroDeDueno(uidDueno: String): Flow<PerfilPerro?> = callbackFlow {
        val job = launch {
            while (isActive) {
                trySend(cargarPerroDueno(uidDueno))
                delay(2000)
            }
        }
        awaitClose { job.cancel() }
    }

    private suspend fun cargarPerroDueno(uidDueno: String): PerfilPerro? {
        val rows = cliente.postgrest.from(Tablas.PERROS).select {
            filter { eq("uid_dueno", uidDueno) }
            limit(1)
        }.decodeList<FilaPerro>()
        return rows.firstOrNull()?.aPerfilPerro()
    }

    override suspend fun guardarPerro(
        uidDueno: String,
        nombre: String,
        raza: String,
        edadAnios: Int,
        biografia: String,
        urlFoto: String?,
        energia: NivelEnergia,
        sociabilidad: Sociabilidad,
    ): Result<String> = runCatching {
        val uid = uidDueno.trim()
        runCatching {
            cliente.postgrest.rpc(
                "pawpals_guardar_mi_perro",
                buildJsonObject {
                    put("p_nombre", nombre)
                    put("p_raza", raza)
                    put("p_edad_anios", edadAnios)
                    put("p_biografia", biografia)
                    put("p_energia", energia.name.lowercase())
                    put("p_sociabilidad", sociabilidad.name.lowercase())
                    if (urlFoto != null) {
                        put("p_url_foto", urlFoto)
                    }
                },
            )
        }
        cargarPerroDueno(uid)?.id?.let { return@runCatching it }

        val existentes = cliente.postgrest.from(Tablas.PERROS).select {
            filter { eq("uid_dueno", uid) }
            limit(1)
        }.decodeList<FilaPerro>()
        val id = existentes.firstOrNull()?.id ?: UUID.randomUUID().toString()
        val ahora = System.currentTimeMillis()
        val fila = FilaPerro(
            id = id,
            uidDueno = uid,
            nombre = nombre,
            raza = raza,
            edadAnios = edadAnios.coerceAtLeast(1),
            biografia = biografia,
            urlFoto = urlFoto,
            energia = energia.name.lowercase(),
            sociabilidad = sociabilidad.name.lowercase(),
            actualizadoEn = ahora,
        )
        if (existentes.isEmpty()) {
            cliente.postgrest.from(Tablas.PERROS).insert(fila)
        } else {
            cliente.postgrest.from(Tablas.PERROS).update({
                set("nombre", nombre)
                set("raza", raza)
                set("edad_anios", edadAnios.coerceAtLeast(1))
                set("biografia", biografia)
                if (urlFoto != null) {
                    set("url_foto", urlFoto)
                }
                set("energia", energia.name.lowercase())
                set("sociabilidad", sociabilidad.name.lowercase())
                set("actualizado_en", ahora)
            }) {
                filter { eq("id", id) }
            }
        }
        cargarPerroDueno(uid)?.id ?: id
    }

    override suspend fun obtenerTodosPerros(): Result<List<PerfilPerro>> = runCatching {
        cliente.postgrest.from(Tablas.PERROS).select {
            limit(200)
        }.decodeList<FilaPerro>().map { it.aPerfilPerro() }
    }

    override suspend fun eliminarPerro(idPerro: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.PERROS).delete {
            filter { eq("id", idPerro) }
        }
        Unit
    }

    override suspend fun eliminarPerroDeDueno(uidDueno: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.PERROS).delete {
            filter { eq("uid_dueno", uidDueno) }
        }
        Unit
    }
}

@Singleton
class RepositorioCoincidenciaSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioCoincidencia {

    override fun observarCoincidenciasDeUsuario(uid: String): Flow<List<Coincidencia>> = callbackFlow {
        val job = launch {
            while (isActive) {
                val list = cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
                    filter { cs("participantes", listOf(uid)) }
                }.decodeList<FilaCoincidencia>()
                    .map { it.aCoincidencia() }
                    .sortedByDescending { it.creadoEn }
                trySend(list)
                delay(2000)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun enviarSolicitudCoincidencia(uidOrigen: String, uidDestino: String): Result<Unit> = runCatching {
        if (uidOrigen == uidDestino) error("No puedes crear una coincidencia contigo mismo")
        val participantes = listOf(uidOrigen, uidDestino).sorted()
        val existentes = cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
            filter {
                eq("usuario_menor", participantes[0])
                eq("usuario_mayor", participantes[1])
            }
            limit(1)
        }.decodeList<FilaCoincidencia>()
        if (existentes.isNotEmpty()) return@runCatching
        val id = UUID.randomUUID().toString()
        val fila = FilaCoincidencia(
            id = id,
            usuarioA = participantes[0],
            usuarioB = participantes[1],
            usuarioMenor = participantes[0],
            usuarioMayor = participantes[1],
            participantes = participantes,
            iniciador = uidOrigen,
            estado = EstadoCoincidencia.PENDIENTE.name.lowercase(),
            creadoEn = System.currentTimeMillis(),
        )
        cliente.postgrest.from(Tablas.COINCIDENCIAS).insert(fila)
        Unit
    }

    override suspend fun responderCoincidencia(idCoincidencia: String, aceptar: Boolean): Result<Unit> = runCatching {
        val estado = if (aceptar) EstadoCoincidencia.ACEPTADA else EstadoCoincidencia.RECHAZADA
        cliente.postgrest.from(Tablas.COINCIDENCIAS).update({
            set("estado", estado.name.lowercase())
        }) {
            filter { eq("id", idCoincidencia) }
        }
        Unit
    }

    override suspend fun eliminarCoincidencia(idCoincidencia: String): Result<Unit> = runCatching {
        val row = cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
            filter { eq("id", idCoincidencia) }
            limit(1)
        }.decodeList<FilaCoincidencia>().firstOrNull()
        cliente.postgrest.from(Tablas.COINCIDENCIAS).delete {
            filter { eq("id", idCoincidencia) }
        }
        if (row?.estado == EstadoCoincidencia.ACEPTADA.name.lowercase()) {
            row.participantes.forEach { uid ->
                runCatching { decrementarCoincidencias(uid) }
            }
        }
        Unit
    }

    private suspend fun decrementarCoincidencias(uid: String) {
        val u = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>().firstOrNull() ?: return
        val nuevo = (u.numeroCoincidencias - 1).coerceAtLeast(0)
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("numero_coincidencias", nuevo)
        }) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun buscarCoincidencia(miUid: String, otroUid: String): Result<Coincidencia?> =
        runCatching {
            val p = listOf(miUid, otroUid).sorted()
            cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
                filter {
                    eq("usuario_menor", p[0])
                    eq("usuario_mayor", p[1])
                }
                limit(1)
            }.decodeList<FilaCoincidencia>().firstOrNull()?.aCoincidencia()
        }
}

@Singleton
class RepositorioConversacionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioConversacion {

    override fun observarMensajes(idConversacion: String): Flow<List<MensajeConversacion>> = callbackFlow {
        val job = launch {
            while (isActive) {
                val list = cliente.postgrest.from(Tablas.MENSAJES).select {
                    filter { eq("conversacion_id", idConversacion) }
                    order("marca_temporal", Order.ASCENDING)
                    limit(200)
                }.decodeList<FilaMensaje>().map { it.aMensaje() }
                trySend(list)
                delay(1200)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun enviarMensaje(idConversacion: String, uidRemitente: String, texto: String): Result<Unit> =
        runCatching {
            val participantes = idConversacion.split("__").filter { it.isNotBlank() }
            require(participantes.size == 2) { "idConversacion inválido: $idConversacion" }
            require(uidRemitente in participantes) { "El emisor no pertenece a la conversación" }

            val existe = cliente.postgrest.from(Tablas.CONVERSACIONES).select {
                filter { eq("id", idConversacion) }
                limit(1)
            }.decodeList<FilaConversacion>().isNotEmpty()

            val ahora = System.currentTimeMillis()
            if (!existe) {
                val conv = FilaConversacion(
                    id = idConversacion,
                    participantes = participantes,
                    creadoEn = ahora,
                    ultimoMensaje = texto,
                    ultimoMensajeEn = ahora,
                    ultimoRemitente = uidRemitente,
                )
                cliente.postgrest.from(Tablas.CONVERSACIONES).insert(conv)
            }

            val msg = FilaMensajeInsert(
                id = UUID.randomUUID().toString(),
                conversacionId = idConversacion,
                uidRemitente = uidRemitente,
                texto = texto,
                marcaTemporal = ahora,
            )
            cliente.postgrest.from(Tablas.MENSAJES).insert(msg)

            cliente.postgrest.from(Tablas.CONVERSACIONES).update({
                set("ultimo_mensaje", texto)
                set("ultimo_mensaje_en", ahora)
                set("ultimo_remitente", uidRemitente)
            }) {
                filter { eq("id", idConversacion) }
            }
            Unit
        }
}

@Singleton
class RepositorioDeslizamientoSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioDeslizamiento {

    override suspend fun registrarDeslizamiento(
        uidOrigen: String,
        uidDestino: String,
        accion: AccionDeslizamiento,
    ): Result<ResultadoDeslizamiento> = runCatching {
        require(uidOrigen != uidDestino) { "No puedes deslizar sobre tu propio perfil" }

        val myDocId = idDeslizamiento(uidOrigen, uidDestino)
        val fila = FilaDeslizamiento(
            id = myDocId,
            uidOrigen = uidOrigen,
            uidDestino = uidDestino,
            accion = accion.name.lowercase(),
            creadoEn = System.currentTimeMillis(),
        )
        cliente.postgrest.from(Tablas.DESLIZAMIENTOS).upsert(fila) {
            onConflict = "id"
        }

        if (accion == AccionDeslizamiento.DESCARTAR) return@runCatching ResultadoDeslizamiento.Guardado

        val reverseId = idDeslizamiento(uidDestino, uidOrigen)
        val reverseDoc = cliente.postgrest.from(Tablas.DESLIZAMIENTOS).select {
            filter { eq("id", reverseId) }
            limit(1)
        }.decodeList<FilaDeslizamiento>().firstOrNull()
        val reverseAction = reverseDoc?.accion
        val reverseIsLike =
            reverseAction == AccionDeslizamiento.ME_GUSTA.name.lowercase() ||
                reverseAction == AccionDeslizamiento.SUPER_ME_GUSTA.name.lowercase()
        if (!reverseIsLike) return@runCatching ResultadoDeslizamiento.Guardado

        val idCoincidencia = idCoincidencia(uidOrigen, uidDestino)
        val participantes = listOf(uidOrigen, uidDestino).sorted()
        val coincidencia = FilaCoincidencia(
            id = idCoincidencia,
            usuarioA = participantes[0],
            usuarioB = participantes[1],
            usuarioMenor = participantes[0],
            usuarioMayor = participantes[1],
            participantes = participantes,
            iniciador = uidOrigen,
            estado = EstadoCoincidencia.ACEPTADA.name.lowercase(),
            creadoEn = System.currentTimeMillis(),
        )
        cliente.postgrest.from(Tablas.COINCIDENCIAS).upsert(coincidencia) {
            onConflict = "id"
        }

        runCatching {
            incrementarCoincidencias(uidOrigen)
            incrementarCoincidencias(uidDestino)
        }

        ResultadoDeslizamiento.CoincidenciaCreada(idCoincidencia)
    }

    private suspend fun incrementarCoincidencias(uid: String) {
        val u = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>().firstOrNull() ?: return
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("numero_coincidencias", u.numeroCoincidencias + 1)
        }) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun cargarUidsDeslizados(miUid: String): Result<Set<String>> = runCatching {
        cliente.postgrest.from(Tablas.DESLIZAMIENTOS).select {
            filter { eq("uid_origen", miUid) }
            limit(500)
        }.decodeList<FilaDeslizamiento>().map { it.uidDestino }.toSet()
    }

    override suspend fun limpiarEntre(aUid: String, bUid: String): Result<Unit> = runCatching {
        val id1 = idDeslizamiento(aUid, bUid)
        val id2 = idDeslizamiento(bUid, aUid)
        cliente.postgrest.from(Tablas.DESLIZAMIENTOS).delete {
            filter { eq("id", id1) }
        }
        cliente.postgrest.from(Tablas.DESLIZAMIENTOS).delete {
            filter { eq("id", id2) }
        }
        Unit
    }

    companion object {
        internal fun idDeslizamiento(from: String, to: String): String = "${from}__$to"
        internal fun idCoincidencia(a: String, b: String): String {
            val sorted = listOf(a, b).sorted()
            return "${sorted[0]}__${sorted[1]}"
        }
    }
}

@Singleton
class RepositorioAmistadSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioAmistad {

    override fun observarSolicitudesEntrantes(miUid: String): Flow<List<SolicitudAmistad>> = callbackFlow {
        val job = launch {
            while (isActive) {
                val list = cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).select {
                    filter {
                        eq("uid_destino", miUid)
                        eq("estado", EstadoSolicitudAmistad.PENDIENTE.name.lowercase())
                    }
                }.decodeList<FilaSolicitud>().map { it.aSolicitud() }
                trySend(list)
                delay(2500)
            }
        }
        awaitClose { job.cancel() }
    }

    override fun observarSolicitudEntre(miUid: String, otroUid: String): Flow<SolicitudAmistad?> = callbackFlow {
        val idAB = idSolicitud(miUid, otroUid)
        val idBA = idSolicitud(otroUid, miUid)
        val job = launch {
            while (isActive) {
                val list = cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).select {
                    filter {
                        or {
                            eq("id", idAB)
                            eq("id", idBA)
                        }
                    }
                }.decodeList<FilaSolicitud>().map { it.aSolicitud() }
                trySend(list.maxByOrNull { it.creadoEn })
                delay(2500)
            }
        }
        awaitClose { job.cancel() }
    }

    override fun observarUidsAmigos(miUid: String): Flow<Set<String>> = callbackFlow {
        val job = launch {
            while (isActive) {
                val uids = cliente.postgrest.from(Tablas.AMIGOS).select {
                    filter { eq("usuario_id", miUid) }
                    limit(500)
                }.decodeList<AmigoFila>().map { it.amigoId }.toSet()
                trySend(uids)
                delay(2500)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun enviarSolicitudAmistad(uidOrigen: String, uidDestino: String): Result<Unit> = runCatching {
        require(uidOrigen != uidDestino) { "No puedes enviarte una solicitud a ti mismo" }
        val id = idSolicitud(uidOrigen, uidDestino)
        val row = buildJsonObject {
            put("id", id)
            put("uid_origen", uidOrigen)
            put("uid_destino", uidDestino)
            put("estado", EstadoSolicitudAmistad.PENDIENTE.name.lowercase())
            put("creado_en", System.currentTimeMillis())
        }
        cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).upsert(row) {
            onConflict = "id"
        }
        Unit
    }

    override suspend fun aceptarSolicitudAmistad(idSolicitud: String): Result<Unit> = runCatching {
        val req = cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).select {
            filter { eq("id", idSolicitud) }
            limit(1)
        }.decodeList<FilaSolicitud>().firstOrNull() ?: error("Solicitud no encontrada")
        val from = req.uidOrigen
        val to = req.uidDestino
        val now = System.currentTimeMillis()
        val a1 = buildJsonObject {
            put("usuario_id", from)
            put("amigo_id", to)
            put("creado_en", now)
        }
        val a2 = buildJsonObject {
            put("usuario_id", to)
            put("amigo_id", from)
            put("creado_en", now)
        }
        cliente.postgrest.from(Tablas.AMIGOS).upsert(a1) { onConflict = "usuario_id,amigo_id" }
        cliente.postgrest.from(Tablas.AMIGOS).upsert(a2) { onConflict = "usuario_id,amigo_id" }
        cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).update({
            set("estado", EstadoSolicitudAmistad.ACEPTADA.name.lowercase())
        }) {
            filter { eq("id", idSolicitud) }
        }
        runCatching {
            incrementarAmigos(from)
            incrementarAmigos(to)
        }
        Unit
    }

    private suspend fun incrementarAmigos(uid: String) {
        val u = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>().firstOrNull() ?: return
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("numero_amigos", u.numeroAmigos + 1)
        }) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun rechazarSolicitudAmistad(idSolicitud: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).update({
            set("estado", EstadoSolicitudAmistad.RECHAZADA.name.lowercase())
        }) {
            filter { eq("id", idSolicitud) }
        }
        Unit
    }

    override suspend fun eliminarAmigo(miUid: String, otroUid: String): Result<Unit> = runCatching {
        cliente.postgrest.from(Tablas.AMIGOS).delete {
            filter {
                eq("usuario_id", miUid)
                eq("amigo_id", otroUid)
            }
        }
        cliente.postgrest.from(Tablas.AMIGOS).delete {
            filter {
                eq("usuario_id", otroUid)
                eq("amigo_id", miUid)
            }
        }
        runCatching {
            decrementarAmigos(miUid)
            decrementarAmigos(otroUid)
        }
        Unit
    }

    private suspend fun decrementarAmigos(uid: String) {
        val u = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>().firstOrNull() ?: return
        val nuevo = (u.numeroAmigos - 1).coerceAtLeast(0)
        cliente.postgrest.from(Tablas.USUARIOS).update({
            set("numero_amigos", nuevo)
        }) {
            filter { eq("id", uid) }
        }
    }

    override suspend fun sonAmigos(miUid: String, otroUid: String): Result<Boolean> = runCatching {
        cliente.postgrest.from(Tablas.AMIGOS).select {
            filter {
                eq("usuario_id", miUid)
                eq("amigo_id", otroUid)
            }
            limit(1)
        }.decodeList<AmigoFila>().isNotEmpty()
    }

    companion object {
        internal fun idSolicitud(from: String, to: String): String = "${from}__$to"
    }

    @Serializable
    private data class AmigoFila(
        val usuarioId: String,
        val amigoId: String,
        val creadoEn: Long = 0,
    )
}

@Singleton
class RepositorioModeracionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioModeracion {

    override suspend fun enviarReporte(
        uidReportante: String,
        tipoObjetivo: TipoObjetivoReporte,
        idObjetivo: String,
        motivo: String,
    ): Result<Unit> = runCatching {
        val row = FilaReporte(
            id = UUID.randomUUID().toString(),
            tipoObjetivo = tipoObjetivo.name.lowercase(),
            idObjetivo = idObjetivo,
            uidReportante = uidReportante,
            motivo = motivo,
            estado = EstadoReporte.ABIERTO.name.lowercase(),
            creadoEn = System.currentTimeMillis(),
        )
        cliente.postgrest.from(Tablas.REPORTES).insert(row)
        Unit
    }

    override fun observarReportesAbiertos(): Flow<List<ReporteContenido>> = callbackFlow {
        val job = launch {
            while (isActive) {
                val list = cliente.postgrest.from(Tablas.REPORTES).select {
                    order("creado_en", Order.DESCENDING)
                    limit(100)
                }.decodeList<FilaReporte>()
                    .map { it.aReporte() }
                    .filter { it.estado == EstadoReporte.ABIERTO }
                trySend(list)
                delay(3000)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun actualizarEstadoReporte(idReporte: String, estado: EstadoReporte): Result<Unit> =
        runCatching {
            cliente.postgrest.from(Tablas.REPORTES).update({
                set("estado", estado.name.lowercase())
            }) {
                filter { eq("id", idReporte) }
            }
            Unit
        }
}

@Singleton
class RepositorioAlmacenamientoSupabase @Inject constructor(
    private val cliente: SupabaseClient,
    @ApplicationContext private val context: Context,
) : RepositorioAlmacenamiento {

    override suspend fun subirFotoPerfil(uid: String, origen: Uri): Result<String> =
        subir("usuarios/$uid/perfil.jpg", origen)

    override suspend fun subirFotoPerro(uidDueno: String, origen: Uri): Result<String> =
        subir("perros/$uidDueno/perro.jpg", origen)

    private suspend fun subir(path: String, origen: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(origen)?.use { it.readBytes() }
            ?: error("No se pudo leer el archivo")
        val bucket = Tablas.BUCKET_MEDIOS
        cliente.storage.from(bucket).upload(path, bytes) {
            upsert = true
        }
        cliente.storage.from(bucket).publicUrl(path)
    }
}

@Singleton
class RepositorioSoporteSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioSoporte {

    override suspend fun enviarTicket(
        uid: String,
        correo: String,
        mensaje: String,
    ): Result<Unit> = runCatching {
        require(mensaje.trim().length >= 10) { "Describe el motivo con al menos 10 caracteres" }
        val row = FilaTicketInsert(
            uid = uid,
            correo = correo,
            mensaje = mensaje.trim(),
            creadoEn = System.currentTimeMillis(),
        )
        cliente.postgrest.from(Tablas.TICKETS_SOPORTE).insert(row)
        Unit
    }
}

@Singleton
class RepositorioDatosDemoSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioDatosDemo {

    override suspend fun sembrarPerfilesDemo(
        centerLat: Double?,
        centerLng: Double?,
    ): Result<Int> = runCatching {
        val baseLat = centerLat ?: DEFAULT_LAT
        val baseLng = centerLng ?: DEFAULT_LNG

        val perfiles = demoDataset()
        perfiles.forEachIndexed { index, p ->
            val (lat, lng) = offsetMeters(baseLat, baseLng, p.offsetNorthMeters, p.offsetEastMeters)
            val uid = RepositorioDatosDemo.DEMO_PREFIX + p.slug
            val ahora = System.currentTimeMillis()
            val userRow = FilaUsuario(
                id = uid,
                correo = "${p.slug}@demo.pawpals",
                nombreVisible = p.nombreVisible,
                zona = p.zona,
                sobreMi = p.sobreMi,
                rol = "usuario",
                bloqueado = false,
                latitud = lat,
                longitud = lng,
                ubicacionActualizadaEn = ahora,
                numeroAmigos = index + 2,
                numeroPaseos = index * 3 + 1,
                numeroCoincidencias = index + 1,
                paseando = index % 3 == 0,
                creadoEn = ahora,
                esDemo = true,
            )
            cliente.postgrest.from(Tablas.USUARIOS).insert(userRow)

            val idPerro = RepositorioDatosDemo.DEMO_PREFIX + "dog_" + p.slug
            val dogRow = FilaPerro(
                id = idPerro,
                uidDueno = uid,
                nombre = p.nombrePerro,
                raza = p.razaPerro,
                edadAnios = p.edadPerro,
                biografia = p.bioPerro,
                urlFoto = null,
                energia = p.energia.name.lowercase(),
                sociabilidad = p.sociabilidad.name.lowercase(),
                actualizadoEn = ahora,
                esDemo = true,
            )
            cliente.postgrest.from(Tablas.PERROS).insert(dogRow)
        }
        perfiles.size
    }

    override suspend fun limpiarPerfilesDemo(): Result<Int> = runCatching {
        val demoRows = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("es_demo", true) }
        }.decodeList<FilaUsuario>()
        cliente.postgrest.from(Tablas.USUARIOS).delete {
            filter { eq("es_demo", true) }
        }
        demoRows.size
    }

    private fun offsetMeters(
        lat: Double,
        lng: Double,
        north: Double,
        east: Double,
    ): Pair<Double, Double> {
        val earthRadius = 6378137.0
        val dLat = north / earthRadius
        val dLng = east / (earthRadius * Math.cos(Math.PI * lat / 180.0))
        return lat + dLat * 180.0 / Math.PI to lng + dLng * 180.0 / Math.PI
    }

    private data class DemoProfile(
        val slug: String,
        val nombreVisible: String,
        val zona: String,
        val sobreMi: String,
        val nombrePerro: String,
        val razaPerro: String,
        val edadPerro: Int,
        val bioPerro: String,
        val energia: NivelEnergia,
        val sociabilidad: Sociabilidad,
        val offsetNorthMeters: Double,
        val offsetEastMeters: Double,
    )

    private fun demoDataset(): List<DemoProfile> = listOf(
        DemoProfile(
            slug = "marta_lopez",
            nombreVisible = "Marta López",
            zona = "Malasaña, Madrid",
            sobreMi = "Educadora canina. Me encantan los paseos largos por la mañana.",
            nombrePerro = "Luna",
            razaPerro = "Labrador",
            edadPerro = 3,
            bioPerro = "Muy cariñosa con otros perros y con niños.",
            energia = NivelEnergia.ALTA,
            sociabilidad = Sociabilidad.MUY_SOCIABLE,
            offsetNorthMeters = 320.0, offsetEastMeters = 180.0,
        ),
        DemoProfile(
            slug = "javier_ruiz",
            nombreVisible = "Javier Ruiz",
            zona = "Chamberí, Madrid",
            sobreMi = "Corredor aficionado, busco compañeros de paseo a primera hora.",
            nombrePerro = "Max",
            razaPerro = "Border Collie",
            edadPerro = 5,
            bioPerro = "Trabajador, le encanta aprender trucos nuevos.",
            energia = NivelEnergia.ALTA,
            sociabilidad = Sociabilidad.SELECTIVO,
            offsetNorthMeters = -420.0, offsetEastMeters = 620.0,
        ),
        DemoProfile(
            slug = "lucia_fernandez",
            nombreVisible = "Lucía Fernández",
            zona = "Lavapiés, Madrid",
            sobreMi = "Me gustan las tardes tranquilas en el parque.",
            nombrePerro = "Coco",
            razaPerro = "Golden Retriever",
            edadPerro = 2,
            bioPerro = "Aún es un cachorro grande, busca amiguitos para jugar.",
            energia = NivelEnergia.MODERADO,
            sociabilidad = Sociabilidad.MUY_SOCIABLE,
            offsetNorthMeters = 700.0, offsetEastMeters = -450.0,
        ),
        DemoProfile(
            slug = "pablo_garcia",
            nombreVisible = "Pablo García",
            zona = "Retiro, Madrid",
            sobreMi = "Trabajo desde casa, flexible para quedar cualquier día.",
            nombrePerro = "Nala",
            razaPerro = "Husky Siberiano",
            edadPerro = 4,
            bioPerro = "Enérgica y habladora. Prefiere perros de tamaño similar.",
            energia = NivelEnergia.ALTA,
            sociabilidad = Sociabilidad.SELECTIVO,
            offsetNorthMeters = -680.0, offsetEastMeters = -260.0,
        ),
        DemoProfile(
            slug = "sara_martin",
            nombreVisible = "Sara Martín",
            zona = "Arganzuela, Madrid",
            sobreMi = "Adoptante y voluntaria en protectoras.",
            nombrePerro = "Rocco",
            razaPerro = "Mestizo",
            edadPerro = 7,
            bioPerro = "Señor tranquilo, disfruta de paseos cortos y miradas largas.",
            energia = NivelEnergia.TRANQUILO,
            sociabilidad = Sociabilidad.MUY_SOCIABLE,
            offsetNorthMeters = 220.0, offsetEastMeters = 920.0,
        ),
        DemoProfile(
            slug = "daniel_perez",
            nombreVisible = "Daniel Pérez",
            zona = "Chamartín, Madrid",
            sobreMi = "Me encanta el senderismo y hacer fotos con Kira.",
            nombrePerro = "Kira",
            razaPerro = "Pastor Alemán",
            edadPerro = 6,
            bioPerro = "Super obediente, ideal para paseos tranquilos y rutas.",
            energia = NivelEnergia.MODERADO,
            sociabilidad = Sociabilidad.SELECTIVO,
            offsetNorthMeters = 980.0, offsetEastMeters = 540.0,
        ),
        DemoProfile(
            slug = "andrea_gomez",
            nombreVisible = "Andrea Gómez",
            zona = "Tetuán, Madrid",
            sobreMi = "Prefiero quedar en parques pequeños con pocos perros.",
            nombrePerro = "Toby",
            razaPerro = "Beagle",
            edadPerro = 4,
            bioPerro = "Curioso y sabueso. Le pierde el olfato.",
            energia = NivelEnergia.MODERADO,
            sociabilidad = Sociabilidad.TIMIDO,
            offsetNorthMeters = 480.0, offsetEastMeters = -900.0,
        ),
        DemoProfile(
            slug = "carlos_dominguez",
            nombreVisible = "Carlos Domínguez",
            zona = "Vallecas, Madrid",
            sobreMi = "Dueño primerizo, abierto a consejos y paseos en grupo.",
            nombrePerro = "Bruno",
            razaPerro = "Bulldog Francés",
            edadPerro = 1,
            bioPerro = "Cachorro muy jugón, le encanta conocer nuevos amigos.",
            energia = NivelEnergia.TRANQUILO,
            sociabilidad = Sociabilidad.MUY_SOCIABLE,
            offsetNorthMeters = -950.0, offsetEastMeters = 150.0,
        ),
    )

    companion object {
        private const val DEFAULT_LAT = 40.4168
        private const val DEFAULT_LNG = -3.7038
    }
}

@Singleton
class RepositorioEstadisticasAdministracionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioEstadisticasAdministracion {

    override suspend fun cargarEstadisticas(): Result<EstadisticasAdministracion> = runCatching {
        val usuarios = cliente.postgrest.from(Tablas.USUARIOS).select {
            limit(5000)
        }.decodeList<FilaUsuario>().size
        val perros = cliente.postgrest.from(Tablas.PERROS).select {
            limit(5000)
        }.decodeList<FilaPerro>().size
        val coincidencias = cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
            limit(5000)
        }.decodeList<FilaCoincidencia>().size
        val reportesAbiertos = cliente.postgrest.from(Tablas.REPORTES).select {
            limit(500)
        }.decodeList<FilaReporte>().map { it.aReporte() }
            .count { it.estado == EstadoReporte.ABIERTO }
        EstadisticasAdministracion(
            numeroUsuarios = usuarios,
            numeroPerros = perros,
            numeroCoincidencias = coincidencias,
            reportesAbiertos = reportesAbiertos,
        )
    }
}

package com.pawpals.app

import android.net.Uri
import kotlinx.coroutines.flow.Flow

// contratos de datos, la app pide estas acciones sin saber como trabaja supabase
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

// fila ligera para listar actividad sin pedir todos los mensajes uno a uno
data class ResumenActividadConversacion(
    val idConversacion: String,
    val ultimoMensajeEn: Long,
    val ultimoRemitenteUid: String?,
    val vistaPrevia: String?,
)

interface RepositorioConversacion {
    fun observarMensajes(idConversacion: String): Flow<List<MensajeConversacion>>
    fun observarResumenesConversacionDondeParticipa(miUid: String): Flow<List<ResumenActividadConversacion>>
    suspend fun enviarMensaje(
        idConversacion: String,
        uidRemitente: String,
        texto: String
    ): Result<Unit>

    companion object {
        fun idConversacionFor(usuarioA: String, usuarioB: String): String =
            idCompuesto(usuarioA, usuarioB, ordenado = true)
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

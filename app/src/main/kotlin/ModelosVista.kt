package com.pawpals.app

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoUiAdministracion(
    val usuarios: List<PerfilUsuario> = emptyList(),
    val estadisticas: EstadisticasAdministracion? = null,
    val reportes: List<ReporteContenido> = emptyList(),
    val cargando: Boolean = false,
    val sembrando: Boolean = false,
    val mensaje: String? = null,
)

@HiltViewModel
// estado y acciones del panel de administracion
class ModeloVistaAdministracion @Inject constructor(
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioEstadisticasAdministracion: RepositorioEstadisticasAdministracion,
    private val repositorioModeracion: RepositorioModeracion,
    private val repositorioDatosDemo: RepositorioDatosDemo,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiAdministracion())
    val estado: StateFlow<EstadoUiAdministracion> = _estado.asStateFlow()

    init {
        refrescarTodo()
        viewModelScope.launch {
            repositorioModeracion.observarReportesAbiertos().collect { list ->
                _estado.update { it.copy(reportes = list) }
            }
        }
    }

    fun refrescarTodo() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensaje = null) }
            val usuarios = repositorioUsuario.obtenerTodosUsuarios().getOrElse { emptyList() }
            val estadisticas = repositorioEstadisticasAdministracion.cargarEstadisticas().getOrNull()
            _estado.update {
                it.copy(usuarios = usuarios, estadisticas = estadisticas, cargando = false)
            }
        }
    }

    fun establecerBloqueado(uid: String, bloqueado: Boolean) {
        viewModelScope.launch {
            val r = repositorioUsuario.establecerBloqueado(uid, bloqueado)
            _estado.update {
                it.copy(
                    mensaje = if (r.isSuccess) null
                    else mensajeErrorSupabaseHumano(
                        r.exceptionOrNull(),
                        "No se pudo cambiar el estado de bloqueo del usuario.",
                    ),
                )
            }
            refrescarTodo()
        }
    }

    fun eliminarUsuario(uid: String) {
        viewModelScope.launch {
            val r = repositorioUsuario.eliminarPerfilUsuario(uid)
            _estado.update {
                it.copy(
                    mensaje = if (r.isSuccess) null
                    else mensajeErrorSupabaseHumano(
                        r.exceptionOrNull(),
                        "No se pudo eliminar el usuario.",
                    ),
                )
            }
            refrescarTodo()
        }
    }

    fun resolverReporte(id: String, accionTomada: Boolean) {
        viewModelScope.launch {
            val estado = if (accionTomada) EstadoReporte.ACCION_TOMADA else EstadoReporte.REVISADO
            repositorioModeracion.actualizarEstadoReporte(id, estado)
        }
    }

    // crea unos perfiles de prueba cerca del admin, o en madrid si no hay ubicacion
    fun sembrarPerfilesDemo() {
        viewModelScope.launch {
            _estado.update { it.copy(sembrando = true, mensaje = null) }
            val yo = repositorioUsuario.obtenerTodosUsuarios().getOrNull()
                ?.firstOrNull { it.rol == RolUsuario.ADMINISTRADOR }
            val result = repositorioDatosDemo.sembrarPerfilesDemo(yo?.latitud, yo?.longitud)
            _estado.update {
                it.copy(
                    sembrando = false,
                    mensaje = result.fold(
                        onSuccess = { count -> "Se han creado $count perfiles de prueba." },
                        onFailure = { e ->
                            mensajeErrorSupabaseHumano(
                                e,
                                "No se pudieron crear los perfiles de prueba.",
                            )
                        },
                    ),
                )
            }
            refrescarTodo()
        }
    }

    // borra los perfiles de prueba que se habian creado
    fun limpiarPerfilesDemo() {
        viewModelScope.launch {
            _estado.update { it.copy(sembrando = true, mensaje = null) }
            val result = repositorioDatosDemo.limpiarPerfilesDemo()
            _estado.update {
                it.copy(
                    sembrando = false,
                    mensaje = result.fold(
                        onSuccess = { count -> "Se han eliminado $count perfiles de prueba." },
                        onFailure = { e ->
                            mensajeErrorSupabaseHumano(
                                e,
                                "No se pudieron eliminar los perfiles de prueba.",
                            )
                        },
                    ),
                )
            }
            refrescarTodo()
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }
}

data class EstadoUiAjustes(
    val notificarMensajes: Boolean = true,
    val notificarPaseos: Boolean = true,
    val notificarCoincidencias: Boolean = true,
    val ubicacionSoloDurantePaseo: Boolean = false,
    val correo: String? = null,
    val ocupado: Boolean = false,
    val info: String? = null,
    val error: String? = null,
)

@HiltViewModel
// mantiene sincronizados los ajustes locales con datastore
class ModeloVistaAjustes @Inject constructor(
    private val preferenciasUsuario: PreferenciasUsuario,
    private val repositorioAutenticacion: RepositorioAutenticacion,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiAjustes())
    val estado: StateFlow<EstadoUiAjustes> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            preferenciasUsuario.notificarMensajes.collect { v -> _estado.update { it.copy(notificarMensajes = v) } }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarPaseos.collect { v -> _estado.update { it.copy(notificarPaseos = v) } }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarCoincidencias.collect { v -> _estado.update { it.copy(notificarCoincidencias = v) } }
        }
        viewModelScope.launch {
            preferenciasUsuario.ubicacionSoloDurantePaseo.collect { v ->
                _estado.update { it.copy(ubicacionSoloDurantePaseo = v) }
            }
        }
        viewModelScope.launch {
            repositorioAutenticacion.estadoAutenticacion.collect { u ->
                _estado.update { it.copy(correo = u?.correo) }
            }
        }
    }

    fun establecerNotificarMensajes(v: Boolean) =
        viewModelScope.launch { preferenciasUsuario.establecerNotificarMensajes(v) }

    fun establecerNotificarPaseos(v: Boolean) =
        viewModelScope.launch { preferenciasUsuario.establecerNotificarPaseos(v) }

    fun establecerNotificarCoincidencias(v: Boolean) =
        viewModelScope.launch { preferenciasUsuario.establecerNotificarCoincidencias(v) }

    fun establecerPrivacidadUbicacion(v: Boolean) =
        viewModelScope.launch { preferenciasUsuario.establecerUbicacionSoloDurantePaseo(v) }

    fun cambiarCorreo(nuevoCorreo: String) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, error = null, info = null) }
            val r = repositorioAutenticacion.actualizarCorreo(nuevoCorreo.trim())
            _estado.update {
                it.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Te hemos enviado un correo a $nuevoCorreo para confirmar el cambio." else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo iniciar el cambio de correo.")
                    },
                )
            }
        }
    }

    fun enviarRestablecimientoContrasena() {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, error = null, info = null) }
            val r = repositorioAutenticacion.enviarCorreoRestablecerContrasena()
            _estado.update {
                it.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Te hemos enviado un correo para restablecer la contraseña." else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo enviar el correo de recuperación.")
                    },
                )
            }
        }
    }

    fun cambiarContrasena(actual: String, nuevaContrasena: String) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, error = null, info = null) }
            val r = repositorioAutenticacion.cambiarContrasena(actual, nuevaContrasena)
            _estado.update {
                it.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Contraseña actualizada correctamente." else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo cambiar la contraseña.")
                    },
                )
            }
        }
    }

    fun limpiarRetroalimentacion() = _estado.update { it.copy(info = null, error = null) }

    fun cerrarSesion() {
        viewModelScope.launch {
            repositorioAutenticacion.cerrarSesion()
        }
    }

    // borra la cuenta despues de volver a comprobar la contrasena
    fun eliminarCuenta(contrasena: String) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, info = null, error = null) }

            if (repositorioAutenticacion.cuentaActual() == null) {
                _estado.update { it.copy(ocupado = false, error = "No hay sesión activa") }
                return@launch
            }

            val r = repositorioAutenticacion.eliminarCuenta(contrasena)
            _estado.update {
                it.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Cuenta eliminada correctamente." else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo eliminar la cuenta. Revisa la contraseña.")
                    },
                )
            }
        }
    }
}

// estado del mensaje que manda un usuario bloqueado a soporte
data class EstadoUiBloqueado(
    val mensaje: String = "",
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ModeloVistaBloqueado @Inject constructor(
    private val repositorioSoporte: RepositorioSoporte,
    private val repositorioAutenticacion: RepositorioAutenticacion,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiBloqueado())
    val estado: StateFlow<EstadoUiBloqueado> = _estado.asStateFlow()

    fun alCambiarMensaje(v: String) = _estado.update { it.copy(mensaje = v, error = null) }

    fun enviarTicket() {
        val actual = _estado.value
        if (actual.enviando) return
        val cuenta = repositorioAutenticacion.cuentaActual() ?: run {
            _estado.update { it.copy(error = "Sesión no disponible, reinicia la app") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            val r = repositorioSoporte.enviarTicket(
                uid = cuenta.uid,
                correo = cuenta.correo.orEmpty(),
                mensaje = actual.mensaje,
            )
            _estado.update {
                if (r.isSuccess) {
                    it.copy(enviando = false, enviado = true, mensaje = "")
                } else {
                    it.copy(
                        enviando = false,
                        error = mensajeErrorSupabaseHumano(
                            r.exceptionOrNull(),
                            "No se pudo enviar el mensaje a soporte.",
                        ),
                    )
                }
            }
        }
    }

    fun reiniciarEnviado() = _estado.update { it.copy(enviado = false) }
}

// datos que se van rellenando en el onboarding de persona y perro
data class EstadoUiConfiguracionPerfil(
    val paso: Int = 0,
    // datos de la persona
    val nombreHumano: String = "",
    val zona: String = "",
    val sobreMi: String = "",
    val uriFotoHumano: Uri? = null,
    // datos del perro
    val nombrePerro: String = "",
    val razaPerro: String = "",
    val edadPerro: String = "",
    val energia: NivelEnergia = NivelEnergia.MODERADO,
    val sociabilidad: Sociabilidad = Sociabilidad.MUY_SOCIABLE,
    val fotoPerroUri: Uri? = null,

    val guardando: Boolean = false,
    val completado: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
// recoge los datos del primer perfil antes de entrar a explorar
class ModeloVistaConfiguracionPerfil @Inject constructor(
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
    private val repositorioAlmacenamiento: RepositorioAlmacenamiento,
    private val repositorioAutenticacion: RepositorioAutenticacion,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiConfiguracionPerfil())
    val estado: StateFlow<EstadoUiConfiguracionPerfil> = _estado.asStateFlow()

    fun establecerNombreHumano(v: String) = _estado.update { it.copy(nombreHumano = v) }
    fun establecerZona(v: String) = _estado.update { it.copy(zona = v) }
    fun establecerSobreMi(v: String) = _estado.update { it.copy(sobreMi = v) }
    fun establecerFotoHumano(uri: Uri?) = _estado.update { it.copy(uriFotoHumano = uri) }

    fun establecerNombrePerroOnboarding(v: String) = _estado.update { it.copy(nombrePerro = v) }
    fun establecerRazaPerro(v: String) = _estado.update { it.copy(razaPerro = v) }
    fun establecerEdadPerro(v: String) = _estado.update {
        it.copy(edadPerro = v.filter { c -> c.isDigit() || c == ' ' || c.isLetter() }.take(12))
    }
    fun establecerEnergia(level: NivelEnergia) = _estado.update { it.copy(energia = level) }
    fun establecerSociabilidad(s: Sociabilidad) = _estado.update { it.copy(sociabilidad = s) }
    fun establecerFotoPerroOnboarding(uri: Uri?) = _estado.update { it.copy(fotoPerroUri = uri) }

    fun pasoSiguiente() = _estado.update { it.copy(paso = (it.paso + 1).coerceAtMost(1)) }
    fun pasoAnteriorOnboarding() = _estado.update { it.copy(paso = (it.paso - 1).coerceAtLeast(0)) }

    fun puedeContinuarHumano(): Boolean {
        val s = _estado.value
        return s.nombreHumano.trim().length >= 2 && s.zona.isNotBlank()
    }

    fun puedeFinalizarOnboarding(): Boolean {
        val s = _estado.value
        return s.nombrePerro.trim().length >= 2 && s.razaPerro.isNotBlank()
    }

    // guarda el perfil completo y sube las fotos si el usuario eligio alguna
    fun finalizarOnboarding(uid: String) {
        viewModelScope.launch {
            val s = _estado.value
            _estado.update { it.copy(guardando = true, error = null) }

            // primero debe existir la fila de usuario, si no el perro falla por la relacion
            val correo = repositorioAutenticacion.cuentaActual()?.correo.orEmpty()
            repositorioUsuario.asegurarDocumentoUsuario(uid, correo).onFailure { e ->
                _estado.update {
                    it.copy(
                        guardando = false,
                        error = mensajeErrorSupabaseHumano(
                            e,
                            "No se pudo crear tu ficha de usuario. Revisa la conexión e inténtalo de nuevo.",
                        ),
                    )
                }
                return@launch
            }

            val humanPhotoUrl = s.uriFotoHumano?.let {
                repositorioAlmacenamiento.subirFotoPerfil(uid, it).getOrNull()
            }
            val urlFotoPerro = s.fotoPerroUri?.let {
                repositorioAlmacenamiento.subirFotoPerro(uid, it).getOrNull()
            }

            val ageDigits = s.edadPerro.filter { it.isDigit() }
            val edadPerroAnios = ageDigits.toIntOrNull()?.coerceIn(1, 25) ?: 1

            val u = repositorioUsuario.actualizarPerfil(
                uid = uid,
                nombreVisible = s.nombreHumano.trim(),
                zona = s.zona.trim(),
                sobreMi = s.sobreMi.trim(),
                urlFoto = humanPhotoUrl,
            )
            val d = repositorioPerro.guardarPerro(
                uidDueno = uid,
                nombre = s.nombrePerro.trim(),
                raza = s.razaPerro.trim(),
                edadAnios = edadPerroAnios,
                biografia = "",
                urlFoto = urlFotoPerro,
                energia = s.energia,
                sociabilidad = s.sociabilidad,
            )
            val fallo = u.exceptionOrNull() ?: d.exceptionOrNull()
            val err = fallo?.let { mensajeErrorSupabaseHumano(it, "No se pudo guardar tu perfil.") }
            _estado.update { it.copy(guardando = false, completado = err == null, error = err) }
        }
    }
}

data class EstadoUiConversacion(
    val mensajes: List<MensajeConversacion> = emptyList(),
    val enviando: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    // solicitud pendiente, si todavia no son amigos
    val solicitudPendiente: SolicitudAmistad? = null,
    // sirve para ocultar el boton de anadir amigo
    val yaSonAmigos: Boolean = false,
    // cuando se rompe la coincidencia, la pantalla vuelve atras
    val coincidenciaDeshecha: Boolean = false,
    // datos rapidos para la cabecera del chat
    val nombreVisibleOtro: String = "",
    val nombrePerroOtro: String = "",
    val urlFotoPerroOtro: String? = null,
    val urlFotoUsuarioOtro: String? = null,
    // perfiles completos para abrir la ficha desde la cabecera
    val perfilUsuarioOtro: PerfilUsuario? = null,
    val perfilPerroOtro: PerfilPerro? = null,
)

@HiltViewModel
// controla mensajes, amistad y datos del otro usuario dentro del chat
class ModeloVistaConversacion @Inject constructor(
    private val repositorioConversacion: RepositorioConversacion,
    private val repositorioAmistad: RepositorioAmistad,
    private val repositorioCoincidencia: RepositorioCoincidencia,
    private val repositorioDeslizamiento: RepositorioDeslizamiento,
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val otroUid: String = checkNotNull(savedStateHandle["otroUid"])

    // valor provisional mientras llegan los datos reales
    val otroUidVistaPrevia: String = otroUid

    private val _estado = MutableStateFlow(EstadoUiConversacion())
    val estado: StateFlow<EstadoUiConversacion> = _estado.asStateFlow()

    private var observando = false

    // empieza a escuchar mensajes, amistad y datos del otro usuario
    fun observar(miUid: String) {
        if (observando) return
        observando = true
        val idConversacion = RepositorioConversacion.idConversacionFor(miUid, otroUid)
        viewModelScope.launch {
            repositorioConversacion.observarMensajes(idConversacion).collect { list ->
                _estado.update { it.copy(mensajes = list) }
            }
        }
        viewModelScope.launch {
            repositorioAmistad.observarSolicitudEntre(miUid, otroUid).collect { req ->
                _estado.update { it.copy(solicitudPendiente = req) }
            }
        }
        viewModelScope.launch {
            repositorioAmistad.observarUidsAmigos(miUid).collect { amigos ->
                _estado.update { it.copy(yaSonAmigos = otroUid in amigos) }
            }
        }
        viewModelScope.launch {
            repositorioUsuario.observarUsuario(otroUid).collect { u ->
                _estado.update {
                    it.copy(
                        nombreVisibleOtro = u?.nombreVisible.orEmpty(),
                        urlFotoUsuarioOtro = u?.urlFoto,
                        perfilUsuarioOtro = u,
                    )
                }
            }
        }
        viewModelScope.launch {
            repositorioPerro.observarPerroDeDueno(otroUid).collect { d ->
                _estado.update {
                    it.copy(
                        nombrePerroOtro = d?.nombre.orEmpty(),
                        urlFotoPerroOtro = d?.urlFoto,
                        perfilPerroOtro = d,
                    )
                }
            }
        }
    }

    fun enviar(miUid: String, texto: String) {
        if (texto.isBlank()) return
        val idConversacion = RepositorioConversacion.idConversacionFor(miUid, otroUid)
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            val r = repositorioConversacion.enviarMensaje(idConversacion, miUid, texto.trim())
            _estado.update {
                it.copy(
                    enviando = false,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo enviar el mensaje.")
                    },
                )
            }
        }
    }

    // manda una solicitud de amistad al otro usuario
    fun solicitarAmistad(miUid: String) {
        viewModelScope.launch {
            val r = repositorioAmistad.enviarSolicitudAmistad(miUid, otroUid)
            _estado.update {
                it.copy(
                    info = if (r.isSuccess) "Solicitud de amistad enviada" else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo enviar la solicitud de amistad.")
                    },
                )
            }
        }
    }

    fun aceptarSolicitudAmistad() {
        val req = _estado.value.solicitudPendiente ?: return
        viewModelScope.launch {
            val r = repositorioAmistad.aceptarSolicitudAmistad(req.id)
            _estado.update {
                it.copy(
                    info = if (r.isSuccess) "¡Ahora sois amigos! Verás su ubicación cuando pasee." else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo aceptar la solicitud.")
                    },
                )
            }
        }
    }

    fun rechazarSolicitudAmistad() {
        val req = _estado.value.solicitudPendiente ?: return
        viewModelScope.launch {
            val r = repositorioAmistad.rechazarSolicitudAmistad(req.id)
            _estado.update {
                it.copy(
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo rechazar la solicitud.")
                    },
                )
            }
        }
    }

    fun eliminarAmigo(miUid: String) {
        viewModelScope.launch {
            val r = repositorioAmistad.eliminarAmigo(miUid, otroUid)
            _estado.update {
                it.copy(
                    info = if (r.isSuccess) "Amigo eliminado" else null,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo eliminar de amigos.")
                    },
                )
            }
        }
    }

    // deshace la coincidencia y permite que ambos puedan volver a encontrarse
    fun deshacerCoincidencia(miUid: String) {
        viewModelScope.launch {
            val coincidencia = repositorioCoincidencia.buscarCoincidencia(miUid, otroUid).getOrNull()
            if (coincidencia != null) {
                repositorioCoincidencia.eliminarCoincidencia(coincidencia.id)
            }
            repositorioDeslizamiento.limpiarEntre(miUid, otroUid)
            _estado.update { it.copy(coincidenciaDeshecha = true, info = "Coincidencia deshecha") }
        }
    }

    fun limpiarInfo() = _estado.update { it.copy(info = null, error = null) }
}

// tarjeta que junta dueno, perro y distancia para explorar
data class TarjetaExplorar(
    val usuario: PerfilUsuario,
    val perro: PerfilPerro?,
    val distanciaMetros: Double?,
)

data class EstadoUiExplorar(
    val cargando: Boolean = false,
    val tarjetas: List<TarjetaExplorar> = emptyList(),
    val indice: Int = 0,
    val error: String? = null,
    // se rellena cuando el me gusta ha sido mutuo
    val ultimoUidCoincidencia: String? = null,
    // texto corto para avisos despues de deslizar
    val mensajeInfo: String? = null,
) {
    val actual: TarjetaExplorar? get() = tarjetas.getOrNull(indice)
    val hayMas: Boolean get() = indice < tarjetas.size
}

@HiltViewModel
// prepara las tarjetas de explorar y registra cada decision del usuario
class ModeloVistaExplorar @Inject constructor(
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
    private val repositorioDeslizamiento: RepositorioDeslizamiento,
    private val controladorUbicacion: ControladorUbicacion,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiExplorar())
    val estado: StateFlow<EstadoUiExplorar> = _estado.asStateFlow()

    // carga candidatos quitando los perfiles que ya se han deslizado
    fun cargar(miUid: String) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            // ubicacion actual para ordenar por cercania y actualizar nuestro perfil
            val loc = controladorUbicacion.ultimaUbicacionConocidaOFresca().getOrNull()
            val myLat = loc?.first
            val myLng = loc?.second
            if (loc != null) repositorioUsuario.actualizarUbicacion(miUid, loc.first, loc.second)

            val yaDeslizados = repositorioDeslizamiento.cargarUidsDeslizados(miUid).getOrElse { emptySet() }

            val usuarios = repositorioUsuario.obtenerTodosUsuarios().getOrElse { emptyList() }
                .filter { it.uid != miUid && !it.bloqueado && it.uid !in yaDeslizados }
            val perros = repositorioPerro.obtenerTodosPerros().getOrElse { emptyList() }
                .associateBy { it.uidDueno }

            val tarjetas = usuarios.map { u ->
                val d = if (myLat != null && myLng != null && u.latitud != null && u.longitud != null) {
                    UtilidadesGeograficas.distanciaKm(myLat, myLng, u.latitud, u.longitud) * 1000.0
                } else null
                TarjetaExplorar(usuario = u, perro = perros[u.uid], distanciaMetros = d)
            }.sortedBy { it.distanciaMetros ?: Double.MAX_VALUE }

            _estado.update { it.copy(cargando = false, tarjetas = tarjetas, indice = 0) }
        }
    }

    // descarta la tarjeta actual
    fun rechazar(miUid: String) = deslizar(miUid, AccionDeslizamiento.DESCARTAR)

    // da me gusta y si la otra persona tambien lo hizo, hay coincidencia
    fun meGusta(miUid: String) = deslizar(miUid, AccionDeslizamiento.ME_GUSTA)

    // marca un me gusta mas destacado
    fun superMeGusta(miUid: String) = deslizar(miUid, AccionDeslizamiento.SUPER_ME_GUSTA)

    private fun deslizar(miUid: String, accion: AccionDeslizamiento) {
        val card = _estado.value.actual ?: return
        val uidObjetivo = card.usuario.uid
        viewModelScope.launch {
            val r = repositorioDeslizamiento.registrarDeslizamiento(miUid, uidObjetivo, accion)
            _estado.update {
                val (ultimaCoincidencia, info) = when (val resultado = r.getOrNull()) {
                    is ResultadoDeslizamiento.CoincidenciaCreada -> uidObjetivo to null
                    ResultadoDeslizamiento.Guardado -> null to when (accion) {
                        AccionDeslizamiento.ME_GUSTA -> "Genial, le avisaremos si también te da me gusta"
                        AccionDeslizamiento.SUPER_ME_GUSTA -> "Super-me gusta enviado: recibirá una notificación destacada"
                        AccionDeslizamiento.DESCARTAR -> null
                    }
                    null -> null to null
                }
                it.copy(
                    indice = it.indice + 1,
                    ultimoUidCoincidencia = ultimaCoincidencia,
                    mensajeInfo = info,
                    error = r.exceptionOrNull()?.let {
                        mensajeErrorSupabaseHumano(it, "No se pudo registrar tu respuesta.")
                    },
                )
            }
        }
    }

    fun limpiarNavegacionCoincidencia() = _estado.update { it.copy(ultimoUidCoincidencia = null) }
    fun limpiarInfo() = _estado.update { it.copy(mensajeInfo = null) }
}

// fila de chat con nombres y fotos, no solo ids
data class EntradaListaConversaciones(
    val coincidencia: Coincidencia,
    val otroUid: String,
    val nombreDueno: String,
    val nombrePerro: String,
    val urlFotoPerro: String?,
    val urlFotoDueno: String?,
)

data class EstadoUiListaConversaciones(
    val entradasAceptadas: List<EntradaListaConversaciones> = emptyList(),
)

@HiltViewModel
// monta la lista de chats con nombres y fotos en lugar de ids
class ModeloVistaListaConversaciones @Inject constructor(
    private val repositorioCoincidencia: RepositorioCoincidencia,
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiListaConversaciones())
    val estado: StateFlow<EstadoUiListaConversaciones> = _estado.asStateFlow()

    private val tareasDetalle = mutableMapOf<String, Job>()

    fun observar(miUid: String) {
        viewModelScope.launch {
            repositorioCoincidencia.observarCoincidenciasDeUsuario(miUid).collect { list ->
                val coincidenciasAceptadas = list.filter { it.estado == EstadoCoincidencia.ACEPTADA }
                val base = coincidenciasAceptadas.map { m ->
                    val otroUid = if (m.usuarioA == miUid) m.usuarioB else m.usuarioA
                    val existing = _estado.value.entradasAceptadas.firstOrNull { it.coincidencia.id == m.id }
                    existing?.copy(coincidencia = m) ?: EntradaListaConversaciones(
                        coincidencia = m,
                        otroUid = otroUid,
                        nombreDueno = "",
                        nombrePerro = "",
                        urlFotoPerro = null,
                        urlFotoDueno = null,
                    )
                }
                _estado.update { it.copy(entradasAceptadas = base) }

                // dejamos solo las escuchas que siguen haciendo falta
                val activeUids = base.map { it.otroUid }.toSet()
                tareasDetalle.keys.filter { it !in activeUids }.forEach { old ->
                    tareasDetalle.remove(old)?.cancel()
                }
                activeUids.forEach { uid -> watchDetails(uid) }
            }
        }
    }

    private fun watchDetails(otroUid: String) {
        if (tareasDetalle.containsKey(otroUid)) return
        val job = viewModelScope.launch {
            launch {
                repositorioUsuario.observarUsuario(otroUid).collect { u ->
                    _estado.update { s ->
                        s.copy(
                            entradasAceptadas = s.entradasAceptadas.map { entry ->
                                if (entry.otroUid == otroUid) {
                                    entry.copy(
                                        nombreDueno = u?.nombreVisible.orEmpty(),
                                        urlFotoDueno = u?.urlFoto,
                                    )
                                } else entry
                            },
                        )
                    }
                }
            }
            launch {
                repositorioPerro.observarPerroDeDueno(otroUid).collect { d ->
                    _estado.update { s ->
                        s.copy(
                            entradasAceptadas = s.entradasAceptadas.map { entry ->
                                if (entry.otroUid == otroUid) {
                                    entry.copy(
                                        nombrePerro = d?.nombre.orEmpty(),
                                        urlFotoPerro = d?.urlFoto,
                                    )
                                } else entry
                            },
                        )
                    }
                }
            }
        }
        tareasDetalle[otroUid] = job
    }

    override fun onCleared() {
        super.onCleared()
        tareasDetalle.values.forEach { it.cancel() }
        tareasDetalle.clear()
    }
}

data class NearbyUserUi(
    val perfil: PerfilUsuario,
    val distanciaKm: Double,
)

data class EstadoUiMapa(
    val cargando: Boolean = false,
    val miUbicacion: LatLng? = null,
    val amigosPaseando: List<PerfilUsuario> = emptyList(),
    val yoPaseando: Boolean = false,
    val mensaje: String? = null,
    val snack: String? = null,
)

@HiltViewModel
// controla el mapa de paseos y limita la ubicacion a amigos confirmados
class ModeloVistaMapa @Inject constructor(
    private val controladorUbicacion: ControladorUbicacion,
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioAmistad: RepositorioAmistad,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiMapa())
    val estado: StateFlow<EstadoUiMapa> = _estado.asStateFlow()

    fun tienePermisoUbicacion(): Boolean = controladorUbicacion.tienePermiso()

    fun cargarCercanos(miUid: String) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val loc = controladorUbicacion.ultimaUbicacionConocidaOFresca()
            val pair = loc.getOrNull()
            if (pair == null) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        mensaje = mensajeErrorSupabaseHumano(
                            loc.exceptionOrNull(),
                            "No pudimos obtener tu ubicación. Revisa los permisos.",
                        ),
                    )
                }
                return@launch
            }
            val (lat, lng) = pair
            repositorioUsuario.actualizarUbicacion(miUid, lat, lng)

            val amigos = repositorioAmistad.observarUidsAmigos(miUid).first()
            val all = repositorioUsuario.obtenerTodosUsuarios().getOrElse { emptyList() }
            val amigosPaseando = all.filter {
                it.uid in amigos && !it.bloqueado && it.paseando &&
                    it.latitud != null && it.longitud != null
            }.sortedBy { f ->
                val fLat = f.latitud ?: return@sortedBy Double.MAX_VALUE
                val fLng = f.longitud ?: return@sortedBy Double.MAX_VALUE
                UtilidadesGeograficas.distanciaKm(lat, lng, fLat, fLng)
            }
            val yo = all.firstOrNull { it.uid == miUid }?.paseando ?: false

            _estado.update {
                it.copy(
                    cargando = false,
                    miUbicacion = LatLng(lat, lng),
                    amigosPaseando = amigosPaseando,
                    yoPaseando = yo,
                )
            }
        }
    }

    // cambia entre paseando y no paseando, y suma el paseo al terminar
    fun alternarPaseo(miUid: String) {
        viewModelScope.launch {
            val estabaPaseando = _estado.value.yoPaseando
            val nuevoPaseando = !estabaPaseando
            val r = repositorioUsuario.establecerPaseando(miUid, nuevoPaseando)
            if (r.isSuccess && estabaPaseando && !nuevoPaseando) {
                repositorioUsuario.incrementarNumeroPaseos(miUid)
            }
            _estado.update {
                it.copy(
                    yoPaseando = nuevoPaseando,
                    snack = if (r.isFailure) {
                        mensajeErrorSupabaseHumano(
                            r.exceptionOrNull(),
                            "No se pudo actualizar el estado del paseo.",
                        )
                    } else {
                        if (nuevoPaseando) "Paseo iniciado. ¡Disfrutad!" else "Paseo finalizado."
                    },
                )
            }
        }
    }

    fun limpiarMensajeSnack() = _estado.update { it.copy(snack = null) }
}

data class EstadoUiPerfil(
    val usuario: PerfilUsuario? = null,
    val perro: PerfilPerro? = null,
    val numeroAmigos: Int = 0,
    val nombreVisible: String = "",
    val zona: String = "",
    val sobreMi: String = "",
    val nombrePerro: String = "",
    val razaPerro: String = "",
    val edadPerro: String = "1",
    val bioPerro: String = "",
    val energia: NivelEnergia = NivelEnergia.MODERADO,
    val sociabilidad: Sociabilidad = Sociabilidad.MUY_SOCIABLE,
    val editando: Boolean = false,
    val guardandoPerfil: Boolean = false,
    val notificarMensajes: Boolean = true,
    val notificarPaseos: Boolean = true,
    val mensaje: String? = null,
)

@HiltViewModel
// escucha el perfil propio y permite editar humano, perro y fotos
class ModeloVistaPerfil @Inject constructor(
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
    private val repositorioAmistad: RepositorioAmistad,
    private val repositorioAutenticacion: RepositorioAutenticacion,
    private val repositorioAlmacenamiento: RepositorioAlmacenamiento,
    private val preferenciasUsuario: PreferenciasUsuario,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiPerfil())
    val estado: StateFlow<EstadoUiPerfil> = _estado.asStateFlow()

    fun observar(uid: String) {
        viewModelScope.launch {
            repositorioUsuario.observarUsuario(uid).collect { u ->
                _estado.update {
                    it.copy(
                        usuario = u,
                        numeroAmigos = u?.numeroAmigos ?: it.numeroAmigos,
                        nombreVisible = u?.nombreVisible.orEmpty(),
                        zona = u?.zona.orEmpty(),
                        sobreMi = u?.sobreMi.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            repositorioPerro.observarPerroDeDueno(uid).collect { d ->
                _estado.update {
                    it.copy(
                        perro = d,
                        nombrePerro = d?.nombre.orEmpty(),
                        razaPerro = d?.raza.orEmpty(),
                        edadPerro = d?.edadAnios?.toString().orEmpty().ifBlank { "1" },
                        bioPerro = d?.biografia.orEmpty(),
                        energia = d?.energia ?: NivelEnergia.MODERADO,
                        sociabilidad = d?.sociabilidad ?: Sociabilidad.MUY_SOCIABLE,
                    )
                }
            }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarMensajes.collect { v -> _estado.update { it.copy(notificarMensajes = v) } }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarPaseos.collect { v -> _estado.update { it.copy(notificarPaseos = v) } }
        }
        viewModelScope.launch {
            repositorioAmistad.observarUidsAmigos(uid).collect { amigos ->
                _estado.update { it.copy(numeroAmigos = amigos.size) }
            }
        }
    }

    fun alternarEdicion() = _estado.update { it.copy(editando = !it.editando) }

    fun alCambiarNombreVisible(v: String) = _estado.update { it.copy(nombreVisible = v) }
    fun alCambiarZona(v: String) = _estado.update { it.copy(zona = v) }
    fun alCambiarSobreMi(v: String) = _estado.update { it.copy(sobreMi = v) }
    fun alCambiarNombrePerro(v: String) = _estado.update { it.copy(nombrePerro = v) }
    fun alCambiarRazaPerro(v: String) = _estado.update { it.copy(razaPerro = v) }
    fun alCambiarEdadPerro(v: String) = _estado.update {
        it.copy(edadPerro = v.filter { ch -> ch.isDigit() }.take(2))
    }
    fun alCambiarBioPerro(v: String) = _estado.update { it.copy(bioPerro = v) }

    fun alternarNotificarMensajes() {
        viewModelScope.launch {
            preferenciasUsuario.establecerNotificarMensajes(!_estado.value.notificarMensajes)
        }
    }
    fun alternarNotificarPaseos() {
        viewModelScope.launch {
            preferenciasUsuario.establecerNotificarPaseos(!_estado.value.notificarPaseos)
        }
    }

    fun guardarPerfil(uid: String) {
        viewModelScope.launch {
            _estado.update { it.copy(guardandoPerfil = true, mensaje = null) }
            val s = _estado.value
            val edadPerroAnios = s.edadPerro.toIntOrNull()?.coerceIn(1, 25) ?: 1
            val u = repositorioUsuario.actualizarPerfil(
                uid = uid,
                nombreVisible = s.nombreVisible.trim(),
                zona = s.zona.trim(),
                sobreMi = s.sobreMi.trim(),
                urlFoto = s.usuario?.urlFoto,
            )
            val d = repositorioPerro.guardarPerro(
                uidDueno = uid,
                nombre = s.nombrePerro.trim().ifBlank { "Mi perro" },
                raza = s.razaPerro.trim().ifBlank { "Mestizo" },
                edadAnios = edadPerroAnios,
                biografia = s.bioPerro.trim(),
                urlFoto = s.perro?.urlFoto,
                energia = s.energia,
                sociabilidad = s.sociabilidad,
            )
            val fallo = u.exceptionOrNull() ?: d.exceptionOrNull()
            val err = fallo?.let { mensajeErrorSupabaseHumano(it, "No se pudo guardar el perfil.") }
            _estado.update {
                it.copy(
                    guardandoPerfil = false,
                    editando = false,
                    mensaje = err ?: "Perfil actualizado",
                )
            }
        }
    }

    suspend fun cerrarSesion() {
        repositorioAutenticacion.cerrarSesion()
    }

    // sube la foto del usuario y guarda la url nueva
    fun actualizarFotoUsuario(uid: String, uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(guardandoPerfil = true) }
            val upload = repositorioAlmacenamiento.subirFotoPerfil(uid, uri)
            val url = upload.getOrNull()
            if (url != null) {
                val s = _estado.value
                repositorioUsuario.actualizarPerfil(
                    uid = uid,
                    nombreVisible = s.usuario?.nombreVisible.orEmpty(),
                    zona = s.usuario?.zona.orEmpty(),
                    sobreMi = s.usuario?.sobreMi.orEmpty(),
                    urlFoto = url,
                )
            }
            _estado.update {
                it.copy(
                    guardandoPerfil = false,
                    mensaje = if (url != null) "Foto actualizada" else "No se pudo subir la foto",
                )
            }
        }
    }

    // sube la foto del perro y actualiza su ficha
    fun actualizarFotoPerro(uid: String, uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(guardandoPerfil = true) }
            val upload = repositorioAlmacenamiento.subirFotoPerro(uid, uri)
            val url = upload.getOrNull()
            if (url != null) {
                val s = _estado.value
                repositorioPerro.guardarPerro(
                    uidDueno = uid,
                    nombre = s.perro?.nombre.orEmpty().ifBlank { "Mi perro" },
                    raza = s.perro?.raza.orEmpty().ifBlank { "Mestizo" },
                    edadAnios = s.perro?.edadAnios ?: 1,
                    biografia = s.perro?.biografia.orEmpty(),
                    urlFoto = url,
                    energia = s.perro?.energia ?: NivelEnergia.MODERADO,
                    sociabilidad = s.perro?.sociabilidad ?: Sociabilidad.MUY_SOCIABLE,
                )
            }
            _estado.update {
                it.copy(
                    guardandoPerfil = false,
                    mensaje = if (url != null) "Foto del perro actualizada" else "No se pudo subir la foto",
                )
            }
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }
}

@HiltViewModel
// punto central que decide si se ve login, onboarding, app, admin o bloqueo
class ModeloVistaRaiz @Inject constructor(
    private val repositorioAutenticacion: RepositorioAutenticacion,
    private val repositorioUsuario: RepositorioUsuario,
) : ViewModel() {

    fun cerrarSesion() {
        viewModelScope.launch {
            repositorioAutenticacion.cerrarSesion()
        }
    }

    private val _estadoSesion = MutableStateFlow<EstadoUiSesion>(EstadoUiSesion.Cargando)
    val estadoSesion: StateFlow<EstadoUiSesion> = _estadoSesion.asStateFlow()

    init {
        viewModelScope.launch {
            repositorioAutenticacion.estadoAutenticacion.collectLatest { cuenta ->
                if (cuenta == null) {
                    _estadoSesion.value = EstadoUiSesion.SinSesion
                } else {
                    repositorioUsuario.asegurarDocumentoUsuario(cuenta.uid, cuenta.correo.orEmpty())
                    repositorioUsuario.observarUsuario(cuenta.uid).collect { perfil ->
                        _estadoSesion.value = mapSession(cuenta, perfil)
                    }
                }
            }
        }
    }

    private fun mapSession(cuenta: CuentaAuth, perfil: PerfilUsuario?): EstadoUiSesion {
        if (perfil?.bloqueado == true) return EstadoUiSesion.Bloqueado
        val correo = cuenta.correo.orEmpty()
        val esAdmin = perfil?.rol == RolUsuario.ADMINISTRADOR &&
            correo.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)
        return if (esAdmin) {
            EstadoUiSesion.Administrador(cuenta, perfil)
        } else {
            EstadoUiSesion.SesionUsuario(cuenta, perfil)
        }
    }
}

sealed interface EstadoUiSesion {
    data object Cargando : EstadoUiSesion
    data object SinSesion : EstadoUiSesion
    data object Bloqueado : EstadoUiSesion
    data class SesionUsuario(val cuenta: CuentaAuth, val perfil: PerfilUsuario?) : EstadoUiSesion
    data class Administrador(val cuenta: CuentaAuth, val perfil: PerfilUsuario?) : EstadoUiSesion
}

data class EstadoUiReporte(
    val enviando: Boolean = false,
    val completado: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
// envia reportes de usuario, perro o mensaje a moderacion
class ModeloVistaReporte @Inject constructor(
    private val repositorioModeracion: RepositorioModeracion,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tipoObjetivoRaw: String = checkNotNull(savedStateHandle["tipoObjetivo"])
    private val idObjetivo: String = checkNotNull(savedStateHandle["idObjetivo"])

    private val _estado = MutableStateFlow(EstadoUiReporte())
    val estado: StateFlow<EstadoUiReporte> = _estado.asStateFlow()

    val etiquetaObjetivo: String = "$tipoObjetivoRaw · $idObjetivo"

    fun enviar(uidReportante: String, motivo: String) {
        val type = when (tipoObjetivoRaw.lowercase()) {
            "mensaje" -> TipoObjetivoReporte.MENSAJE
            "perro" -> TipoObjetivoReporte.PERRO
            else -> TipoObjetivoReporte.USUARIO
        }
        viewModelScope.launch {
            _estado.value = EstadoUiReporte(enviando = true)
            val r = repositorioModeracion.enviarReporte(
                uidReportante = uidReportante,
                tipoObjetivo = type,
                idObjetivo = idObjetivo,
                motivo = motivo.trim(),
            )
            _estado.value = if (r.isSuccess) {
                EstadoUiReporte(completado = true)
            } else {
                EstadoUiReporte(
                    error = mensajeErrorSupabaseHumano(
                        r.exceptionOrNull(),
                        "No se pudo enviar el reporte.",
                    ),
                )
            }
        }
    }
}

// datos que se muestran en la pantalla de coincidencia
data class EstadoUiResultadoCoincidencia(
    val nombreOtroUsuario: String = "",
    val nombrePerro: String = "",
    val urlFotoUsuario: String? = null,
    val urlFotoPerro: String? = null,
)

@HiltViewModel
// carga los datos que se ven al celebrar una coincidencia
class ModeloVistaResultadoCoincidencia @Inject constructor(
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiResultadoCoincidencia())
    val estado: StateFlow<EstadoUiResultadoCoincidencia> = _estado.asStateFlow()

    fun cargar(otroUid: String) {
        viewModelScope.launch {
            repositorioUsuario.observarUsuario(otroUid).collect { u ->
                _estado.update {
                    it.copy(
                        nombreOtroUsuario = u?.nombreVisible.orEmpty(),
                        urlFotoUsuario = u?.urlFoto,
                    )
                }
            }
        }
        viewModelScope.launch {
            repositorioPerro.observarPerroDeDueno(otroUid).collect { d ->
                _estado.update {
                    it.copy(
                        nombrePerro = d?.nombre.orEmpty(),
                        urlFotoPerro = d?.urlFoto,
                    )
                }
            }
        }
    }
}

data class EstadoUiAutenticacion(
    val cargando: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    // si el registro pide verificar correo, volvemos al login
    val cambiarAPestanaInicioSesion: Boolean = false,
)

@HiltViewModel
// gestiona login, registro y recuperacion de contrasena
class ModeloVistaAutenticacion @Inject constructor(
    private val repositorioAutenticacion: RepositorioAutenticacion,
    private val repositorioUsuario: RepositorioUsuario,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiAutenticacion())
    val estado: StateFlow<EstadoUiAutenticacion> = _estado.asStateFlow()

    fun iniciarSesion(correo: String, contrasena: String) {
        viewModelScope.launch {
            _estado.value = EstadoUiAutenticacion(cargando = true)
            val r = repositorioAutenticacion.iniciarSesionCorreo(correo.trim(), contrasena)
            _estado.value = if (r.isSuccess) {
                val u = repositorioAutenticacion.cuentaActual()
                if (u != null) {
                    repositorioUsuario.asegurarDocumentoUsuario(u.uid, u.correo.orEmpty())
                }
                EstadoUiAutenticacion()
            } else {
                EstadoUiAutenticacion(
                    error = mensajeErrorSupabaseHumano(
                        r.exceptionOrNull(),
                        "No pudimos iniciar sesión. Revisa correo y contraseña.",
                    ),
                )
            }
        }
    }

    fun registrar(correo: String, contrasena: String) {
        viewModelScope.launch {
            val correoTrim = correo.trim()
            _estado.value = EstadoUiAutenticacion(cargando = true)
            val r = repositorioAutenticacion.registrarCorreo(correoTrim, contrasena)
            _estado.value = when {
                r.isFailure -> EstadoUiAutenticacion(
                    error = mensajeErrorSupabaseHumano(
                        r.exceptionOrNull(),
                        "No pudimos crear la cuenta. Inténtalo de nuevo.",
                    ),
                )
                r.getOrNull()?.sesionActiva == true -> {
                    val u = repositorioAutenticacion.cuentaActual()
                    if (u != null) {
                        repositorioUsuario.asegurarDocumentoUsuario(u.uid, u.correo.orEmpty())
                    }
                    EstadoUiAutenticacion()
                }
                else -> {
                    val c = r.getOrNull()?.correo ?: correoTrim
                    EstadoUiAutenticacion(
                        info = "Cuenta creada correctamente. Te hemos enviado un correo a $c. " +
                            "Abre el enlace y confirma tu correo antes de iniciar sesión.",
                        cambiarAPestanaInicioSesion = true,
                    )
                }
            }
        }
    }

    // manda el correo para recuperar la contrasena
    fun restablecerContrasena(correo: String) {
        viewModelScope.launch {
            _estado.update {
                it.copy(cargando = true, error = null, info = null, cambiarAPestanaInicioSesion = false)
            }
            val r = repositorioAutenticacion.enviarCorreoRestablecerContrasena(correo)
            _estado.update {
                if (r.isSuccess) {
                    it.copy(cargando = false, info = "Te hemos enviado un correo para restablecer la contraseña")
                } else {
                    it.copy(
                        cargando = false,
                        error = mensajeErrorSupabaseHumano(
                            r.exceptionOrNull(),
                            "No se pudo enviar el correo de recuperación.",
                        ),
                    )
                }
            }
        }
    }

    fun limpiarRetroalimentacion() = _estado.update {
        it.copy(info = null, error = null, cambiarAPestanaInicioSesion = false)
    }

    fun consumirCambioPestanaTrasRegistro() =
        _estado.update { it.copy(cambiarAPestanaInicioSesion = false) }
}

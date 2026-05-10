package com.pawpals.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoUiAutenticacion(
    val cargando: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    // si el registro pide verificar correo, volvemos al login
    val cambiarAPestanaInicioSesion: Boolean = false,
)

@HiltViewModel
// gestiona login, registro y recuperacion de contraseña
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
                it.copy(
                    cargando = true,
                    error = null,
                    info = null,
                    cambiarAPestanaInicioSesion = false
                )
            }
            val r = repositorioAutenticacion.enviarCorreoRestablecerContrasena(correo)
            _estado.update {
                if (r.isSuccess) {
                    it.copy(
                        cargando = false,
                        info = "Te hemos enviado un correo para restablecer la contraseña"
                    )
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
            preferenciasUsuario.notificarMensajes.collect { v ->
                _estado.update {
                    it.copy(
                        notificarMensajes = v
                    )
                }
            }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarPaseos.collect { v ->
                _estado.update {
                    it.copy(
                        notificarPaseos = v
                    )
                }
            }
        }
        viewModelScope.launch {
            preferenciasUsuario.notificarCoincidencias.collect { v ->
                _estado.update {
                    it.copy(
                        notificarCoincidencias = v
                    )
                }
            }
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
            _estado.update { estadoActual ->
                estadoActual.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Te hemos enviado un correo a $nuevoCorreo para confirmar el cambio." else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo iniciar el cambio de correo.")
                    },
                )
            }
        }
    }

    fun enviarRestablecimientoContrasena() {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, error = null, info = null) }
            val r = repositorioAutenticacion.enviarCorreoRestablecerContrasena()
            _estado.update { estadoActual ->
                estadoActual.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Te hemos enviado un correo para restablecer la contraseña." else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(
                            error,
                            "No se pudo enviar el correo de recuperación."
                        )
                    },
                )
            }
        }
    }

    fun cambiarContrasena(actual: String, nuevaContrasena: String) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, error = null, info = null) }
            val r = repositorioAutenticacion.cambiarContrasena(actual, nuevaContrasena)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Contraseña actualizada correctamente." else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo cambiar la contraseña.")
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

    // borra la cuenta despues de volver a comprobar la contraseña
    fun eliminarCuenta(contrasena: String) {
        viewModelScope.launch {
            _estado.update { it.copy(ocupado = true, info = null, error = null) }

            if (repositorioAutenticacion.cuentaActual() == null) {
                _estado.update { it.copy(ocupado = false, error = "No hay sesión activa") }
                return@launch
            }

            val r = repositorioAutenticacion.eliminarCuenta(contrasena)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    ocupado = false,
                    info = if (r.isSuccess) "Cuenta eliminada correctamente." else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(
                            error,
                            "No se pudo eliminar la cuenta. Revisa la contraseña."
                        )
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
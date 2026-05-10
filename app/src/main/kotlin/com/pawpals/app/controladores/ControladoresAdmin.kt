package com.pawpals.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            val estadisticas =
                repositorioEstadisticasAdministracion.cargarEstadisticas().getOrNull()
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

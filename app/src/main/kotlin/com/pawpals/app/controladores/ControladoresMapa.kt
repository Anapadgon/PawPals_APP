package com.pawpals.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class EstadoUiMapa(
    val cargando: Boolean = false,
    val miUbicacion: LatLng? = null,
    val amigosPaseando: List<PerfilUsuario> = emptyList(),
    val yoPaseando: Boolean = false,
    val mensaje: String? = null,
    val snack: String? = null,
)

@HiltViewModel
// controla el mapa de paseos y limita la ubicacion a amigos agregados
class ModeloVistaMapa @Inject constructor(
    private val controladorUbicacion: ControladorUbicacion,
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioAmistad: RepositorioAmistad,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiMapa())
    val estado: StateFlow<EstadoUiMapa> = _estado.asStateFlow()
    private var cargaCercanosJob: Job? = null

    fun tienePermisoUbicacion(): Boolean = controladorUbicacion.tienePermiso()

    fun cargarCercanos(miUid: String) {
        cargaCercanosJob?.cancel()
        cargaCercanosJob = viewModelScope.launch(Dispatchers.IO) {
            _estado.update { it.copy(cargando = true, mensaje = null) }
            try {
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
                withTimeoutOrNull(TIEMPO_MAXIMO_CARGA_MS) {
                    repositorioUsuario.actualizarUbicacion(miUid, lat, lng)
                }

                val amigos = withTimeoutOrNull(TIEMPO_MAXIMO_CARGA_MS) {
                    repositorioAmistad.observarUidsAmigos(miUid).first()
                } ?: emptySet()
                val usuariosResult = withTimeoutOrNull(TIEMPO_MAXIMO_CARGA_MS) {
                    repositorioUsuario.obtenerTodosUsuarios()
                }
                val all = usuariosResult?.getOrElse { emptyList() } ?: emptyList()
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
                        mensaje = if (usuariosResult == null) {
                            "La carga está tardando demasiado. Mostramos la vista sin amigos por ahora."
                        } else {
                            usuariosResult.exceptionOrNull()?.let { error ->
                                mensajeErrorSupabaseHumano(
                                    error,
                                    "No pudimos cargar los paseos cercanos."
                                )
                            }
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        mensaje = mensajeErrorSupabaseHumano(
                            e,
                            "No pudimos cargar los paseos cercanos.",
                        ),
                    )
                }
            }
        }
    }

    // cambia entre paseando y no paseando, y suma el paseo al terminar
    fun alternarPaseo(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val estabaPaseando = _estado.value.yoPaseando
            val nuevoPaseando = !estabaPaseando
            val r = repositorioUsuario.establecerPaseando(miUid, nuevoPaseando)
            if (r.isSuccess && estabaPaseando && !nuevoPaseando) {
                repositorioUsuario.incrementarNumeroPaseos(miUid)
            }
            _estado.update {
                it.copy(
                    yoPaseando = if (r.isSuccess) nuevoPaseando else estabaPaseando,
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

    companion object {
        private const val TIEMPO_MAXIMO_CARGA_MS = 10_000L
    }
}

package com.pawpals.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.jvm.Volatile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


// logica social de la app: explorar perfiles, chats, amigos, reportes y match final
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
        viewModelScope.launch(Dispatchers.IO) {
            _estado.update { it.copy(cargando = true, error = null) }

            // ubicacion actual para ordenar por cercania y actualizar nuestro perfil
            val loc = controladorUbicacion.ultimaUbicacionConocidaOFresca().getOrNull()
            val myLat = loc?.first
            val myLng = loc?.second
            if (loc != null) repositorioUsuario.actualizarUbicacion(miUid, loc.first, loc.second)

            val yaDeslizados =
                repositorioDeslizamiento.cargarUidsDeslizados(miUid).getOrElse { emptySet() }

            val usuarios = repositorioUsuario.obtenerTodosUsuarios().getOrElse { emptyList() }
                .filter { it.uid != miUid && !it.bloqueado && it.uid !in yaDeslizados }
            val perros = repositorioPerro.obtenerTodosPerros().getOrElse { emptyList() }
                .associateBy { it.uidDueno }

            val tarjetas = usuarios.map { u ->
                val d =
                    if (myLat != null && myLng != null && u.latitud != null && u.longitud != null) {
                        UtilidadesGeograficas.distanciaKm(
                            myLat,
                            myLng,
                            u.latitud,
                            u.longitud
                        ) * 1000.0
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

    // marca un super me gusta
    fun superMeGusta(miUid: String) = deslizar(miUid, AccionDeslizamiento.SUPER_ME_GUSTA)

    private fun deslizar(miUid: String, accion: AccionDeslizamiento) {
        val card = _estado.value.actual ?: return
        val uidObjetivo = card.usuario.uid
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioDeslizamiento.registrarDeslizamiento(miUid, uidObjetivo, accion)
            _estado.update { estadoActual ->
                val (ultimaCoincidencia, info) = when (val resultado = r.getOrNull()) {
                    is ResultadoDeslizamiento.CoincidenciaCreada -> uidObjetivo to null
                    ResultadoDeslizamiento.Guardado -> null to when (accion) {
                        AccionDeslizamiento.ME_GUSTA -> "Genial, le avisaremos si también te da me gusta"
                        AccionDeslizamiento.SUPER_ME_GUSTA -> "Súper me gusta enviado: recibirá una notificación destacada"
                        AccionDeslizamiento.DESCARTAR -> null
                    }

                    null -> null to null
                }
                estadoActual.copy(
                    indice = estadoActual.indice + 1,
                    ultimoUidCoincidencia = ultimaCoincidencia,
                    mensajeInfo = info,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo registrar tu respuesta.")
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
    val ultimoMensaje: String = "",
    val ultimoMensajeEn: Long = 0L,
    val ultimoRemitente: String = "",
    val tieneMensajeNuevo: Boolean = false,
)

data class EstadoUiListaConversaciones(
    val entradasAceptadas: List<EntradaListaConversaciones> = emptyList(),
)

@HiltViewModel
// monta la lista de chats con nombres y fotos en lugar de ids
class ModeloVistaListaConversaciones @Inject constructor(
    private val repositorioCoincidencia: RepositorioCoincidencia,
    private val repositorioConversacion: RepositorioConversacion,
    private val repositorioUsuario: RepositorioUsuario,
    private val repositorioPerro: RepositorioPerro,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoUiListaConversaciones())
    val estado: StateFlow<EstadoUiListaConversaciones> = _estado.asStateFlow()

    private val tareasDetalle = mutableMapOf<String, Job>()

    fun observar(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repositorioCoincidencia.observarCoincidenciasDeUsuario(miUid).collect { list ->
                val coincidenciasAceptadas =
                    list.filter { it.estado == EstadoCoincidencia.ACEPTADA }
                val base = coincidenciasAceptadas.map { m ->
                    val otroUid = if (m.usuarioA == miUid) m.usuarioB else m.usuarioA
                    val existing =
                        _estado.value.entradasAceptadas.firstOrNull { it.coincidencia.id == m.id }
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
                activeUids.forEach { uid -> watchDetails(miUid, uid) }
            }
        }
    }

    private fun watchDetails(miUid: String, otroUid: String) {
        if (tareasDetalle.containsKey(otroUid)) return
        val job = viewModelScope.launch(Dispatchers.IO) {
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
            launch {
                val idConversacion = RepositorioConversacion.idConversacionFor(miUid, otroUid)
                repositorioConversacion.observarMensajes(idConversacion).collect { mensajes ->
                    val ultimo = mensajes.maxByOrNull { it.marcaTemporal }
                    val nuevoTexto = ultimo?.texto.orEmpty()
                    val nuevaMarca = ultimo?.marcaTemporal ?: 0L
                    val nuevoRem = ultimo?.uidRemitente.orEmpty()
                    _estado.update { s ->
                        val entryPrev = s.entradasAceptadas.firstOrNull { it.otroUid == otroUid }
                        if (entryPrev != null &&
                            entryPrev.ultimoMensaje == nuevoTexto &&
                            entryPrev.ultimoMensajeEn == nuevaMarca &&
                            entryPrev.ultimoRemitente == nuevoRem
                        ) {
                            s
                        } else {
                            s.copy(
                                entradasAceptadas = s.entradasAceptadas.map { entry ->
                                    if (entry.otroUid == otroUid) {
                                        entry.copy(
                                            ultimoMensaje = nuevoTexto,
                                            ultimoMensajeEn = nuevaMarca,
                                            ultimoRemitente = nuevoRem,
                                            tieneMensajeNuevo = ultimo != null && ultimo.uidRemitente != miUid,
                                        )
                                    } else entry
                                },
                            )
                        }
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

@HiltViewModel
// escucha cambios importantes y lanza avisos del movil sin meter otra pantalla
class ModeloVistaNotificaciones @Inject constructor(
    private val repositorioCoincidencia: RepositorioCoincidencia,
    private val repositorioConversacion: RepositorioConversacion,
    private val repositorioAmistad: RepositorioAmistad,
    private val repositorioUsuario: RepositorioUsuario,
    private val preferenciasUsuario: PreferenciasUsuario,
    private val notificador: NotificadorLocalPaw,
) : ViewModel() {

    private var iniciado = false
    private val tareasPaseos = mutableMapOf<String, Job>()
    @Volatile
    private var notifMensajes = true
    @Volatile
    private var notifCoincidencias = true
    @Volatile
    private var notifPaseos = true

    fun iniciar(miUid: String) {
        if (iniciado) return
        iniciado = true
        viewModelScope.launch {
            combine(
                preferenciasUsuario.notificarMensajes,
                preferenciasUsuario.notificarCoincidencias,
                preferenciasUsuario.notificarPaseos,
            ) { m, c, p -> Triple(m, c, p) }
                .collect { (m, c, p) ->
                    notifMensajes = m
                    notifCoincidencias = c
                    notifPaseos = p
                }
        }
        observarMatchesSolo(miUid)
        observarResumenMensajesParaNotificar(miUid)
        observarSolicitudes(miUid)
        observarAmigosYPaseos(miUid)
    }

    private fun observarMatchesSolo(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var primeraCarga = true
            var idsConocidos = emptySet<String>()
            repositorioCoincidencia.observarCoincidenciasDeUsuario(miUid).collect { coincidencias ->
                val aceptadas = coincidencias.filter { it.estado == EstadoCoincidencia.ACEPTADA }
                val idsActuales = aceptadas.map { it.id }.toSet()
                if (!primeraCarga && notifCoincidencias) {
                    (idsActuales - idsConocidos).forEach { id ->
                        notificador.publicar(
                            id.hashCode(),
                            "Nuevo match",
                            "Tienes un nuevo match en PawPals.",
                        )
                    }
                }
                primeraCarga = false
                idsConocidos = idsActuales
            }
        }
    }

    private fun observarResumenMensajesParaNotificar(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var primera = true
            var porId = mapOf<String, Long>()
            repositorioConversacion.observarResumenesConversacionDondeParticipa(miUid)
                .collect { lista ->
                    if (!primera && notifMensajes) {
                        lista.forEach { res ->
                            val antes = porId[res.idConversacion] ?: 0L
                            val rem = res.ultimoRemitenteUid
                            if (res.ultimoMensajeEn > antes && rem != null && rem != miUid) {
                                val texto = res.vistaPrevia?.trim().orEmpty()
                                    .let { if (it.length > 80) it.take(80) + "…" else it }
                                    .ifBlank { "Tienes un mensaje nuevo" }
                                notificador.publicar(
                                    res.idConversacion.hashCode(),
                                    "Nuevo mensaje",
                                    texto,
                                )
                            }
                        }
                    }
                    primera = false
                    porId = lista.associate { it.idConversacion to it.ultimoMensajeEn }
                }
        }
    }

    private fun observarSolicitudes(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var primeraCarga = true
            var idsConocidos = emptySet<String>()
            repositorioAmistad.observarSolicitudesEntrantes(miUid).collect { solicitudes ->
                val idsActuales = solicitudes.map { it.id }.toSet()
                if (!primeraCarga && notifMensajes) {
                    (idsActuales - idsConocidos).forEach { id ->
                        notificador.publicar(
                            id.hashCode(),
                            "Solicitud de amistad",
                            "Alguien quiere añadirse como amigo en PawPals.",
                        )
                    }
                }
                primeraCarga = false
                idsConocidos = idsActuales
            }
        }
    }

    private fun observarAmigosYPaseos(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var primeraCarga = true
            var amigosConocidos = emptySet<String>()
            repositorioAmistad.observarUidsAmigos(miUid).collect { amigos ->
                if (!primeraCarga && notifMensajes) {
                    (amigos - amigosConocidos).forEach { uid ->
                        notificador.publicar(
                            uid.hashCode(),
                            "Solicitud aceptada",
                            "Ya sois amigos en PawPals.",
                        )
                    }
                }
                primeraCarga = false
                amigosConocidos = amigos
                amigos.forEach { uid -> observarPaseoAmigo(uid) }
                tareasPaseos.keys.filter { it !in amigos }.forEach { uid ->
                    tareasPaseos.remove(uid)?.cancel()
                }
            }
        }
    }

    private fun observarPaseoAmigo(uidAmigo: String) {
        if (tareasPaseos.containsKey(uidAmigo)) return
        tareasPaseos[uidAmigo] = viewModelScope.launch(Dispatchers.IO) {
            var primeraCarga = true
            var paseabaAntes = false
            repositorioUsuario.observarUsuario(uidAmigo).collect { usuario ->
                val paseandoAhora = usuario?.paseando == true
                if (
                    !primeraCarga &&
                    !paseabaAntes &&
                    paseandoAhora &&
                    notifPaseos
                ) {
                    notificador.publicar(
                        uidAmigo.hashCode(),
                        "Amigo en modo paseo",
                        "${usuario?.nombreVisible ?: "Un amigo"} ha empezado un paseo.",
                    )
                }
                primeraCarga = false
                paseabaAntes = paseandoAhora
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tareasPaseos.values.forEach { it.cancel() }
        tareasPaseos.clear()
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
    private val repositorioModeracion: RepositorioModeracion,
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
        viewModelScope.launch(Dispatchers.IO) {
            repositorioConversacion.observarMensajes(idConversacion).collect { list ->
                _estado.update { it.copy(mensajes = list) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repositorioAmistad.observarSolicitudEntre(miUid, otroUid).collect { req ->
                _estado.update { it.copy(solicitudPendiente = req) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repositorioAmistad.observarUidsAmigos(miUid).collect { amigos ->
                _estado.update { it.copy(yaSonAmigos = otroUid in amigos) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            _estado.update { it.copy(enviando = true, error = null) }
            val r = repositorioConversacion.enviarMensaje(idConversacion, miUid, texto.trim())
            _estado.update { estadoActual ->
                estadoActual.copy(
                    enviando = false,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo enviar el mensaje.")
                    },
                )
            }
        }
    }

    // manda una solicitud de amistad al otro usuario
    fun solicitarAmistad(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioAmistad.enviarSolicitudAmistad(miUid, otroUid)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    info = if (r.isSuccess) "Solicitud de amistad enviada" else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(
                            error,
                            "No se pudo enviar la solicitud de amistad."
                        )
                    },
                )
            }
        }
    }

    fun aceptarSolicitudAmistad() {
        val req = _estado.value.solicitudPendiente ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioAmistad.aceptarSolicitudAmistad(req.id)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    info = if (r.isSuccess) "¡Ahora sois amigos! Verás su ubicación cuando pasee." else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo aceptar la solicitud.")
                    },
                )
            }
        }
    }

    fun rechazarSolicitudAmistad() {
        val req = _estado.value.solicitudPendiente ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioAmistad.rechazarSolicitudAmistad(req.id)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo rechazar la solicitud.")
                    },
                )
            }
        }
    }

    fun eliminarAmigo(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioAmistad.eliminarAmigo(miUid, otroUid)
            _estado.update { estadoActual ->
                estadoActual.copy(
                    info = if (r.isSuccess) "Amigo eliminado" else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo eliminar de amigos.")
                    },
                )
            }
        }
    }

    fun reportarUsuario(miUid: String, motivo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = repositorioModeracion.enviarReporte(
                uidReportante = miUid,
                tipoObjetivo = TipoObjetivoReporte.USUARIO,
                idObjetivo = otroUid,
                motivo = motivo,
            )
            _estado.update { estadoActual ->
                estadoActual.copy(
                    info = if (r.isSuccess) "Reporte enviado" else null,
                    error = r.exceptionOrNull()?.let { error ->
                        mensajeErrorSupabaseHumano(error, "No se pudo enviar el reporte.")
                    },
                )
            }
        }
    }

    // deshace la coincidencia y permite que ambos puedan volver a encontrarse
    fun deshacerCoincidencia(miUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val coincidencia =
                repositorioCoincidencia.buscarCoincidencia(miUid, otroUid).getOrNull()
            if (coincidencia != null) {
                repositorioCoincidencia.eliminarCoincidencia(coincidencia.id)
            }
            repositorioDeslizamiento.limpiarEntre(miUid, otroUid)
            _estado.update { it.copy(coincidenciaDeshecha = true, info = "Match deshecho") }
        }
    }

    fun limpiarInfo() = _estado.update { it.copy(info = null, error = null) }
}

// tarjeta que junta dueño, perro y distancia para explorar

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
        viewModelScope.launch(Dispatchers.IO) {
            repositorioUsuario.observarUsuario(otroUid).collect { u ->
                _estado.update {
                    it.copy(
                        nombreOtroUsuario = u?.nombreVisible.orEmpty(),
                        urlFotoUsuario = u?.urlFoto,
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
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

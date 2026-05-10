package com.pawpals.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
// escucha el perfil propio y deja editar humano, perro y fotos
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

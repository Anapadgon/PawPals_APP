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
import io.github.jan.supabase.storage.storage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// conecta la app con supabase auth para login, registro y sesion
class RepositorioAutenticacionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioAutenticacion {

    private val auth: Auth get() = cliente.auth

    override val estadoAutenticacion: Flow<CuentaAuth?> =
        auth.sessionStatus.transform { st ->
            // mientras supabase recupera la sesion guardada no emitimos "sin cuenta"
            // asi se evita mostrar bienvenida o login durante un instante
            when (st) {
                SessionStatus.Initializing -> Unit
                is SessionStatus.Authenticated -> emit(
                    st.session.user?.let { CuentaAuth(uid = it.id, correo = it.email) },
                )

                is SessionStatus.NotAuthenticated -> emit(null)
                is SessionStatus.RefreshFailure -> emit(
                    auth.currentUserOrNull()?.let { CuentaAuth(uid = it.id, correo = it.email) },
                )
            }
        }.distinctUntilChanged()

    override fun cuentaActual(): CuentaAuth? =
        auth.currentUserOrNull()?.let { CuentaAuth(uid = it.id, correo = it.email) }

    override suspend fun iniciarSesionCorreo(correo: String, contrasena: String): Result<Unit> =
        runCatching {
            auth.signInWith(Email) {
                email = correo
                password = contrasena
            }
        }

    override suspend fun registrarCorreo(
        correo: String,
        contrasena: String
    ): Result<ResultadoRegistroCorreo> =
        runCatching {
            val correoTrim = correo.trim()
            auth.signUpWith(Email) {
                email = correoTrim
                password = contrasena
            }
            // si supabase no pide confirmar correo, el registro deja la sesion abierta
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

    override suspend fun enviarCorreoRestablecerContrasena(correo: String): Result<Unit> =
        runCatching {
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
// lee y actualiza la ficha humana del usuario
class RepositorioUsuarioSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioUsuario {

    override fun observarUsuario(uid: String): Flow<PerfilUsuario?> =
        observarConPolling(valorEnError = null) { cargarUsuario(uid) }

    private suspend fun cargarUsuario(uid: String): PerfilUsuario? {
        val rows = cliente.postgrest.from(Tablas.USUARIOS).select {
            filter { eq("id", uid) }
            limit(1)
        }.decodeList<FilaUsuario>()
        return rows.firstOrNull()?.aPerfilUsuario()
    }

    override suspend fun asegurarDocumentoUsuario(uid: String, correo: String): Result<Unit> =
        runCatching {
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
                // la rpc crea usuario normal; si es el admin de demo se corrige despues
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

    override suspend fun actualizarUbicacion(uid: String, lat: Double, lng: Double): Result<Unit> =
        runCatching {
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

    override suspend fun establecerPaseando(uid: String, paseando: Boolean): Result<Unit> =
        runCatching {
            cliente.postgrest.from(Tablas.USUARIOS).update({
                set("paseando", paseando)
            }) {
                filter { eq("id", uid) }
            }
            Unit
        }

    override suspend fun actualizarTokenFcm(uid: String, token: String): Result<Unit> =
        runCatching {
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

    override suspend fun establecerBloqueado(uid: String, bloqueado: Boolean): Result<Unit> =
        runCatching {
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
        cliente.cambiarContadorUsuario(uid, "numero_paseos", 1)
    }
}

@Singleton
// guarda el perfil del perro y lo mantiene ligado a su dueño
class RepositorioPerroSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioPerro {

    override fun observarPerroDeDueno(uidDueno: String): Flow<PerfilPerro?> =
        observarConPolling(valorEnError = null) { cargarPerroDueno(uidDueno) }

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
// sube imagenes al bucket publico y devuelve su url
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


package com.pawpals.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// guarda y consulta los matches entre dos usuarios
class RepositorioCoincidenciaSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioCoincidencia {

    override fun observarCoincidenciasDeUsuario(uid: String): Flow<List<Coincidencia>> =
        observarConPolling(valorEnError = emptyList()) {
            cliente.postgrest.from(Tablas.COINCIDENCIAS).select {
                filter { cs("participantes", listOf(uid)) }
            }.decodeList<FilaCoincidencia>()
                .map { it.aCoincidencia() }
                .sortedByDescending { it.creadoEn }
        }

    override suspend fun enviarSolicitudCoincidencia(
        uidOrigen: String,
        uidDestino: String
    ): Result<Unit> = runCatching {
        if (uidOrigen == uidDestino) error("No puedes crear un match contigo mismo")
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

    override suspend fun responderCoincidencia(
        idCoincidencia: String,
        aceptar: Boolean
    ): Result<Unit> = runCatching {
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

    private suspend fun decrementarCoincidencias(uid: String) =
        cliente.cambiarContadorUsuario(uid, "numero_coincidencias", -1)

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
// mensajes sencillos entre usuarios que ya tienen conversacion
class RepositorioConversacionSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioConversacion {

    override fun observarMensajes(idConversacion: String): Flow<List<MensajeConversacion>> =
        observarConPolling(valorEnError = emptyList()) {
            cliente.postgrest.from(Tablas.MENSAJES).select {
                filter { eq("conversacion_id", idConversacion) }
                order("marca_temporal", Order.ASCENDING)
                limit(200)
            }.decodeList<FilaMensaje>().map { it.aMensaje() }
        }

    override fun observarResumenesConversacionDondeParticipa(miUid: String): Flow<List<ResumenActividadConversacion>> =
        observarConPolling(periodoMs = 4500L, valorEnError = emptyList()) {
            cliente.postgrest.from(Tablas.CONVERSACIONES).select {
                filter { cs("participantes", listOf(miUid)) }
                limit(200)
            }.decodeList<FilaConversacion>().map {
                ResumenActividadConversacion(
                    idConversacion = it.id,
                    ultimoMensajeEn = it.ultimoMensajeEn ?: 0L,
                    ultimoRemitenteUid = it.ultimoRemitente,
                    vistaPrevia = it.ultimoMensaje,
                )
            }
        }

    override suspend fun enviarMensaje(
        idConversacion: String,
        uidRemitente: String,
        texto: String
    ): Result<Unit> =
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
// registra los swipes y crea coincidencia cuando el gusto es mutuo
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

    private suspend fun incrementarCoincidencias(uid: String) =
        cliente.cambiarContadorUsuario(uid, "numero_coincidencias", 1)

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
        internal fun idDeslizamiento(from: String, to: String) =
            idCompuesto(from, to, ordenado = false)

        internal fun idCoincidencia(a: String, b: String) = idCompuesto(a, b, ordenado = true)
    }
}

@Singleton
// convierte solicitudes aceptadas en amistad real en ambos sentidos
class RepositorioAmistadSupabase @Inject constructor(
    private val cliente: SupabaseClient,
) : RepositorioAmistad {

    override fun observarSolicitudesEntrantes(miUid: String): Flow<List<SolicitudAmistad>> =
        observarConPolling(periodoMs = 2500L, valorEnError = emptyList()) {
            cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).select {
                filter {
                    eq("uid_destino", miUid)
                    eq("estado", EstadoSolicitudAmistad.PENDIENTE.name.lowercase())
                }
            }.decodeList<FilaSolicitud>().map { it.aSolicitud() }
        }

    override fun observarSolicitudEntre(miUid: String, otroUid: String): Flow<SolicitudAmistad?> {
        val idAB = idSolicitud(miUid, otroUid)
        val idBA = idSolicitud(otroUid, miUid)
        return observarConPolling(periodoMs = 2500L, valorEnError = null) {
            cliente.postgrest.from(Tablas.SOLICITUDES_AMISTAD).select {
                filter {
                    or {
                        eq("id", idAB)
                        eq("id", idBA)
                    }
                }
            }.decodeList<FilaSolicitud>()
                .map { it.aSolicitud() }
                .maxByOrNull { it.creadoEn }
        }
    }

    override fun observarUidsAmigos(miUid: String): Flow<Set<String>> =
        observarConPolling(periodoMs = 2500L, valorEnError = emptySet()) {
            cliente.postgrest.from(Tablas.AMIGOS).select {
                filter { eq("usuario_id", miUid) }
                limit(500)
            }.decodeList<AmigoFila>().map { it.amigoId }.toSet()
        }

    override suspend fun enviarSolicitudAmistad(
        uidOrigen: String,
        uidDestino: String
    ): Result<Unit> = runCatching {
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

    private suspend fun incrementarAmigos(uid: String) =
        cliente.cambiarContadorUsuario(uid, "numero_amigos", 1)

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

    private suspend fun decrementarAmigos(uid: String) =
        cliente.cambiarContadorUsuario(uid, "numero_amigos", -1)

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
        internal fun idSolicitud(from: String, to: String) = idCompuesto(from, to, ordenado = false)
    }

    @Serializable
    private data class AmigoFila(
        @SerialName("usuario_id")
        val usuarioId: String,
        @SerialName("amigo_id")
        val amigoId: String,
        @SerialName("creado_en")
        val creadoEn: Long = 0,
    )
}

@Singleton
// guarda reportes para que el panel admin los pueda revisar
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

    override fun observarReportesAbiertos(): Flow<List<ReporteContenido>> =
        observarConPolling(periodoMs = 3000L, valorEnError = emptyList()) {
            cliente.postgrest.from(Tablas.REPORTES).select {
                order("creado_en", Order.DESCENDING)
                limit(100)
            }.decodeList<FilaReporte>()
                .map { it.aReporte() }
                .filter { it.estado == EstadoReporte.ABIERTO }
        }

    override suspend fun actualizarEstadoReporte(
        idReporte: String,
        estado: EstadoReporte
    ): Result<Unit> =
        runCatching {
            cliente.postgrest.from(Tablas.REPORTES).update({
                set("estado", estado.name.lowercase())
            }) {
                filter { eq("id", idReporte) }
            }
            Unit
        }
}

package com.pawpals.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

// envia mensajes de soporte cuando un usuario necesita ayuda
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
// crea perfiles falsos para probar explorar, mapa y estadisticas
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
            bioPerro = "Súper obediente, ideal para paseos tranquilos y rutas.",
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
// calcula numeros simples para el resumen del panel admin
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

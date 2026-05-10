package com.pawpals.app

// helpers para no repetir constructores enormes en cada test.
// asi un test se centra en lo que prueba y no en armar datos de relleno.
internal object TestFactores {

    fun usuario(
        uid: String = "u1",
        nombreVisible: String = "Usuario",
        zona: String = "Madrid",
        rol: RolUsuario = RolUsuario.USUARIO,
        bloqueado: Boolean = false,
        latitud: Double? = null,
        longitud: Double? = null,
        paseando: Boolean = false,
        numeroAmigos: Int = 0,
        numeroPaseos: Int = 0,
        numeroCoincidencias: Int = 0,
    ): PerfilUsuario = PerfilUsuario(
        uid = uid,
        correo = "$uid@example.com",
        nombreVisible = nombreVisible,
        zona = zona,
        sobreMi = "",
        rol = rol,
        bloqueado = bloqueado,
        latitud = latitud,
        longitud = longitud,
        tokenFcm = null,
        urlFoto = null,
        numeroAmigos = numeroAmigos,
        numeroPaseos = numeroPaseos,
        numeroCoincidencias = numeroCoincidencias,
        paseando = paseando,
    )

    fun perro(
        id: String = "p1",
        uidDueno: String = "u1",
        nombre: String = "Luna",
        raza: String = "Labrador",
        edad: Int = 3,
        biografia: String = "",
        energia: NivelEnergia = NivelEnergia.MODERADO,
        sociabilidad: Sociabilidad = Sociabilidad.MUY_SOCIABLE,
    ): PerfilPerro = PerfilPerro(
        id = id,
        uidDueno = uidDueno,
        nombre = nombre,
        raza = raza,
        edadAnios = edad,
        biografia = biografia,
        urlFoto = null,
        energia = energia,
        sociabilidad = sociabilidad,
    )

    fun coincidencia(
        id: String = "c1",
        usuarioA: String = "u1",
        usuarioB: String = "u2",
        iniciador: String = "u1",
        estado: EstadoCoincidencia = EstadoCoincidencia.ACEPTADA,
        creadoEn: Long = 1000L,
    ): Coincidencia = Coincidencia(
        id = id,
        usuarioA = usuarioA,
        usuarioB = usuarioB,
        uidIniciador = iniciador,
        estado = estado,
        creadoEn = creadoEn,
    )

    fun solicitudAmistad(
        id: String = "s1",
        origen: String = "u1",
        destino: String = "u2",
        estado: EstadoSolicitudAmistad = EstadoSolicitudAmistad.PENDIENTE,
    ): SolicitudAmistad = SolicitudAmistad(
        id = id,
        uidOrigen = origen,
        uidDestino = destino,
        estado = estado,
        creadoEn = 100L,
    )

    fun reporte(
        id: String = "r1",
        tipo: TipoObjetivoReporte = TipoObjetivoReporte.USUARIO,
        idObjetivo: String = "u2",
        reportante: String = "u1",
        motivo: String = "Perfil falso",
        estado: EstadoReporte = EstadoReporte.ABIERTO,
    ): ReporteContenido = ReporteContenido(
        id = id,
        tipoObjetivo = tipo,
        idObjetivo = idObjetivo,
        uidReportante = reportante,
        motivo = motivo,
        estado = estado,
        creadoEn = 500L,
    )
}

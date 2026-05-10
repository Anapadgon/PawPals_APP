package com.pawpals.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelosTest {

    @Test
    fun nivelEnergia_fromRaw_convierteValoresGuardadosEnBaseDeDatos() {
        assertEquals(NivelEnergia.TRANQUILO, NivelEnergia.fromRaw("tranquilo"))
        assertEquals(NivelEnergia.ALTA, NivelEnergia.fromRaw("alta"))
        assertEquals(NivelEnergia.MODERADO, NivelEnergia.fromRaw("moderado"))
        assertEquals(NivelEnergia.MODERADO, NivelEnergia.fromRaw(null))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "nivel energia",
            detalle = "los valores de base de datos se convierten al enum de la app",
        )
    }

    @Test
    fun sociabilidad_fromRaw_convierteValoresGuardadosEnBaseDeDatos() {
        assertEquals(Sociabilidad.SELECTIVO, Sociabilidad.fromRaw("selectivo"))
        assertEquals(Sociabilidad.TIMIDO, Sociabilidad.fromRaw("timido"))
        assertEquals(Sociabilidad.MUY_SOCIABLE, Sociabilidad.fromRaw("muy_sociable"))
        assertEquals(Sociabilidad.MUY_SOCIABLE, Sociabilidad.fromRaw(null))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "sociabilidad",
            detalle = "los valores de base de datos se convierten al enum de la app",
        )
    }

    @Test
    fun perfilUsuario_ciudadDevuelveLaZona() {
        val usuario = PerfilUsuario(
            uid = "u1",
            correo = "ana@example.com",
            nombreVisible = "Ana",
            zona = "Cordoba",
            sobreMi = "Me gustan los perros",
            rol = RolUsuario.USUARIO,
            bloqueado = false,
            latitud = null,
            longitud = null,
            tokenFcm = null,
            urlFoto = null,
        )

        assertEquals("Cordoba", usuario.ciudad)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "alias ciudad",
            detalle = "ciudad mantiene compatibilidad con el campo zona",
        )
    }

    @Test
    fun repositorioConversacion_idConversacionFor_usaSiempreElMismoIdOrdenado() {
        val id1 = RepositorioConversacion.idConversacionFor("usuario_b", "usuario_a")
        val id2 = RepositorioConversacion.idConversacionFor("usuario_a", "usuario_b")

        assertEquals("usuario_a__usuario_b", id1)
        assertEquals(id1, id2)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "id conversacion",
            detalle = "dos usuarios generan siempre el mismo id de chat",
        )
    }

    @Test
    fun nivelEnergia_label_devuelveTextoLeibleEnLaUi() {
        // las etiquetas se ven en chips de explorar y perfil, no pueden estar vacias
        org.junit.Assert.assertTrue(NivelEnergia.TRANQUILO.label.isNotBlank())
        org.junit.Assert.assertTrue(NivelEnergia.MODERADO.label.isNotBlank())
        org.junit.Assert.assertTrue(NivelEnergia.ALTA.label.isNotBlank())
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "etiquetas energia",
            detalle = "cada nivel tiene una etiqueta lista para mostrar al usuario",
        )
    }

    @Test
    fun sociabilidad_label_devuelveTextoLeibleEnLaUi() {
        org.junit.Assert.assertTrue(Sociabilidad.MUY_SOCIABLE.label.isNotBlank())
        org.junit.Assert.assertTrue(Sociabilidad.SELECTIVO.label.isNotBlank())
        org.junit.Assert.assertTrue(Sociabilidad.TIMIDO.label.isNotBlank())
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "etiquetas sociabilidad",
            detalle = "cada tipo de sociabilidad tiene su etiqueta visible",
        )
    }

    @Test
    fun estadoCoincidencia_tieneLosTresEstadosEsperados() {
        // si en el futuro alguien añade o quita un estado, este test lo detecta
        val esperados = setOf(
            EstadoCoincidencia.PENDIENTE,
            EstadoCoincidencia.ACEPTADA,
            EstadoCoincidencia.RECHAZADA,
        )
        assertEquals(esperados, EstadoCoincidencia.values().toSet())
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "estados coincidencia",
            detalle = "pendiente, aceptada y rechazada son los unicos estados validos",
        )
    }

    @Test
    fun accionDeslizamiento_tieneTresAccionesEsperadas() {
        val esperadas = setOf(
            AccionDeslizamiento.ME_GUSTA,
            AccionDeslizamiento.SUPER_ME_GUSTA,
            AccionDeslizamiento.DESCARTAR,
        )
        assertEquals(esperadas, AccionDeslizamiento.values().toSet())
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "acciones deslizamiento",
            detalle = "los tres botones del explorar tienen accion definida",
        )
    }

    @Test
    fun resultadoDeslizamiento_coincidenciaCreadaIncluyeIdDeLaMisma() {
        val resultado: ResultadoDeslizamiento = ResultadoDeslizamiento.CoincidenciaCreada("c123")
        org.junit.Assert.assertTrue(resultado is ResultadoDeslizamiento.CoincidenciaCreada)
        assertEquals(
            "c123",
            (resultado as ResultadoDeslizamiento.CoincidenciaCreada).idCoincidencia
        )
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "resultado deslizamiento",
            detalle = "cuando hay match, el resultado lleva el id para navegar al chat",
        )
    }

    @Test
    fun perfilUsuario_aliasCiudad_funcionaConZonaVacia() {
        val sinZona = TestFactores.usuario(uid = "vacio", zona = "")
        assertEquals("", sinZona.ciudad)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "ciudad sin zona",
            detalle = "el alias ciudad no inventa datos si el usuario no ha rellenado la zona",
        )
    }

    @Test
    fun perfilPerro_seConstruyeConValoresPorDefectoCoherentes() {
        // un perro nuevo debe quedar utilizable aunque no se rellenen los rasgos
        val perro = TestFactores.perro(nombre = "Toby", raza = "Mestizo")
        assertEquals(NivelEnergia.MODERADO, perro.energia)
        assertEquals(Sociabilidad.MUY_SOCIABLE, perro.sociabilidad)
        org.junit.Assert.assertTrue(perro.edadAnios >= 1)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "perro por defecto",
            detalle = "un perro recien creado tiene rasgos por defecto razonables",
        )
    }
}

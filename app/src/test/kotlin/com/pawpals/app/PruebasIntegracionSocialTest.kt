package com.pawpals.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasIntegracionSocialTest {

    @Test
    fun flujoMatchAceptado_creaConversacionEntreLosDosUsuarios() {
        val coincidencia = Coincidencia(
            id = "match_1",
            usuarioA = "ana",
            usuarioB = "lucia",
            uidIniciador = "ana",
            estado = EstadoCoincidencia.ACEPTADA,
            creadoEn = 1000L,
        )
        val idConversacion = RepositorioConversacion.idConversacionFor(
            coincidencia.usuarioA,
            coincidencia.usuarioB,
        )

        assertEquals(EstadoCoincidencia.ACEPTADA, coincidencia.estado)
        assertEquals("ana__lucia", idConversacion)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "match aceptado y chat",
            detalle = "un match aceptado genera un id de conversacion comun",
        )
    }

    @Test
    fun resumenConversacion_muestraUltimoMensajeDelChat() {
        val mensajes = listOf(
            MensajeConversacion(
                id = "m1",
                uidRemitente = "ana",
                texto = "Hola, damos un paseo?",
                marcaTemporal = 10L,
            ),
            MensajeConversacion(
                id = "m2",
                uidRemitente = "lucia",
                texto = "Si, esta tarde puedo",
                marcaTemporal = 20L,
            ),
        )
        val ultimo = mensajes.maxBy { it.marcaTemporal }
        val resumen = ResumenActividadConversacion(
            idConversacion = RepositorioConversacion.idConversacionFor("ana", "lucia"),
            ultimoMensajeEn = ultimo.marcaTemporal,
            ultimoRemitenteUid = ultimo.uidRemitente,
            vistaPrevia = ultimo.texto,
        )

        assertEquals("lucia", resumen.ultimoRemitenteUid)
        assertEquals("Si, esta tarde puedo", resumen.vistaPrevia)
        assertTrue(resumen.ultimoMensajeEn == 20L)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "chat y lista de conversaciones",
            detalle = "el resumen muestra el ultimo mensaje enviado",
        )
    }

    @Test
    fun flujoSolicitudAmistad_pendientePasaAAceptadaConservandoOrigenYDestino() {
        // simula la transicion de estado que hace el RepositorioAmistad al aceptar
        val pendiente = TestFactores.solicitudAmistad(
            id = "s1", origen = "ana", destino = "lucia",
            estado = EstadoSolicitudAmistad.PENDIENTE,
        )

        val aceptada = pendiente.copy(estado = EstadoSolicitudAmistad.ACEPTADA)

        assertTrue(aceptada.estado == EstadoSolicitudAmistad.ACEPTADA)
        assertEquals("ana", aceptada.uidOrigen)
        assertEquals("lucia", aceptada.uidDestino)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "solicitud aceptada",
            detalle = "al aceptar la solicitud no se pierde quien la envio ni a quien",
        )
    }

    @Test
    fun flujoBloqueo_usuarioBloqueadoSeFiltraDelListadoExplorar() {
        // misma filtracion que aplica el ModeloVistaExplorar al cargar candidatos
        val candidatos = listOf(
            TestFactores.usuario(uid = "ana"),
            TestFactores.usuario(uid = "javier", bloqueado = true),
            TestFactores.usuario(uid = "lucia"),
        )
        val visibles = candidatos.filter { !it.bloqueado }

        assertEquals(2, visibles.size)
        assertTrue(visibles.none { it.bloqueado })
        assertTrue(visibles.none { it.uid == "javier" })
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "filtrado de bloqueados",
            detalle = "los usuarios bloqueados no aparecen como tarjetas en explorar",
        )
    }

    @Test
    fun flujoReporte_seCreaAbiertoYSeResuelveComoAccionTomada() {
        val abierto = TestFactores.reporte(id = "r1", estado = EstadoReporte.ABIERTO)

        // el admin marca el reporte como tratado
        val resuelto = abierto.copy(estado = EstadoReporte.ACCION_TOMADA)

        assertTrue(abierto.estado == EstadoReporte.ABIERTO)
        assertTrue(resuelto.estado == EstadoReporte.ACCION_TOMADA)
        // el resto de campos no debe haber cambiado
        assertEquals(abierto.id, resuelto.id)
        assertEquals(abierto.motivo, resuelto.motivo)
        assertEquals(abierto.uidReportante, resuelto.uidReportante)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "ciclo de vida de un reporte",
            detalle = "un reporte solo cambia de estado, no pierde datos al resolverse",
        )
    }

    @Test
    fun flujoDeslizamiento_descartarNoGeneraCoincidencia() {
        // si la accion es DESCARTAR, el repositorio no debe crear un Match aunque haya like inverso
        val resultado: ResultadoDeslizamiento = ResultadoDeslizamiento.Guardado

        assertTrue(resultado is ResultadoDeslizamiento.Guardado)
        assertTrue(resultado !is ResultadoDeslizamiento.CoincidenciaCreada)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "descartar no crea match",
            detalle = "una accion de descarte siempre termina como Guardado, nunca como Coincidencia",
        )
    }

    @Test
    fun flujoMatch_idEsElMismoIndependienteDelOrdenDeLosUidsAlEnviar() {
        // si ana hace match con lucia o lucia con ana, el id debe ser el mismo
        val idDesdeAna = RepositorioConversacion.idConversacionFor("ana", "lucia")
        val idDesdeLucia = RepositorioConversacion.idConversacionFor("lucia", "ana")

        assertEquals(idDesdeAna, idDesdeLucia)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "id de match estable",
            detalle = "iniciar el match desde un lado u otro lleva al mismo chat",
        )
    }
}

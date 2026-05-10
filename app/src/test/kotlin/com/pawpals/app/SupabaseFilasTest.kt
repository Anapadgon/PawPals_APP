package com.pawpals.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseFilasTest {

    @Test
    fun idsAuthCoinciden_aceptaMismoIdConMayusculasDiferentes() {
        assertTrue(idsAuthCoinciden("ABC-123", "abc-123"))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "ids mayusculas",
            detalle = "los ids coinciden aunque cambien mayusculas y minusculas",
        )
    }

    @Test
    fun idsAuthCoinciden_aceptaMismoIdConGuionesDiferentes() {
        assertTrue(idsAuthCoinciden("abc-123-def", "abc123def"))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "ids con guiones",
            detalle = "los ids coinciden aunque falten guiones",
        )
    }

    @Test
    fun idsAuthCoinciden_rechazaIdsDistintos() {
        assertFalse(idsAuthCoinciden("abc-123", "xyz-999"))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "ids distintos",
            detalle = "dos ids diferentes no se aceptan como iguales",
        )
    }

    @Test
    fun filaUsuario_aPerfilUsuario_convierteRolAdministrador() {
        val perfil = FilaUsuario(
            id = "admin1",
            correo = "admin@pawpals.app",
            nombreVisible = "Admin",
            rol = "administrador",
        ).aPerfilUsuario()

        assertTrue(perfil.rol == RolUsuario.ADMINISTRADOR)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila usuario a modelo",
            detalle = "la fila de supabase se transforma en perfil administrador",
        )
    }

    @Test
    fun filaPerro_aPerfilPerro_corrigeEdadMinima() {
        val perro = FilaPerro(
            id = "p1",
            uidDueno = "u1",
            nombre = "Luna",
            raza = "Mestiza",
            edadAnios = 0,
        ).aPerfilPerro()

        assertTrue(perro.edadAnios >= 1)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila perro a modelo",
            detalle = "una edad no valida se corrige antes de llegar a la pantalla",
        )
    }

    @Test
    fun filaCoincidencia_aCoincidencia_convierteEstadoAceptado() {
        val coincidencia = FilaCoincidencia(
            id = "c1",
            usuarioA = "u1",
            usuarioB = "u2",
            usuarioMenor = "u1",
            usuarioMayor = "u2",
            participantes = listOf("u1", "u2"),
            iniciador = "u1",
            estado = "aceptada",
            creadoEn = 100L,
        ).aCoincidencia()

        assertTrue(coincidencia.estado == EstadoCoincidencia.ACEPTADA)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila coincidencia a modelo",
            detalle = "el estado aceptada se convierte al modelo usado por la app",
        )
    }

    @Test
    fun idCompuesto_ordenado_devuelveMismoIdAunqueSeIntercambien() {
        val ab = idCompuesto("usuario_b", "usuario_a", ordenado = true)
        val ba = idCompuesto("usuario_a", "usuario_b", ordenado = true)

        assertTrue(ab == ba)
        assertTrue(ab == "usuario_a__usuario_b")
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "idCompuesto ordenado",
            detalle = "matches y conversaciones reciben el mismo id sin importar quien lo crea",
        )
    }

    @Test
    fun idCompuesto_noOrdenado_mantieneOrigenYDestino() {
        val origenADestino = idCompuesto("ana", "lucia", ordenado = false)
        val destinoAOrigen = idCompuesto("lucia", "ana", ordenado = false)

        assertFalse(origenADestino == destinoAOrigen)
        assertTrue(origenADestino == "ana__lucia")
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "idCompuesto no ordenado",
            detalle = "deslizamientos y solicitudes guardan quien fue origen y quien destino",
        )
    }

    @Test
    fun filaUsuario_aPerfilUsuario_rolDesconocidoCaeAUsuario() {
        // un rol raro que llegue del servidor no debe romper la app
        val perfil = FilaUsuario(
            id = "u1",
            correo = "raro@pawpals.app",
            nombreVisible = "Raro",
            rol = "valor_inesperado",
        ).aPerfilUsuario()

        assertTrue(perfil.rol == RolUsuario.USUARIO)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "rol desconocido",
            detalle = "un rol no reconocido se trata como usuario normal, sin privilegios",
        )
    }

    @Test
    fun filaUsuario_aPerfilUsuario_rolUsuarioPorDefecto() {
        // la fila puede llegar sin campo rol; debe asumirse "usuario"
        val perfil = FilaUsuario(id = "u1").aPerfilUsuario()

        assertTrue(perfil.rol == RolUsuario.USUARIO)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "rol por defecto",
            detalle = "el rol por defecto es usuario, no administrador",
        )
    }

    @Test
    fun filaMensaje_aMensaje_mantieneIdRemitenteYTexto() {
        val fila = FilaMensaje(
            id = "m_xyz",
            conversacionId = "ana__lucia",
            uidRemitente = "ana",
            texto = "Hola Lucia",
            marcaTemporal = 999L,
        )

        val mensaje = fila.aMensaje()

        assertTrue(mensaje.id == "m_xyz")
        assertTrue(mensaje.uidRemitente == "ana")
        assertTrue(mensaje.texto == "Hola Lucia")
        assertTrue(mensaje.marcaTemporal == 999L)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila mensaje a modelo",
            detalle = "los campos del servidor se preservan al pasar al modelo de la app",
        )
    }

    @Test
    fun filaSolicitud_aSolicitud_traduceLosTresEstados() {
        val pendiente = FilaSolicitud("s1", "u1", "u2", "pendiente", 1L).aSolicitud().estado
        val aceptada = FilaSolicitud("s2", "u1", "u2", "aceptada", 1L).aSolicitud().estado
        val rechazada = FilaSolicitud("s3", "u1", "u2", "rechazada", 1L).aSolicitud().estado
        // cualquier valor desconocido tiene que comportarse como pendiente para no perder datos
        val desconocido = FilaSolicitud("s4", "u1", "u2", "???", 1L).aSolicitud().estado

        assertTrue(pendiente == EstadoSolicitudAmistad.PENDIENTE)
        assertTrue(aceptada == EstadoSolicitudAmistad.ACEPTADA)
        assertTrue(rechazada == EstadoSolicitudAmistad.RECHAZADA)
        assertTrue(desconocido == EstadoSolicitudAmistad.PENDIENTE)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila solicitud a modelo",
            detalle = "los tres estados se mapean y un estado raro queda como pendiente",
        )
    }

    @Test
    fun filaReporte_aReporte_cubreLosTresTiposYTresEstados() {
        val tipoUsuario = FilaReporte("r1", "usuario", "u2", "u1", "spam", "abierto", 1L).aReporte()
        val tipoMensaje =
            FilaReporte("r2", "mensaje", "m1", "u1", "ofensivo", "revisado", 1L).aReporte()
        val tipoPerro =
            FilaReporte("r3", "perro", "p2", "u1", "fake", "accion_tomada", 1L).aReporte()

        assertTrue(tipoUsuario.tipoObjetivo == TipoObjetivoReporte.USUARIO)
        assertTrue(tipoMensaje.tipoObjetivo == TipoObjetivoReporte.MENSAJE)
        assertTrue(tipoPerro.tipoObjetivo == TipoObjetivoReporte.PERRO)

        assertTrue(tipoUsuario.estado == EstadoReporte.ABIERTO)
        assertTrue(tipoMensaje.estado == EstadoReporte.REVISADO)
        assertTrue(tipoPerro.estado == EstadoReporte.ACCION_TOMADA)
        RegistroPruebas.ok(
            tipo = "integracion",
            caso = "fila reporte a modelo",
            detalle = "los tipos y estados de reporte se traducen al modelo correctamente",
        )
    }

    @Test
    fun idsAuthCoinciden_aceptaIdsConEspaciosAlrededor() {
        // supabase a veces devuelve uids con espacios; no deberian considerarse distintos
        assertTrue(idsAuthCoinciden("  abc-123  ", "abc-123"))
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "ids con espacios",
            detalle = "los ids siguen siendo iguales aunque vengan con espacios sobrantes",
        )
    }
}

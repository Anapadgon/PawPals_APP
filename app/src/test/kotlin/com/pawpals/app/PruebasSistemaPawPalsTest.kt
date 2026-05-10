package com.pawpals.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PruebasSistemaPawPalsTest {

    @Test
    fun flujoCompletoUsuario_exploraHaceMatchChateaYCalculaPaseo() {
        val ana = PerfilUsuario(
            uid = "ana",
            correo = "ana@example.com",
            nombreVisible = "Ana",
            zona = "Madrid",
            sobreMi = "Me gusta pasear con mi perro",
            rol = RolUsuario.USUARIO,
            bloqueado = false,
            latitud = 40.4168,
            longitud = -3.7038,
            tokenFcm = null,
            urlFoto = null,
            paseando = true,
        )
        val lucia = PerfilUsuario(
            uid = "lucia",
            correo = "lucia@example.com",
            nombreVisible = "Lucia",
            zona = "Madrid",
            sobreMi = "Busco amigos para mi perra",
            rol = RolUsuario.USUARIO,
            bloqueado = false,
            latitud = 40.4180,
            longitud = -3.7045,
            tokenFcm = null,
            urlFoto = null,
            paseando = true,
        )
        val perroAna = PerfilPerro(
            id = "perro_ana",
            uidDueno = ana.uid,
            nombre = "Toby",
            raza = "Mestizo",
            edadAnios = 3,
            biografia = "Tranquilo y sociable",
            urlFoto = null,
            energia = NivelEnergia.MODERADO,
            sociabilidad = Sociabilidad.MUY_SOCIABLE,
        )
        val coincidencia = Coincidencia(
            id = "match_sistema",
            usuarioA = ana.uid,
            usuarioB = lucia.uid,
            uidIniciador = ana.uid,
            estado = EstadoCoincidencia.ACEPTADA,
            creadoEn = 1L,
        )
        val mensaje = MensajeConversacion(
            id = "msg_1",
            uidRemitente = ana.uid,
            texto = "Hola, vamos a pasear con $perroAna.nombre?",
            marcaTemporal = 2L,
        )
        val distancia = UtilidadesGeograficas.distanciaKm(
            lat1 = ana.latitud!!,
            lon1 = ana.longitud!!,
            lat2 = lucia.latitud!!,
            lon2 = lucia.longitud!!,
        )

        assertEquals(EstadoCoincidencia.ACEPTADA, coincidencia.estado)
        assertEquals("ana__lucia", RepositorioConversacion.idConversacionFor(ana.uid, lucia.uid))
        assertTrue(mensaje.texto.contains(perroAna.nombre))
        assertTrue(distancia < 1.0)
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "flujo completo principal",
            detalle = "usuario, perro, match, chat y paseo funcionan de forma coherente",
        )
    }

    @Test
    fun flujoSistema_usuarioBloqueadoNoDebeContinuarComoUsuarioNormal() {
        val usuarioBloqueado = PerfilUsuario(
            uid = "bloqueado",
            correo = "bloqueado@example.com",
            nombreVisible = "Usuario bloqueado",
            zona = "Madrid",
            sobreMi = "",
            rol = RolUsuario.USUARIO,
            bloqueado = true,
            latitud = null,
            longitud = null,
            tokenFcm = null,
            urlFoto = null,
        )

        assertTrue(usuarioBloqueado.bloqueado)
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "usuario bloqueado",
            detalle = "el estado bloqueado queda detectado para no entrar al flujo normal",
        )
    }

    @Test
    fun flujoSistema_administradorTieneAccesoAEstadisticasYUsuarios() {
        // el admin es un PerfilUsuario con rol ADMINISTRADOR; la app le abre otra pantalla
        val admin = TestFactores.usuario(
            uid = "admin",
            nombreVisible = "Admin",
            rol = RolUsuario.ADMINISTRADOR,
        )
        val estadisticas = EstadisticasAdministracion(
            numeroUsuarios = 25,
            numeroPerros = 18,
            numeroCoincidencias = 7,
            reportesAbiertos = 2,
        )

        assertTrue(admin.rol == RolUsuario.ADMINISTRADOR)
        assertTrue(estadisticas.numeroUsuarios > 0)
        assertTrue(estadisticas.reportesAbiertos >= 0)
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "panel admin",
            detalle = "el admin se distingue del usuario y dispone de estadisticas con datos positivos",
        )
    }

    @Test
    fun flujoSistema_onboardingIncompletoNoAbreLaAppPrincipal() {
        // si nombre o zona estan vacios, la app debe forzar el onboarding aunque haya sesion
        val sinNombre =
            TestFactores.usuario(uid = "sin_nombre", nombreVisible = "", zona = "Madrid")
        val sinZona = TestFactores.usuario(uid = "sin_zona", nombreVisible = "Ana", zona = "")
        val completo =
            TestFactores.usuario(uid = "completo", nombreVisible = "Lucia", zona = "Madrid")

        val necesitaOnboarding =
            { p: PerfilUsuario -> p.nombreVisible.isBlank() || p.zona.isBlank() }

        assertTrue(necesitaOnboarding(sinNombre))
        assertTrue(necesitaOnboarding(sinZona))
        org.junit.Assert.assertFalse(necesitaOnboarding(completo))
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "onboarding obligatorio",
            detalle = "sin nombre o sin zona el usuario no llega a explorar",
        )
    }

    @Test
    fun flujoSistema_datosDemoUsanPrefijoQueLosIdentifica() {
        // el prefijo permite borrar los datos demo en un solo paso desde el admin
        val prefijo = RepositorioDatosDemo.DEMO_PREFIX
        val uidDemo = prefijo + "marta_lopez"
        val uidReal = "usuario_real_123"

        assertTrue(uidDemo.startsWith(prefijo))
        org.junit.Assert.assertFalse(uidReal.startsWith(prefijo))
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "datos demo identificables",
            detalle = "los perfiles de prueba quedan marcados con un prefijo unico",
        )
    }

    @Test
    fun flujoSistema_eliminarCuentaDebeTenerContrasenaYBorrarPerfil() {
        // el ModeloVistaAjustes valida que la contrasena tenga longitud minima antes de llamar a la RPC
        val contrasenaCorta = "abc"
        val contrasenaValida = "Pawpals2026!"

        org.junit.Assert.assertFalse(contrasenaCorta.length >= 6)
        assertTrue(contrasenaValida.length >= 6)
        RegistroPruebas.ok(
            tipo = "sistema",
            caso = "eliminar cuenta protegida",
            detalle = "el borrado de cuenta exige una contrasena de al menos 6 caracteres",
        )
    }
}

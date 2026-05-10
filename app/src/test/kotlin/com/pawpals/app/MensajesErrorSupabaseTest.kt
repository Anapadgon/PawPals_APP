package com.pawpals.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MensajesErrorSupabaseTest {

    @Test
    fun mensajeErrorSupabaseHumano_traduceCredencialesInvalidas() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("invalid login credentials"),
        )

        assertEquals("Correo o contraseña incorrectos.", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "error login",
            detalle = "un error tecnico se muestra como mensaje entendible",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_ocultaMensajesTecnicosConUrl() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("request url https://demo.supabase.co with apikey secret"),
            predeterminado = "Error generico",
        )

        assertEquals("Error generico", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "ocultar datos tecnicos",
            detalle = "no se muestran urls ni claves tecnicas al usuario",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_devuelvePredeterminadoSiNoHayCausa() {
        val mensaje = mensajeErrorSupabaseHumano(
            causa = null,
            predeterminado = "No se pudo completar la accion",
        )

        assertEquals("No se pudo completar la accion", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "error nulo",
            detalle = "si no hay causa se usa el mensaje predeterminado",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceProblemasDeConexion() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("timeout while trying to connect"),
        )

        assertEquals(
            "No hay conexión o el servidor tarda en responder. Revisa tu internet.",
            mensaje
        )
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "error conexion",
            detalle = "el timeout se transforma en una indicacion clara para el usuario",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceDemasiadosIntentos() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("too_many_requests"),
        )

        assertEquals("Demasiados intentos. Espera un poco y prueba otra vez.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "demasiados intentos",
            detalle = "la app informa de que hay que esperar antes de reintentar",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceUsuarioYaRegistrado() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("User already registered"),
        )

        assertEquals("Ya existe una cuenta con este correo. Prueba a iniciar sesión.", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "correo duplicado en registro",
            detalle = "se redirige al usuario a iniciar sesion si ya tenia cuenta",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceCorreoNoConfirmado() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("Email not confirmed"),
        )

        assertEquals("Revisa tu correo y confirma la cuenta antes de entrar.", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "correo sin confirmar",
            detalle = "el usuario sabe que tiene que abrir el enlace del correo",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceContrasenaDebil() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("Password should be at least 6 characters"),
        )

        assertEquals("Elige una contraseña más segura (más larga o con más variedad).", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "contrasena debil",
            detalle = "el usuario recibe una indicacion clara sobre la fortaleza de su contrasena",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceErrorRlsComoFaltaDePermiso() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("new row violates row-level security policy"),
        )

        assertEquals(
            "No tienes permiso para esta acción. Si persiste, contacta con soporte.",
            mensaje
        )
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "permiso denegado en BD",
            detalle = "los errores tecnicos de RLS se traducen a 'no tienes permiso'",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_truncaMensajesDemasiadoLargos() {
        // mensajes muy largos a menudo son trazas tecnicas que no aportan al usuario
        val muyLargo = "a".repeat(400)
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException(muyLargo),
            predeterminado = "Algo salió mal",
        )

        assertEquals("Algo salió mal", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "mensaje demasiado largo",
            detalle = "el usuario nunca recibe un mensaje gigantesco; se cae al predeterminado",
        )
    }

    @Test
    fun mensajeErrorSupabaseHumano_traduceContrasenaIgualALaActual() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("New password should be different from the old password"),
        )

        assertEquals("La nueva contraseña debe ser distinta de la actual.", mensaje)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "contrasena igual al cambiarla",
            detalle = "si el usuario intenta poner la misma contrasena, se le avisa",
        )
    }
}

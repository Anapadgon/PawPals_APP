package com.pawpals.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PruebasDepuracionTest {

    @Test
    fun depuracion_errorFichaUsuarioFaltante_muestraMensajeUtil() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("user_row_missing"),
        )

        assertEquals(
            "Falta tu ficha de usuario en la base de datos. Vuelve atrás o cierra sesión y entra de nuevo.",
            mensaje
        )
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "ficha usuario faltante",
            detalle = "la app explica como recuperarse si falta el perfil en base de datos",
        )
    }

    @Test
    fun depuracion_errorSesionCaducada_muestraMensajeUtil() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("jwt expired"),
        )

        assertEquals("La sesión ha caducado. Vuelve a iniciar sesión.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "sesion caducada",
            detalle = "la app pide iniciar sesion de nuevo cuando el token caduca",
        )
    }

    @Test
    fun depuracion_stacktraceTecnicoNoSeMuestraAlUsuario() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("Exception at io.github.jan.supabase.internal.Client line 42"),
            predeterminado = "No se pudo completar la accion",
        )

        assertEquals("No se pudo completar la accion", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "ocultar stacktrace",
            detalle = "los detalles internos no aparecen en pantalla",
        )
    }

    @Test
    fun depuracion_errorAlIntentarRegistroBloqueado_muestraMensajeUtil() {
        // si supabase tiene desactivado el registro, el usuario tiene que saberlo
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("Signups not allowed for this instance"),
        )

        assertEquals("El registro con correo no está disponible ahora.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "registro deshabilitado",
            detalle = "si el servidor bloquea el registro, el usuario lo entiende sin tecnicismos",
        )
    }

    @Test
    fun depuracion_errorClaveDuplicada_seExplicaSinTecnicismos() {
        // por ejemplo, un email que ya existe en la base de datos
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("duplicate key value violates unique constraint"),
        )

        assertEquals("Ese dato ya está en uso. Prueba con otro.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "clave duplicada",
            detalle = "los duplicados de base de datos se explican como 'ya esta en uso'",
        )
    }

    @Test
    fun depuracion_errorReautenticacionRequerida_pideContrasenaDeNuevo() {
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("Action requires reauthentication"),
        )

        assertEquals(
            "Por seguridad, vuelve a introducir tu contraseña e inténtalo otra vez.",
            mensaje,
        )
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "reautenticacion necesaria",
            detalle = "acciones sensibles avisan al usuario de volver a introducir la contrasena",
        )
    }

    @Test
    fun depuracion_errorClaveForanea_seTraduceAFaltaDeDatos() {
        // por ejemplo, intentar crear un perro sin que exista el dueño aun
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("insert violates foreign key constraint"),
        )

        assertEquals("No se pudo completar la acción porque faltan datos relacionados.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "clave foranea rota",
            detalle = "errores de integridad referencial se muestran al usuario como falta de datos",
        )
    }

    @Test
    fun depuracion_errorSubidaArchivo_seExplicaComoErrorDeMedios() {
        // por ejemplo, subir una foto al bucket cuando no hay conexion estable
        val mensaje = mensajeErrorSupabaseHumano(
            IllegalStateException("storage bucket upload failed"),
        )

        assertEquals("No se pudo subir o descargar el archivo. Inténtalo de nuevo.", mensaje)
        RegistroPruebas.ok(
            tipo = "depuracion",
            caso = "error de subida",
            detalle = "fallos al subir fotos se notifican con un mensaje claro",
        )
    }
}

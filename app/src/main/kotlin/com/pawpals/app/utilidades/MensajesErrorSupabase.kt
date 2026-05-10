package com.pawpals.app

// convierte errores tecnicos de supabase en mensajes cortos para la pantalla
fun mensajeErrorSupabaseHumano(
    causa: Throwable?,
    predeterminado: String = "Algo salió mal. Inténtalo de nuevo en un momento.",
): String {
    val raw = causa?.message?.trim().orEmpty()
    if (raw.isEmpty()) return predeterminado

    val compact = raw.lowercase()

    when {
        compact.contains("over_email_send_rate_limit") ||
                compact.contains("email rate limit") ->
            return "Hemos enviado demasiados correos seguidos. Espera un minuto y vuelve a intentarlo."

        compact.contains("same_password") ||
                compact.contains("should be different") ->
            return "La nueva contraseña debe ser distinta de la actual."

        compact.contains("weak_password") ||
                compact.contains("password should be at least") ->
            return "Elige una contraseña más segura (más larga o con más variedad)."

        compact.contains("invalid_credentials") ||
                compact.contains("invalid login credentials") ||
                compact.contains("invalid_grant") ->
            return "Correo o contraseña incorrectos."

        compact.contains("user_already_registered") ||
                compact.contains("already registered") ||
                compact.contains("already been registered") ||
                compact.contains("user already exists") ->
            return "Ya existe una cuenta con este correo. Prueba a iniciar sesión."

        compact.contains("email not confirmed") ||
                compact.contains("email_not_confirmed") ->
            return "Revisa tu correo y confirma la cuenta antes de entrar."

        compact.contains("signup_disabled") ||
                compact.contains("signups not allowed") ->
            return "El registro con correo no está disponible ahora."

        compact.contains("reauthentication") ||
                compact.contains("reauthenticate") ||
                compact.contains("requires recent login") ->
            return "Por seguridad, vuelve a introducir tu contraseña e inténtalo otra vez."

        compact.contains("session_expired") ||
                compact.contains("jwt expired") ||
                compact.contains("token expired") ->
            return "La sesión ha caducado. Vuelve a iniciar sesión."

        compact.contains("not_authenticated") ||
                compact.contains("not authenticated") ->
            return "La sesión no es válida. Cierra sesión y vuelve a entrar."

        compact.contains("user_row_missing") ->
            return "Falta tu ficha de usuario en la base de datos. Vuelve atrás o cierra sesión y entra de nuevo."

        compact.contains("refresh_token_not_found") ||
                compact.contains("invalid refresh token") ->
            return "Sesión no válida. Inicia sesión de nuevo."

        compact.contains("user_not_found") ->
            return "No encontramos una cuenta con esos datos."

        compact.contains("too_many_requests") ||
                compact.contains(" 429") ||
                compact.contains("(429)") ->
            return "Demasiados intentos. Espera un poco y prueba otra vez."

        compact.contains("network") ||
                compact.contains("failed to connect") ||
                compact.contains("connection refused") ||
                compact.contains("timeout") ||
                compact.contains("unreachable") ||
                compact.contains("unable to resolve host") ->
            return "No hay conexión o el servidor tarda en responder. Revisa tu internet."

        compact.contains("row-level security") ||
                compact.contains(" new row violates row-level security") ||
                compact.contains("rls") ||
                compact.contains("permission denied") ||
                compact.contains("42501") ->
            return "No tienes permiso para esta acción. Si persiste, contacta con soporte."

        compact.contains("duplicate key") ||
                compact.contains("unique constraint") ->
            return "Ese dato ya está en uso. Prueba con otro."

        compact.contains("foreign key") ||
                compact.contains("violates foreign key") ->
            return "No se pudo completar la acción porque faltan datos relacionados."

        (compact.contains("storage") || compact.contains("bucket")) &&
                (compact.contains("object") || compact.contains("upload") || compact.contains("download")) ->
            return "No se pudo subir o descargar el archivo. Inténtalo de nuevo."

        compact.contains("jwt") && compact.contains("malformed") ->
            return "Sesión incorrecta. Cierra sesión y vuelve a entrar."
    }

    if (esMensajeTecnicoInvasivo(raw)) {
        return predeterminado
    }

    if (raw.length <= 120 &&
        !compact.contains("http") &&
        !compact.contains("exception") &&
        !compact.contains("at com.") &&
        !compact.contains("at io.github")
    ) {
        return raw
    }

    return predeterminado
}

private fun esMensajeTecnicoInvasivo(raw: String): Boolean {
    val lower = raw.lowercase()
    if (raw.length > 280) return true
    return lower.contains("http://") ||
            lower.contains("https://") ||
            lower.contains("authorization:") ||
            lower.contains("authorization\":") ||
            lower.contains("apikey") ||
            lower.contains("bearer ") ||
            lower.contains("request url") ||
            lower.contains("headers") ||
            lower.contains("supabase.co") ||
            lower.contains("x-client-info") ||
            lower.contains("accept-charset") ||
            lower.contains("curl ")
}

package com.pawpals.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pawDataStore by preferencesDataStore(name = "pawpals_preferencias")

@Singleton
// guarda ajustes simples del usuario sin tener que pedirlos al servidor
class PreferenciasUsuario @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificarMensajesKey = booleanPreferencesKey("notificar_mensajes")
    private val notificarPaseosKey = booleanPreferencesKey("notificar_paseos")
    private val notificarCoincidenciasKey = booleanPreferencesKey("notificar_coincidencias")
    private val ubicacionSoloDurantePaseoKey = booleanPreferencesKey("ubicacion_solo_durante_paseo")

    // valores por defecto pensados para que la app funcione desde el primer arranque
    val notificarMensajes: Flow<Boolean> = context.pawDataStore.data.map { it[notificarMensajesKey] ?: true }
    val notificarPaseos: Flow<Boolean> = context.pawDataStore.data.map { it[notificarPaseosKey] ?: true }
    val notificarCoincidencias: Flow<Boolean> = context.pawDataStore.data.map { it[notificarCoincidenciasKey] ?: true }
    val ubicacionSoloDurantePaseo: Flow<Boolean> =
        context.pawDataStore.data.map { it[ubicacionSoloDurantePaseoKey] ?: false }

    suspend fun establecerNotificarMensajes(value: Boolean) {
        context.pawDataStore.edit { it[notificarMensajesKey] = value }
    }

    suspend fun establecerNotificarPaseos(value: Boolean) {
        context.pawDataStore.edit { it[notificarPaseosKey] = value }
    }

    suspend fun establecerNotificarCoincidencias(value: Boolean) {
        context.pawDataStore.edit { it[notificarCoincidenciasKey] = value }
    }

    suspend fun establecerUbicacionSoloDurantePaseo(value: Boolean) {
        context.pawDataStore.edit { it[ubicacionSoloDurantePaseoKey] = value }
    }
}

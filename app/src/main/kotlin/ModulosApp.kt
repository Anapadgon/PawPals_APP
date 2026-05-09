package com.pawpals.app

import com.pawpals.app.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
// aqui se enlazan las interfaces con sus clases reales de supabase
abstract class ModuloEnlacesApp {
    @Binds @Singleton abstract fun bindRepositorioAutenticacion(impl: RepositorioAutenticacionSupabase): RepositorioAutenticacion
    @Binds @Singleton abstract fun bindRepositorioUsuario(impl: RepositorioUsuarioSupabase): RepositorioUsuario
    @Binds @Singleton abstract fun bindRepositorioPerro(impl: RepositorioPerroSupabase): RepositorioPerro
    @Binds @Singleton abstract fun bindRepositorioCoincidencia(impl: RepositorioCoincidenciaSupabase): RepositorioCoincidencia
    @Binds @Singleton abstract fun bindRepositorioConversacion(impl: RepositorioConversacionSupabase): RepositorioConversacion
    @Binds @Singleton abstract fun bindRepositorioModeracion(impl: RepositorioModeracionSupabase): RepositorioModeracion
    @Binds @Singleton abstract fun bindRepositorioEstadisticasAdministracion(
        impl: RepositorioEstadisticasAdministracionSupabase,
    ): RepositorioEstadisticasAdministracion
    @Binds @Singleton abstract fun bindRepositorioDatosDemo(impl: RepositorioDatosDemoSupabase): RepositorioDatosDemo
    @Binds @Singleton abstract fun bindRepositorioAlmacenamiento(impl: RepositorioAlmacenamientoSupabase): RepositorioAlmacenamiento
    @Binds @Singleton abstract fun bindRepositorioDeslizamiento(impl: RepositorioDeslizamientoSupabase): RepositorioDeslizamiento
    @Binds @Singleton abstract fun bindRepositorioAmistad(impl: RepositorioAmistadSupabase): RepositorioAmistad
    @Binds @Singleton abstract fun bindRepositorioSoporte(impl: RepositorioSoporteSupabase): RepositorioSoporte
}

@Module
@InstallIn(SingletonComponent::class)
object ModuloSupabase {
    @Provides
    @Singleton
    // crea un unico cliente compartido para auth, base de datos y storage
    fun proveerClienteSupabase(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        check(url.isNotEmpty() && key.isNotEmpty()) {
            "Añade SUPABASE_URL y SUPABASE_ANON_KEY en local.properties y sincroniza Gradle."
        }
        return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}

plugins {
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    // Debe coincidir con la versión de Kotlin (p. ej. Kotlin 2.2.10 → KSP 2.2.10-2.0.2). No uses un número suelto tipo "2.3.2".
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

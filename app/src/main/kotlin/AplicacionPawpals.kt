package com.pawpals.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// activa hilt para poder inyectar repositorios y servicios en toda la app
@HiltAndroidApp
class AplicacionPawpals : Application()

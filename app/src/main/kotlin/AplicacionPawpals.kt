package com.pawpals.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: genera el grafo de dependencias en toda la app. */
@HiltAndroidApp
class AplicacionPawpals : Application()

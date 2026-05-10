package com.pawpals.app

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilidadesGeograficasTest {

    @Test
    fun distanciaKm_devuelveCeroSiLasCoordenadasSonIguales() {
        val distancia = UtilidadesGeograficas.distanciaKm(
            lat1 = 40.4168,
            lon1 = -3.7038,
            lat2 = 40.4168,
            lon2 = -3.7038,
        )

        assertEquals(0.0, distancia, 0.0001)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "distancia igual",
            detalle = "la misma coordenada devuelve 0 km",
        )
    }

    @Test
    fun distanciaKm_calculaDistanciaAproximadaEntreMadridYBarcelona() {
        val distancia = UtilidadesGeograficas.distanciaKm(
            lat1 = 40.4168,
            lon1 = -3.7038,
            lat2 = 41.3874,
            lon2 = 2.1686,
        )

        assertTrue(abs(distancia - 505.0) < 15.0)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "distancia madrid barcelona",
            detalle = "distancia calculada: ${"%.2f".format(distancia)} km",
        )
    }

    @Test
    fun distanciaKm_esSimetricaEntreDosPuntos() {
        val ida = UtilidadesGeograficas.distanciaKm(
            lat1 = 40.4168,
            lon1 = -3.7038,
            lat2 = 41.3874,
            lon2 = 2.1686,
        )
        val vuelta = UtilidadesGeograficas.distanciaKm(
            lat1 = 41.3874,
            lon1 = 2.1686,
            lat2 = 40.4168,
            lon2 = -3.7038,
        )

        assertEquals(ida, vuelta, 0.0001)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "distancia simetrica",
            detalle = "la distancia de ida y vuelta coincide",
        )
    }

    @Test
    fun distanciaKm_dosPuntosCercanosDanMenosDeUnKilometro() {
        // simula dos perros en el mismo barrio
        val km = UtilidadesGeograficas.distanciaKm(
            lat1 = 40.4168,
            lon1 = -3.7038,
            lat2 = 40.4180,
            lon2 = -3.7045,
        )

        assertTrue(km in 0.0..1.0)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "distancia barrio",
            detalle = "dos puntos a unos pocos metros caen por debajo de 1 km",
        )
    }

    @Test
    fun distanciaKm_nuncaDevuelveValoresNegativos() {
        // la distancia es un valor absoluto, no puede ser negativa nunca
        val km = UtilidadesGeograficas.distanciaKm(
            lat1 = -33.8688,
            lon1 = 151.2093,
            lat2 = 51.5074,
            lon2 = -0.1278,
        )

        assertTrue(km >= 0.0)
        RegistroPruebas.ok(
            tipo = "unitaria",
            caso = "distancia no negativa",
            detalle = "una distancia siempre es positiva o cero",
        )
    }
}

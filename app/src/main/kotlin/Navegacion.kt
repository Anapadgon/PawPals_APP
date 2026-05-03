package com.pawpals.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object DestinosPaw {
    const val ONBOARDING_INTRO = "bienvenida_intro"
    const val LOGIN = "inicio_sesion"
    const val REGISTER = "registro"
    const val ONBOARDING_HUMAN = "bienvenida_humano"
    const val ONBOARDING_DOG = "bienvenida_perro"
    const val EXPLORAR = "explorar"
    const val CHATS = "conversaciones"
    const val PASEOS = "paseos"
    const val PROFILE = "perfil"
    const val SETTINGS = "ajustes"
    const val MATCH_RESULT = "resultado_coincidencia/{otroUid}"
    const val CHAT_THREAD = "conversacion/{otroUid}"
    const val REPORT = "reporte/{tipoObjetivo}/{idObjetivo}"
    const val ADMIN = "administradoristracion"

    fun conversacionThread(otroUid: String) = "conversacion/$otroUid"
    fun coincidenciaResult(otroUid: String) = "resultado_coincidencia/$otroUid"
    fun report(tipoObjetivo: String, idObjetivo: String) = "reporte/$tipoObjetivo/$idObjetivo"
}

@Composable
fun ContenedorPrincipalUsuario(
    miUid: String,
    nombreVisible: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val permisos = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    LaunchedEffect(Unit) {
        lanzadorPermisos.launch(permisos)
    }

    val pilaNavegacion by navController.currentBackStackEntryAsState()
    val rutaActual = pilaNavegacion?.destination?.route
    val rutasPestanas = setOf(
        DestinosPaw.EXPLORAR,
        DestinosPaw.CHATS,
        DestinosPaw.PASEOS,
        DestinosPaw.PROFILE,
    )
    val mostrarBarraInferior = rutaActual in rutasPestanas

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (mostrarBarraInferior) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(
                        Triple(DestinosPaw.EXPLORAR, "Explorar", Icons.Filled.Home),
                        Triple(DestinosPaw.CHATS, "Conversaciones", Icons.Filled.Chat),
                        Triple(DestinosPaw.PASEOS, "Paseos", Icons.Filled.Place),
                        Triple(DestinosPaw.PROFILE, "Perfil", Icons.Filled.Person),
                    ).forEach { (ruta, etiqueta, icono) ->
                        NavigationBarItem(
                            selected = rutaActual == ruta,
                            onClick = {
                                navController.navigate(ruta) {
                                    popUpTo(DestinosPaw.EXPLORAR) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icono, contentDescription = etiqueta) },
                            label = { Text(etiqueta) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoralPrimary,
                                selectedTextColor = CoralPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = PeachPale,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DestinosPaw.EXPLORAR,
            modifier = Modifier.padding(padding),
        ) {
            composable(DestinosPaw.EXPLORAR) {
                PantallaExplorar(
                    miUid = miUid,
                    onOpenCoincidencia = { otroUid ->
                        navController.navigate(DestinosPaw.coincidenciaResult(otroUid))
                    },
                )
            }
            composable(DestinosPaw.CHATS) {
                PantallaListaConversaciones(
                    miUid = miUid,
                    onOpenThread = { otroUidConversacion ->
                        navController.navigate(DestinosPaw.conversacionThread(otroUidConversacion))
                    },
                )
            }
            composable(DestinosPaw.PASEOS) {
                PantallaMapa(miUid = miUid)
            }
            composable(DestinosPaw.PROFILE) {
                PantallaPerfil(
                    miUid = miUid,
                    onOpenSettings = { navController.navigate(DestinosPaw.SETTINGS) },
                    onOpenReport = { type, id ->
                        navController.navigate(DestinosPaw.report(type, id))
                    },
                )
            }
            composable(DestinosPaw.SETTINGS) {
                PantallaAjustes(onBack = { navController.popBackStack() })
            }
            composable(
                route = DestinosPaw.CHAT_THREAD,
                arguments = listOf(navArgument("otroUid") { type = NavType.StringType }),
            ) {
                PantallaConversacion(
                    miUid = miUid,
                    onBack = { navController.popBackStack() },
                    onOpenReport = { type, id ->
                        navController.navigate(DestinosPaw.report(type, id))
                    },
                )
            }
            composable(
                route = DestinosPaw.MATCH_RESULT,
                arguments = listOf(navArgument("otroUid") { type = NavType.StringType }),
            ) { entry ->
                val otroUid = entry.arguments?.getString("otroUid").orEmpty()
                PantallaResultadoCoincidencia(
                    otroUid = otroUid,
                    onSendMessage = {
                        navController.navigate(DestinosPaw.conversacionThread(otroUid)) {
                            popUpTo(DestinosPaw.EXPLORAR)
                        }
                    },
                    onKeepExploring = {
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = DestinosPaw.REPORT,
                arguments = listOf(
                    navArgument("tipoObjetivo") { type = NavType.StringType },
                    navArgument("idObjetivo") { type = NavType.StringType },
                ),
            ) {
                PantallaReporte(
                    miUid = miUid,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
fun AnfitrionNavegacionRaiz(
    modifier: Modifier = Modifier,
    rootViewModel: ModeloVistaRaiz = hiltViewModel(),
) {
    val sesion by rootViewModel.estadoSesion.collectAsStateWithLifecycle()
    val bienvenidaHecha by rootViewModel.bienvenidaCompletada.collectAsStateWithLifecycle()

    when (val s = sesion) {
        EstadoUiSesion.Cargando -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        EstadoUiSesion.SinSesion -> {
            val navController = rememberNavController()
            key(bienvenidaHecha) {
                NavHost(
                    navController = navController,
                    startDestination = if (bienvenidaHecha) {
                        DestinosPaw.LOGIN
                    } else {
                        DestinosPaw.ONBOARDING_INTRO
                    },
                    modifier = modifier,
                ) {
                    composable(DestinosPaw.ONBOARDING_INTRO) {
                        PantallaBienvenida(
                            onContinue = {
                                rootViewModel.completarBienvenida()
                                navController.navigate(DestinosPaw.LOGIN) {
                                    popUpTo(DestinosPaw.ONBOARDING_INTRO) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(DestinosPaw.LOGIN) {
                        PantallaInicioSesion(
                            onGoRegister = { navController.navigate(DestinosPaw.REGISTER) },
                        )
                    }
                    composable(DestinosPaw.REGISTER) {
                        PantallaRegistro(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
        is EstadoUiSesion.SesionUsuario -> {
            val perfil = s.perfil
            val necesitaConfiguracionPerfil = perfil == null ||
                perfil.nombreVisible.isBlank() ||
                perfil.zona.isBlank()

            if (necesitaConfiguracionPerfil) {
                val configuracionPerfil: ModeloVistaConfiguracionPerfil = hiltViewModel()
                val bienvenidaNav = rememberNavController()
                NavHost(
                    navController = bienvenidaNav,
                    startDestination = DestinosPaw.ONBOARDING_HUMAN,
                    modifier = modifier,
                ) {
                    composable(DestinosPaw.ONBOARDING_HUMAN) {
                        OnboardingHumanScreen(
                            onNext = { bienvenidaNav.navigate(DestinosPaw.ONBOARDING_DOG) },
                            viewModel = configuracionPerfil,
                            onSignOut = { rootViewModel.cerrarSesion() },
                        )
                    }
                    composable(DestinosPaw.ONBOARDING_DOG) {
                        OnboardingDogScreen(
                            miUid = s.cuenta.uid,
                            onFinish = { },
                            viewModel = configuracionPerfil,
                            onBack = { bienvenidaNav.popBackStack() },
                            onSignOut = { rootViewModel.cerrarSesion() },
                        )
                    }
                }
            } else {
                val nombre = perfil.nombreVisible.takeIf { it.isNotBlank() }
                    ?: s.cuenta.correo?.substringBefore("@").orEmpty().ifBlank { "Amigo" }
                ContenedorPrincipalUsuario(
                    miUid = s.cuenta.uid,
                    nombreVisible = nombre,
                    modifier = modifier,
                )
            }
        }
        is EstadoUiSesion.Administrador -> {
            PantallaAdministracion(
                onSignOut = { rootViewModel.cerrarSesion() },
                modifier = modifier,
            )
        }
        EstadoUiSesion.Bloqueado -> {
            PantallaBloqueado(
                onSignOut = { rootViewModel.cerrarSesion() },
                modifier = modifier,
            )
        }
    }
}

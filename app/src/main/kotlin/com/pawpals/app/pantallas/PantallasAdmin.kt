package com.pawpals.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// vista del admin para usuarios, reportes y datos de prueba
fun PantallaAdministracion(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaAdministracion = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Usuarios", "Estadísticas", "Moderación")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Panel administrador") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Salir")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (tab) {
                0 -> AdminUsersTab(estado, viewModel)
                1 -> EstadisticasAdministracionTab(estado, viewModel)
                2 -> AdminModerationTab(estado, viewModel)
            }
        }
    }
}

@Composable
private fun AdminUsersTab(estado: EstadoUiAdministracion, vm: ModeloVistaAdministracion) {
    if (estado.cargando) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(estado.usuarios, key = { it.uid }) { u ->
            Column {
                Text(
                    u.nombreVisible.ifBlank { u.correo },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(u.correo, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Rol: ${u.rol.name} · Bloqueado: ${u.bloqueado}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                BotonPrimarioPaw(
                    texto = if (u.bloqueado) "Desbanear" else "Banear",
                    onClick = { vm.establecerBloqueado(u.uid, !u.bloqueado) },
                )
                Spacer(Modifier.height(8.dp))
                BotonPrimarioPaw(
                    texto = "Eliminar perfil de la base de datos",
                    onClick = { vm.eliminarUsuario(u.uid) },
                )
            }
        }
    }
}

@Composable
private fun EstadisticasAdministracionTab(
    estado: EstadoUiAdministracion,
    vm: ModeloVistaAdministracion
) {
    Column(Modifier.padding(16.dp)) {
        if (estado.cargando) {
            CircularProgressIndicator()
        }
        val estadisticas = estado.estadisticas
        if (estadisticas != null) {
            TituloSeccion("Resumen")
            Text("Usuarios: ${estadisticas.numeroUsuarios}")
            Text("Perros: ${estadisticas.numeroPerros}")
            Text("Matches: ${estadisticas.numeroCoincidencias}")
            Text("Reportes abiertos: ${estadisticas.reportesAbiertos}")
        } else {
            Text("Sin datos")
        }

        Spacer(Modifier.height(24.dp))
        TituloSeccion("Datos de prueba")
        Text(
            "Crea un conjunto de perfiles ficticios con sus perros para poder " +
                    "probar el listado \"Explorar\", los matches y las conversaciones sin registrar " +
                    "cuentas nuevas. Se colocan cerca de tu ubicación actual.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        BotonPrimarioPaw(
            texto = if (estado.sembrando) "Procesando…" else "Crear perfiles de prueba",
            onClick = { vm.sembrarPerfilesDemo() },
            habilitado = !estado.sembrando,
        )
        Spacer(Modifier.height(8.dp))
        BotonContornoPaw(
            texto = "Eliminar perfiles de prueba",
            onClick = { vm.limpiarPerfilesDemo() },
            habilitado = !estado.sembrando,
            colorBorde = CoralPrimary,
            colorContenido = CoralPrimary,
        )
        estado.mensaje?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = CoralPrimary)
        }
    }
}

@Composable
private fun AdminModerationTab(estado: EstadoUiAdministracion, vm: ModeloVistaAdministracion) {
    if (estado.reportes.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = PeachPale,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Sin reportes pendientes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Cuando un usuario envíe un reporte o un ticket de soporte aparecerá aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(estado.reportes, key = { it.id }) { r ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceMuted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Gavel,
                            null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Reporte · ${r.tipoObjetivo.name.lowercase()}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoralPrimary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Objetivo: ${r.idObjetivo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        r.motivo.ifBlank { "(Sin motivo)" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonContornoPaw(
                            texto = "Revisado",
                            onClick = { vm.resolverReporte(r.id, false) },
                            colorBorde = CoralPrimary,
                            colorContenido = CoralPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        BotonPrimarioPaw(
                            texto = "Acción tomada",
                            onClick = { vm.resolverReporte(r.id, true) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
// formulario simple para enviar una incidencia a moderacion
fun PantallaReporte(
    miUid: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeloVistaReporte = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var motivo by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        BarraSuperiorPaw(titulo = "Reportar contenido", alVolver = onBack)
        Text(
            text = viewModel.etiquetaObjetivo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        CampoContornoPaw(
            valor = motivo,
            alCambiarValor = { motivo = it },
            etiqueta = "Describe el motivo",
            unaLinea = false,
        )
        Spacer(Modifier.height(16.dp))
        if (estado.completado) {
            Text(
                "Gracias, el equipo revisará el reporte.",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            BotonPrimarioPaw(texto = "Volver", onClick = onDone)
        } else {
            BotonPrimarioPaw(
                texto = if (estado.enviando) "Enviando…" else "Enviar reporte",
                habilitado = !estado.enviando && motivo.isNotBlank(),
                onClick = { viewModel.enviar(miUid, motivo) },
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

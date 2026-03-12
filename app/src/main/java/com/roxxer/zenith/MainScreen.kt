package com.roxxer.zenith

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.flow.StateFlow
import androidx.core.graphics.toColorInt

sealed class RutaApp(val ruta: String, val icono: ImageVector, val titulo: String) {
    object Inicio : RutaApp("inicio", Icons.Rounded.Home, "Inicio")
    object Buscar : RutaApp("buscar", Icons.Rounded.Search, "Buscar")
    object Biblioteca : RutaApp("biblioteca", Icons.Rounded.LibraryMusic, "Biblioteca")
    object Playlists : RutaApp("playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, "Playlist")
    object Ajustes : RutaApp("ajustes", Icons.Rounded.Settings, "Ajustes")
    object AjustesAudio : RutaApp("ajustes_audio", Icons.Rounded.GraphicEq, "Audio")
    object Ecualizador : RutaApp("ecualizador", Icons.Rounded.Tune, "Ecualizador")
    object AjustesInterfaz : RutaApp("ajustes_interfaz", Icons.Rounded.Palette, "Interfaz")
    object AjustesAlmacenamiento : RutaApp("ajustes_almacenamiento", Icons.Rounded.Storage, "Almacenamiento")
    object AjustesOtros : RutaApp("ajustes_otros", Icons.Rounded.Extension, "Otros")
    object AjustesPrivacidad : RutaApp("ajustes_privacidad", Icons.Rounded.Security, "Privacidad")
    object AjustesAcercaDe : RutaApp("ajustes_acerca_de", Icons.Rounded.Favorite, "Acerca de")
}

@Composable
fun MainScreen(musicViewModel: MusicViewModel = viewModel()) {
    val navController = rememberNavController()
    val rutaActual by navController.currentBackStackEntryAsState()
    val destinoActual = rutaActual?.destination?.route
    val context = LocalContext.current

    val cancionSonando by musicViewModel.cancionActual.collectAsState()
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()
    var reproductorExpandido by remember { mutableStateOf(false) }

    val duracionTotal by musicViewModel.duracionTotal.collectAsState()
    val favoritos by musicViewModel.listaFavoritos.collectAsState()
    val mensajeGlobal by musicViewModel.mensajeNotificacion.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()

    val acentoHex by musicViewModel.colorAcento.collectAsState()
    val colorDinamico = remember(acentoHex) {
        try { Color(acentoHex.toColorInt()) } catch (_: Exception) { Color(0xFF1DB954) }
    }

    val temaGlobal by musicViewModel.temaGlobal.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val esModoOscuro = when (temaGlobal) {
        "Claro" -> false
        "Oscuro" -> true
        else -> isSystemDark
    }

    val animacionesActivas by musicViewModel.animacionesActivas.collectAsState()
    val listaPestanas = listOf(RutaApp.Inicio, RutaApp.Buscar, RutaApp.Biblioteca, RutaApp.Playlists)

    val ocultarBottomBar = destinoActual == RutaApp.Ajustes.ruta ||
            destinoActual == RutaApp.AjustesAudio.ruta ||
            destinoActual == RutaApp.Ecualizador.ruta ||
            destinoActual == RutaApp.AjustesInterfaz.ruta ||
            destinoActual == RutaApp.AjustesAlmacenamiento.ruta ||
            destinoActual == RutaApp.AjustesOtros.ruta ||
            destinoActual == RutaApp.AjustesPrivacidad.ruta ||
            destinoActual == RutaApp.AjustesAcercaDe.ruta

    var tiempoUltimoToqueAtras by remember { mutableLongStateOf(0L) }

    if (!reproductorExpandido && (destinoActual == RutaApp.Inicio.ruta || destinoActual == RutaApp.Buscar.ruta || destinoActual == RutaApp.Biblioteca.ruta || destinoActual == RutaApp.Playlists.ruta)) {
        BackHandler {
            val tiempoActual = System.currentTimeMillis()
            if (tiempoActual - tiempoUltimoToqueAtras < 2000) {
                (context as? Activity)?.moveTaskToBack(true)
            } else {
                tiempoUltimoToqueAtras = tiempoActual
                musicViewModel.mostrarMensaje("Toca de nuevo para salir 🎧")
            }
        }
    }

    CompositionLocalProvider(LocalThemeIsDark provides esModoOscuro) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = DarkBackground,
                bottomBar = {
                    if (!ocultarBottomBar) {
                        Column(modifier = Modifier.background(Color.Transparent)) {
                            AnimatedVisibility(
                                visible = cancionSonando != null && !reproductorExpandido,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = Modifier.fillMaxWidth()
                            ) {
                                val esFavorito = cancionSonando?.uri?.toString()?.let { favoritos.contains(it) } ?: false
                                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                    MiniReproductor(
                                        cancion = cancionSonando!!,
                                        portadasCustom = portadasCustom,
                                        estaReproduciendo = estaReproduciendo,
                                        posicionFlow = musicViewModel.posicionActual,
                                        duracionTotal = duracionTotal,
                                        esFavorito = esFavorito,
                                        colorAcento = colorDinamico,
                                        animacionesActivas = animacionesActivas,
                                        alAlternarFavorito = { musicViewModel.alternarFavorito(cancionSonando!!.uri.toString()) },
                                        alAnterior = { musicViewModel.anteriorCancion() }, alAlternarPlay = { musicViewModel.alternarPlayPause() }, alSiguiente = { musicViewModel.siguienteCancion() }, alExpandir = { reproductorExpandido = true }
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().height(64.dp).background(DarkSurface), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                listaPestanas.forEach { pestana ->
                                    val seleccionado = destinoActual == pestana.ruta
                                    val colorElemento = if (seleccionado) colorDinamico else DarkTextSecondary
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp, vertical = 6.dp).clip(RoundedCornerShape(12.dp)).clickable {
                                            reproductorExpandido = false
                                            if (!seleccionado) {
                                                navController.navigate(pestana.ruta) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                                            } else {
                                                if (pestana.ruta == RutaApp.Biblioteca.ruta) musicViewModel.explorarTabBiblioteca(0)
                                                navController.navigate(pestana.ruta) {
                                                    popUpTo(pestana.ruta) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = pestana.icono, contentDescription = pestana.titulo, modifier = Modifier.size(26.dp), tint = colorElemento)
                                        Text(text = pestana.titulo, fontSize = 11.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium, color = colorElemento, modifier = Modifier.offset(y = 1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
                    NavHost(navController = navController, startDestination = RutaApp.Inicio.ruta) {
                        composable(RutaApp.Inicio.ruta) { HomeScreen(musicViewModel = musicViewModel, onIrABiblioteca = { navController.navigate(RutaApp.Biblioteca.ruta) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, onIrAPlaylists = { navController.navigate(RutaApp.Playlists.ruta) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, onIrAAjustes = { navController.navigate(RutaApp.Ajustes.ruta) }) }

                        // --- MAGIA: Inyectamos el poder de navegación al buscador ---
                        composable(RutaApp.Buscar.ruta) {
                            SearchScreen(
                                musicViewModel = musicViewModel,
                                onIrABiblioteca = { navController.navigate(RutaApp.Biblioteca.ruta) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                onIrAPlaylists = { navController.navigate(RutaApp.Playlists.ruta) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }
                            )
                        }

                        composable(RutaApp.Biblioteca.ruta) { LibraryScreen(musicViewModel) }
                        composable(RutaApp.Playlists.ruta) { PlaylistsScreen(musicViewModel) }

                        composable(RutaApp.Ajustes.ruta) {
                            SettingsScreen(
                                musicViewModel = musicViewModel,
                                onVolver = { navController.popBackStack() },
                                onIrAAudio = { navController.navigate(RutaApp.AjustesAudio.ruta) },
                                onIrAInterfaz = { navController.navigate(RutaApp.AjustesInterfaz.ruta) },
                                onIrAAlmacenamiento = { navController.navigate(RutaApp.AjustesAlmacenamiento.ruta) },
                                onIrAOtros = { navController.navigate(RutaApp.AjustesOtros.ruta) },
                                onIrAPrivacidad = { navController.navigate(RutaApp.AjustesPrivacidad.ruta) },
                                onIrAAcercaDe = { navController.navigate(RutaApp.AjustesAcercaDe.ruta) }
                            )
                        }
                        composable(RutaApp.AjustesAudio.ruta) { AudioSettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }, onIrAEcualizador = { navController.navigate(RutaApp.Ecualizador.ruta) }) }
                        composable(RutaApp.Ecualizador.ruta) { EqualizerScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                        composable(RutaApp.AjustesInterfaz.ruta) { InterfaceSettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                        composable(RutaApp.AjustesAlmacenamiento.ruta) { StorageSettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                        composable(RutaApp.AjustesOtros.ruta) { ExtraSettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                        composable(RutaApp.AjustesPrivacidad.ruta) { PrivacySettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                        composable(RutaApp.AjustesAcercaDe.ruta) { AboutSettingsScreen(musicViewModel = musicViewModel, onVolver = { navController.popBackStack() }) }
                    }
                }

                AnimatedVisibility(
                    visible = cancionSonando != null && reproductorExpandido,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(500, easing = LinearOutSlowInEasing)) + fadeIn(tween(500)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut(tween(500)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    NowPlayingScreen(cancion = cancionSonando!!, alCerrar = { reproductorExpandido = false }, alIrAlContexto = { val rutaDestino = musicViewModel.prepararNavegacionAContexto(); reproductorExpandido = false; if (destinoActual != rutaDestino) { navController.navigate(rutaDestino) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } } })
                }
            }

            AnimatedVisibility(
                visible = mensajeGlobal != null, enter = slideInVertically(initialOffsetY = { -300 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(500)), exit = slideOutVertically(targetOffsetY = { -300 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut(tween(500)), modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
            ) {
                Row(modifier = Modifier.shadow(20.dp, RoundedCornerShape(50), ambientColor = colorDinamico).background(DarkSurface, RoundedCornerShape(50)).border(1.dp, colorDinamico.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colorDinamico, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = mensajeGlobal ?: "", color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun MiniReproductor(
    cancion: AudioModel,
    portadasCustom: Map<String, String>,
    estaReproduciendo: Boolean,
    posicionFlow: StateFlow<Long>,
    duracionTotal: Long, esFavorito: Boolean, colorAcento: Color, animacionesActivas: Boolean,
    alAlternarFavorito: () -> Unit, alAnterior: () -> Unit, alAlternarPlay: () -> Unit, alSiguiente: () -> Unit, alExpandir: () -> Unit
) {
    val pathCustom = portadasCustom["cover_song_${cancion.id}"]
    val imagenAUsar = pathCustom ?: cancion.albumArtUri.toString()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = colorAcento).clip(RoundedCornerShape(12.dp)).background(DarkSurface).clickable { alExpandir() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground), contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(
                    model = imagenAUsar,
                    contentDescription = "Carátula",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        GeneradorGradiente(texto = cancion.titulo, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 20.sp)
                    }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = cancion.titulo, color = colorAcento, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Text(text = cancion.artista, color = DarkTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = alAlternarFavorito, modifier = Modifier.size(32.dp)) { Icon(imageVector = if (esFavorito) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Favorito", tint = if (esFavorito) colorAcento else DarkTextSecondary, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = alAnterior, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = DarkTextPrimary, modifier = Modifier.size(24.dp)) }
            IconButton(onClick = alAlternarPlay, modifier = Modifier.size(36.dp)) { Icon(if (estaReproduciendo) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play", tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }
            IconButton(onClick = alSiguiente, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = DarkTextPrimary, modifier = Modifier.size(24.dp)) }
        }

        ProgressBarMini(posicionFlow = posicionFlow, duracionTotal = duracionTotal, colorAcento = colorAcento, animacionesActivas = animacionesActivas)
    }
}

@Composable
fun ProgressBarMini(posicionFlow: StateFlow<Long>, duracionTotal: Long, colorAcento: Color, animacionesActivas: Boolean) {
    val posicion by posicionFlow.collectAsState()
    val progreso = if (duracionTotal > 0) (posicion.toFloat() / duracionTotal.toFloat()).coerceIn(0f, 1f) else 0f
    val progresoAnimado by animateFloatAsState(targetValue = progreso, animationSpec = if (animacionesActivas) tween(1000, easing = LinearEasing) else snap(), label = "progresoMini")

    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.05f))) {
        Box(modifier = Modifier.fillMaxWidth(fraction = progresoAnimado).fillMaxHeight().background(colorAcento))
    }
}
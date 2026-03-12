package com.roxxer.zenith

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(musicViewModel: MusicViewModel = viewModel()) {
    val tabActivaVM by musicViewModel.tabBibliotecaActiva.collectAsState()
    var tabSeleccionada by remember { mutableIntStateOf(tabActivaVM) }

    LaunchedEffect(tabActivaVM) { tabSeleccionada = tabActivaVM }

    val titulosTabs = listOf("Carpetas", "Canciones", "Artistas", "Álbumes")

    val misPlaylists by musicViewModel.misPlaylists.collectAsState()
    var cancionParaAñadir by remember { mutableStateOf<AudioModel?>(null) }

    val cancionActual by musicViewModel.cancionActual.collectAsState()
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()
    val nombresCustom by musicViewModel.nombresCustom.collectAsState()

    val acentoHex by musicViewModel.colorAcento.collectAsState()
    val colorDinamico = remember(acentoHex) { try { Color(android.graphics.Color.parseColor(acentoHex)) } catch (_: Exception) { Color(0xFF1DB954) } }
    val multiplicadorPortada by musicViewModel.tamanoPortadas.collectAsState()
    val animacionesActivas by musicViewModel.animacionesActivas.collectAsState()

    if (cancionParaAñadir != null) {
        ModalBottomSheet(onDismissRequest = { cancionParaAñadir = null }, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Añadir a Playlist", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                if (misPlaylists.isEmpty()) { Text("Aún no has creado ninguna playlist.", color = DarkTextSecondary, modifier = Modifier.padding(16.dp)) } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(misPlaylists, key = { it.idPlaylist }) { playlist ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { musicViewModel.agregarAPlaylist(playlist.idPlaylist, cancionParaAñadir!!.uri.toString(), playlist.nombre); cancionParaAñadir = null }.padding(vertical = 16.dp, horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = colorDinamico, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(playlist.nombre, color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Text("Tu Biblioteca", color = DarkTextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

        ScrollableTabRow(selectedTabIndex = tabSeleccionada, containerColor = DarkBackground, contentColor = colorDinamico, edgePadding = 24.dp, divider = {}, indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]), color = colorDinamico, height = 3.dp) }) {
            titulosTabs.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index; musicViewModel.explorarTabBiblioteca(index) },
                    text = { Text(titulo, color = if (tabSeleccionada == index) DarkTextPrimary else DarkTextSecondary, fontWeight = if (tabSeleccionada == index) FontWeight.Bold else FontWeight.Medium, fontSize = 16.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (tabSeleccionada) {
                0 -> VistaCarpetas(musicViewModel, cancionActual, estaReproduciendo, colorDinamico, multiplicadorPortada, animacionesActivas, portadasCustom, nombresCustom, onAñadirClick = { cancionParaAñadir = it })
                1 -> VistaCanciones(musicViewModel, cancionActual, estaReproduciendo, colorDinamico, multiplicadorPortada, animacionesActivas, portadasCustom, nombresCustom, onAñadirClick = { cancionParaAñadir = it })
                2 -> VistaAgrupada(musicViewModel, porArtista = true, colorDinamico, multiplicadorPortada, cancionActual, estaReproduciendo, animacionesActivas, portadasCustom, nombresCustom, onAñadirClick = { cancionParaAñadir = it })
                3 -> VistaAgrupada(musicViewModel, porArtista = false, colorDinamico, multiplicadorPortada, cancionActual, estaReproduciendo, animacionesActivas, portadasCustom, nombresCustom, onAñadirClick = { cancionParaAñadir = it })
            }
        }
    }
}

// --- NUEVO: Menú Premium Reutilizable para Renombrar/Eliminar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuOpcionesPremium(
    tituloOriginal: String, tituloActual: String, llaveDataStore: String, esCarpeta: Boolean, cantidadCanciones: Int = 0,
    alCerrar: () -> Unit, musicViewModel: MusicViewModel, onEliminarFisico: (() -> Unit)? = null
) {
    var mostrarRenombrar by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf(tituloActual) }
    val colorAcento = musicViewModel.colorAcento.collectAsState().value.toColor()

    if (mostrarRenombrar) {
        AlertDialog(
            onDismissRequest = { mostrarRenombrar = false }, containerColor = DarkSurface,
            title = { Text("Renombrar", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nuevoNombre, onValueChange = { nuevoNombre = it },
                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, focusedIndicatorColor = colorAcento, cursorColor = colorAcento),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = { TextButton(onClick = { musicViewModel.guardarNombreCustom(llaveDataStore, nuevoNombre); mostrarRenombrar = false; alCerrar() }) { Text("Guardar", color = colorAcento, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { mostrarRenombrar = false }) { Text("Cancelar", color = DarkTextSecondary) } }
        )
    }

    ModalBottomSheet(onDismissRequest = alCerrar, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(tituloActual, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
            if (tituloActual != tituloOriginal) Text("Original: $tituloOriginal", color = DarkTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBackground))

            Row(modifier = Modifier.fillMaxWidth().clickable { mostrarRenombrar = true }.padding(vertical = 16.dp, horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Edit, null, tint = DarkTextPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Renombrar (Alias Local)", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            if (onEliminarFisico != null) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onEliminarFisico(); alCerrar() }.padding(vertical = 16.dp, horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteForever, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(if (esCarpeta) "Eliminar Carpeta del Teléfono" else "Eliminar del Teléfono", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (esCarpeta) Text("Borrará $cantidadCanciones archivos físicamente", color = Color(0xFFFF5252).copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun VistaCarpetas(musicViewModel: MusicViewModel, cancionActual: AudioModel?, estaReproduciendo: Boolean, colorAcento: Color, tamañoPortadas: Float, animaciones: Boolean, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, onAñadirClick: (AudioModel) -> Unit) {
    val cancionesAgrupadas by musicViewModel.cancionesAgrupadasPorCarpeta.collectAsState()
    val carpetaAExplorar by musicViewModel.carpetaAExplorar.collectAsState()
    val gatilloDeScroll by musicViewModel.scrollTrigger.collectAsState()
    var carpetaSeleccionada by remember { mutableStateOf<String?>(null) }

    var carpetaOpcionesActiva by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(carpetaAExplorar) {
        if (carpetaAExplorar != null) { carpetaSeleccionada = carpetaAExplorar; musicViewModel.explorarCarpeta(null) }
    }

    if (carpetaOpcionesActiva != null) {
        val llaveAlias = "alias_folder_${carpetaOpcionesActiva!!}"
        val nombreActual = nombresCustom[llaveAlias] ?: carpetaOpcionesActiva!!
        val cancionesDeLaCarpeta = cancionesAgrupadas[carpetaOpcionesActiva] ?: emptyList()

        MenuOpcionesPremium(
            tituloOriginal = carpetaOpcionesActiva!!, tituloActual = nombreActual, llaveDataStore = llaveAlias, esCarpeta = true, cantidadCanciones = cancionesDeLaCarpeta.size, alCerrar = { carpetaOpcionesActiva = null }, musicViewModel = musicViewModel,
            onEliminarFisico = {
                val urisABorrar = cancionesDeLaCarpeta.map { it.uri }
                musicViewModel.borrarCancionesHeredado(urisABorrar)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (carpetaSeleccionada != null) {
            BackHandler { carpetaSeleccionada = null }
            val tituloCarpeta = nombresCustom["alias_folder_$carpetaSeleccionada"] ?: carpetaSeleccionada!!

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonInteractivo(alHacerClic = { carpetaSeleccionada = null }, colorAcento = colorAcento, animacionesActivas = animaciones) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = DarkTextPrimary, modifier = Modifier.size(28.dp).padding(end = 8.dp)) }
                Text(tituloCarpeta, color = DarkTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            val canciones = cancionesAgrupadas[carpetaSeleccionada] ?: emptyList()
            val listState = rememberLazyListState()

            LaunchedEffect(carpetaSeleccionada, gatilloDeScroll) {
                val index = canciones.indexOfFirst { it.id == cancionActual?.id }
                if (index != -1) {
                    delay(if (animaciones) 300 else 10)
                    val targetIndex = maxOf(0, index - 3)
                    if (animaciones) {
                        val currentVisible = listState.firstVisibleItemIndex
                        val distancia = abs(currentVisible - targetIndex)
                        if (distancia > 20) { val saltoPrevio = if (currentVisible < targetIndex) maxOf(0, targetIndex - 10) else targetIndex + 10; listState.scrollToItem(saltoPrevio) }
                        listState.animateScrollToItem(targetIndex)
                    } else { listState.scrollToItem(targetIndex) }
                }
            }

            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 120.dp)) {
                items(canciones, key = { it.id }) { cancion ->
                    ItemCancion(cancion = cancion, esCancionActual = cancion.id == cancionActual?.id, estaReproduciendo = estaReproduciendo, colorAcento = colorAcento, tamañoPortadas = tamañoPortadas, portadasCustom = portadasCustom, nombresCustom = nombresCustom, onAñadirClick = { onAñadirClick(cancion) }, alMantener = { /* Aquí podríamos añadir opciones para canción individual */ }) { musicViewModel.reproducirCancion(cancion, canciones, tipoContexto = "CARPETA", valorContexto = carpetaSeleccionada ?: cancion.carpeta) }
                }
            }
        } else {
            val nombresDeCarpetas = cancionesAgrupadas.keys.toList().sorted()
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(nombresDeCarpetas, key = { it }) { nombreCarpeta ->
                    val cantidad = cancionesAgrupadas[nombreCarpeta]?.size ?: 0
                    val tituloAMostrar = nombresCustom["alias_folder_$nombreCarpeta"] ?: nombreCarpeta
                    ItemCarpeta(tituloOriginal = nombreCarpeta, tituloMostrado = tituloAMostrar, cantidad = cantidad, colorAcento = colorAcento, tamañoPortadas = tamañoPortadas, portadasCustom = portadasCustom, alMantener = { carpetaOpcionesActiva = nombreCarpeta }) { carpetaSeleccionada = nombreCarpeta }
                }
            }
        }
    }
}

@Composable
fun VistaCanciones(musicViewModel: MusicViewModel, cancionActual: AudioModel?, estaReproduciendo: Boolean, colorAcento: Color, tamañoPortadas: Float, animaciones: Boolean, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, onAñadirClick: (AudioModel) -> Unit) {
    val listaDeCanciones by musicViewModel.canciones.collectAsState()
    val gatilloDeScroll by musicViewModel.scrollTrigger.collectAsState()
    val listState = rememberLazyListState()

    var cancionOpcionesActiva by remember { mutableStateOf<AudioModel?>(null) }

    if (cancionOpcionesActiva != null) {
        val c = cancionOpcionesActiva!!
        val llaveAlias = "alias_song_${c.id}"
        val nombreActual = nombresCustom[llaveAlias] ?: c.titulo
        MenuOpcionesPremium(
            tituloOriginal = c.titulo, tituloActual = nombreActual, llaveDataStore = llaveAlias, esCarpeta = false, alCerrar = { cancionOpcionesActiva = null }, musicViewModel = musicViewModel,
            onEliminarFisico = { musicViewModel.borrarCancionesHeredado(listOf(c.uri)) }
        )
    }

    LaunchedEffect(gatilloDeScroll) {
        val index = listaDeCanciones.indexOfFirst { it.id == cancionActual?.id }
        if (index != -1) {
            delay(if (animaciones) 300 else 10)
            val targetIndex = maxOf(0, index - 3)
            if (animaciones) {
                val currentVisible = listState.firstVisibleItemIndex
                val distancia = abs(currentVisible - targetIndex)
                if (distancia > 20) { val saltoPrevio = if (currentVisible < targetIndex) maxOf(0, targetIndex - 10) else targetIndex + 10; listState.scrollToItem(saltoPrevio) }
                listState.animateScrollToItem(targetIndex)
            } else { listState.scrollToItem(targetIndex) }
        }
    }

    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 120.dp)) {
        items(listaDeCanciones, key = { it.id }) { cancion ->
            ItemCancion(cancion = cancion, esCancionActual = cancion.id == cancionActual?.id, estaReproduciendo = estaReproduciendo, colorAcento = colorAcento, tamañoPortadas = tamañoPortadas, portadasCustom = portadasCustom, nombresCustom = nombresCustom, onAñadirClick = { onAñadirClick(cancion) }, alMantener = { cancionOpcionesActiva = cancion }) {
                musicViewModel.reproducirCancion(cancion, listaDeCanciones, tipoContexto = "CANCIONES")
            }
        }
    }
}

@Composable
fun VistaAgrupada(
    musicViewModel: MusicViewModel, porArtista: Boolean, colorAcento: Color, tamañoPortadas: Float, cancionActual: AudioModel?, estaReproduciendo: Boolean, animaciones: Boolean, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, onAñadirClick: (AudioModel) -> Unit
) {
    val agrupacionArtista by musicViewModel.cancionesAgrupadasPorArtista.collectAsState()
    val agrupacionAlbum by musicViewModel.cancionesAgrupadasPorAlbum.collectAsState()
    val gatilloDeScroll by musicViewModel.scrollTrigger.collectAsState()

    val agrupacion = if (porArtista) agrupacionArtista else agrupacionAlbum
    val nombres = agrupacion.keys.toList().sorted()

    val agrupacionTarget by musicViewModel.agrupacionAExplorar.collectAsState()
    var seleccionActual by remember { mutableStateOf<String?>(null) }

    var agrupacionOpcionesActiva by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(agrupacionTarget) { if (agrupacionTarget != null) { seleccionActual = agrupacionTarget; musicViewModel.explorarAgrupacion(null) } }

    if (agrupacionOpcionesActiva != null) {
        val prefijo = if (porArtista) "artista" else "album"
        val llaveAlias = "alias_${prefijo}_${agrupacionOpcionesActiva!!}"
        val nombreActual = nombresCustom[llaveAlias] ?: agrupacionOpcionesActiva!!
        val cancionesDelGrupo = agrupacion[agrupacionOpcionesActiva] ?: emptyList()

        MenuOpcionesPremium(
            tituloOriginal = agrupacionOpcionesActiva!!, tituloActual = nombreActual, llaveDataStore = llaveAlias, esCarpeta = true, cantidadCanciones = cancionesDelGrupo.size, alCerrar = { agrupacionOpcionesActiva = null }, musicViewModel = musicViewModel,
            onEliminarFisico = {
                val urisABorrar = cancionesDelGrupo.map { it.uri }
                musicViewModel.borrarCancionesHeredado(urisABorrar)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (seleccionActual != null) {
            BackHandler { seleccionActual = null }
            val prefijo = if (porArtista) "artista" else "album"
            val tituloMostrado = nombresCustom["alias_${prefijo}_$seleccionActual"] ?: seleccionActual!!

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonInteractivo(alHacerClic = { seleccionActual = null }, colorAcento = colorAcento, animacionesActivas = animaciones) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = DarkTextPrimary, modifier = Modifier.size(28.dp).padding(end = 8.dp))
                }
                Text(tituloMostrado, color = DarkTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            val cancionesSeleccionadas = agrupacion[seleccionActual] ?: emptyList()
            val listState = rememberLazyListState()

            LaunchedEffect(seleccionActual, gatilloDeScroll) {
                val index = cancionesSeleccionadas.indexOfFirst { it.id == cancionActual?.id }
                if (index != -1) {
                    delay(if (animaciones) 300 else 10)
                    val targetIndex = maxOf(0, index - 3)
                    if (animaciones) {
                        val currentVisible = listState.firstVisibleItemIndex
                        val distancia = abs(currentVisible - targetIndex)
                        if (distancia > 20) { val saltoPrevio = if (currentVisible < targetIndex) maxOf(0, targetIndex - 10) else targetIndex + 10; listState.scrollToItem(saltoPrevio) }
                        listState.animateScrollToItem(targetIndex)
                    } else { listState.scrollToItem(targetIndex) }
                }
            }

            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 120.dp)) {
                items(cancionesSeleccionadas, key = { it.id }) { cancion ->
                    ItemCancion(cancion = cancion, esCancionActual = cancion.id == cancionActual?.id, estaReproduciendo = estaReproduciendo, colorAcento = colorAcento, tamañoPortadas = tamañoPortadas, portadasCustom = portadasCustom, nombresCustom = nombresCustom, onAñadirClick = { onAñadirClick(cancion) }, alMantener = { /* Opcional */ }) {
                        val tipoCtx = if (porArtista) "ARTISTA" else "ALBUM"
                        musicViewModel.reproducirCancion(cancion, cancionesSeleccionadas, tipoContexto = tipoCtx, valorContexto = seleccionActual ?: "")
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(nombres, key = { it }) { nombre ->
                    val cantidad = agrupacion[nombre]?.size ?: 0
                    val prefijo = if (porArtista) "artista" else "album"
                    val llaveCustom = "cover_${prefijo}_${nombre}"
                    val pathCustom = portadasCustom[llaveCustom]
                    val tituloAMostrar = nombresCustom["alias_${prefijo}_$nombre"] ?: nombre

                    Row(
                        modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { seleccionActual = nombre }, onLongPress = { agrupacionOpcionesActiva = nombre }) }.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(56.dp * tamañoPortadas).clip(if (porArtista) CircleShape else RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
                            SubcomposeAsyncImage(
                                model = pathCustom ?: "",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = { GeneradorGradiente(texto = nombre, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 24.sp) }
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tituloAMostrar, color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$cantidad canciones", color = DarkTextSecondary, fontSize = 14.sp)
                        }
                        IconButton(onClick = { agrupacionOpcionesActiva = nombre }) { Icon(Icons.Rounded.MoreVert, "Opciones", tint = DarkTextSecondary) }
                    }
                }
            }
        }
    }
}

@Composable
fun BotonInteractivo(alHacerClic: () -> Unit, modifier: Modifier = Modifier, colorAcento: Color = DarkPrimary, animacionesActivas: Boolean = true, contenido: @Composable () -> Unit) {
    var estaPresionado by remember { mutableStateOf(false) }
    val escalaAnimada by animateFloatAsState(targetValue = if (estaPresionado) 0.90f else 1f, animationSpec = if (animacionesActivas) spring() else snap(), label = "Escala")
    val brilloAnimado by animateFloatAsState(targetValue = if (estaPresionado) 25f else 0f, animationSpec = if (animacionesActivas) tween() else snap(), label = "Brillo")

    Box(modifier = modifier.graphicsLayer { scaleX = escalaAnimada; scaleY = escalaAnimada }.shadow(brilloAnimado.dp, CircleShape, ambientColor = colorAcento, spotColor = colorAcento).pointerInput(Unit) { detectTapGestures(onPress = { estaPresionado = true; tryAwaitRelease(); estaPresionado = false; alHacerClic() }) }, contentAlignment = Alignment.Center) { contenido() }
}

@Composable
fun ItemCarpeta(tituloOriginal: String, tituloMostrado: String, cantidad: Int, colorAcento: Color, tamañoPortadas: Float, portadasCustom: Map<String, String>, alMantener: () -> Unit, alHacerClic: () -> Unit) {
    val pathCustom = portadasCustom["cover_folder_${tituloOriginal}"]

    Row(modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { alHacerClic() }, onLongPress = { alMantener() }) }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp * tamañoPortadas).clip(RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = pathCustom ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { GeneradorGradiente(texto = tituloOriginal, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 24.sp) }
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tituloMostrado, color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$cantidad canciones", color = colorAcento, fontSize = 14.sp)
        }
        IconButton(onClick = alMantener) { Icon(Icons.Rounded.MoreVert, "Opciones", tint = DarkTextSecondary) }
    }
}

@Composable
fun ItemCancion(cancion: AudioModel, esCancionActual: Boolean = false, estaReproduciendo: Boolean = false, colorAcento: Color, tamañoPortadas: Float = 1f, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, onAñadirClick: (() -> Unit)? = null, alMantener: () -> Unit, alHacerClic: () -> Unit) {
    val pathCustom = portadasCustom["cover_song_${cancion.id}"]
    val imagenAUsar = pathCustom ?: cancion.albumArtUri.toString()
    val tituloAMostrar = nombresCustom["alias_song_${cancion.id}"] ?: cancion.titulo

    Row(modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { alHacerClic() }, onLongPress = { alMantener() }) }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp * tamañoPortadas).clip(RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = imagenAUsar,
                contentDescription = "Carátula",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { GeneradorGradiente(texto = cancion.titulo, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 24.sp) }
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tituloAMostrar, color = if (esCancionActual) colorAcento else DarkTextPrimary, fontSize = 16.sp, fontWeight = if (esCancionActual) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cancion.artista, color = DarkTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (esCancionActual && estaReproduciendo) { Icon(Icons.Rounded.GraphicEq, null, tint = colorAcento, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)) } else { Text(cancion.duracionStr, color = DarkTextSecondary, fontSize = 14.sp) }
        if (onAñadirClick != null) { IconButton(onClick = onAñadirClick) { Icon(Icons.Rounded.MoreVert, "Añadir a Playlist", tint = DarkTextSecondary) } }
    }
}
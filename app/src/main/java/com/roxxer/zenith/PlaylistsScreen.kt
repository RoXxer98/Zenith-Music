package com.roxxer.zenith

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(musicViewModel: MusicViewModel) {
    val canciones by musicViewModel.canciones.collectAsState()
    val favoritos by musicViewModel.listaFavoritos.collectAsState()
    val misPlaylists by musicViewModel.misPlaylists.collectAsState()
    val playlistAExplorar by musicViewModel.playlistAExplorar.collectAsState()
    val explorarFavoritos by musicViewModel.explorarFavoritos.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()

    var playlistSeleccionada by remember { mutableStateOf<PlaylistEntity?>(null) }
    var viendoFavoritos by remember { mutableStateOf(false) }

    val acentoHex by musicViewModel.colorAcento.collectAsState()
    val colorDinamico = remember(acentoHex) {
        try { Color(android.graphics.Color.parseColor(acentoHex)) } catch (_: Exception) { Color(0xFF1DB954) }
    }

    val multiplicadorPortada by musicViewModel.tamanoPortadas.collectAsState()
    val animaciones by musicViewModel.animacionesActivas.collectAsState()

    LaunchedEffect(playlistAExplorar, explorarFavoritos) {
        if (playlistAExplorar != null) { playlistSeleccionada = playlistAExplorar; viendoFavoritos = false; musicViewModel.explorarPlaylist(null) }
        else if (explorarFavoritos) { viendoFavoritos = true; playlistSeleccionada = null; musicViewModel.explorarFavoritos(false) }
    }

    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var nombreNuevaPlaylist by remember { mutableStateOf("") }

    // --- NUEVO: Estado para Renombrar Playlists ---
    var playlistARenombrar by remember { mutableStateOf<PlaylistEntity?>(null) }
    var nombreEdicionPlaylist by remember { mutableStateOf("") }

    // --- NUEVO: Estado para el Menú Premium de Opciones ---
    var menuOpcionesActivo by remember { mutableStateOf<PlaylistEntity?>(null) }

    if (mostrarDialogoCrear) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCrear = false }, containerColor = DarkSurface,
            title = { Text("Nueva Playlist", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nombreNuevaPlaylist, onValueChange = { nombreNuevaPlaylist = it }, placeholder = { Text("Ej. Rock de los 80s", color = DarkTextSecondary) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, focusedIndicatorColor = colorDinamico, cursorColor = colorDinamico),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = { TextButton(onClick = { if (nombreNuevaPlaylist.isNotBlank()) { musicViewModel.crearPlaylist(nombreNuevaPlaylist); nombreNuevaPlaylist = ""; mostrarDialogoCrear = false } }) { Text("Crear", color = colorDinamico, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { mostrarDialogoCrear = false }) { Text("Cancelar", color = DarkTextSecondary) } }
        )
    }

    if (playlistARenombrar != null) {
        AlertDialog(
            onDismissRequest = { playlistARenombrar = null }, containerColor = DarkSurface,
            title = { Text("Renombrar Playlist", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nombreEdicionPlaylist, onValueChange = { nombreEdicionPlaylist = it },
                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, focusedIndicatorColor = colorDinamico, cursorColor = colorDinamico),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = { TextButton(onClick = { if (nombreEdicionPlaylist.isNotBlank()) { musicViewModel.renombrarPlaylist(playlistARenombrar!!.idPlaylist, nombreEdicionPlaylist); playlistARenombrar = null } }) { Text("Guardar", color = colorDinamico, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { playlistARenombrar = null }) { Text("Cancelar", color = DarkTextSecondary) } }
        )
    }

    // --- NUEVO: Menú Inferior de Opciones de Playlist ---
    if (menuOpcionesActivo != null) {
        val p = menuOpcionesActivo!!
        ModalBottomSheet(onDismissRequest = { menuOpcionesActivo = null }, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(p.nombre, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBackground))

                Row(modifier = Modifier.fillMaxWidth().clickable { nombreEdicionPlaylist = p.nombre; playlistARenombrar = p; menuOpcionesActivo = null }.padding(vertical = 16.dp, horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Edit, null, tint = DarkTextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Renombrar", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth().clickable { musicViewModel.eliminarPlaylist(p.idPlaylist); menuOpcionesActivo = null }.padding(vertical = 16.dp, horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Eliminar Playlist", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (viendoFavoritos) {
            BackHandler { viendoFavoritos = false }
            val cancionesFavoritas = canciones.filter { favoritos.contains(it.uri.toString()) }
            VistaDetalleLista(titulo = "Tus Favoritos", cancionesLista = cancionesFavoritas, iconoCabecera = Icons.Rounded.Favorite, musicViewModel = musicViewModel, alRegresar = { viendoFavoritos = false }, esFavoritos = true, idPlaylistCustom = null, colorAcento = colorDinamico, tamañoPortadas = multiplicadorPortada, animaciones = animaciones, portadasCustom = portadasCustom)
        } else if (playlistSeleccionada != null) {
            BackHandler { playlistSeleccionada = null }
            val urisDeEstaPlaylist by musicViewModel.obtenerUrisDePlaylist(playlistSeleccionada!!.idPlaylist).collectAsState(initial = emptyList())
            val cancionesDePlaylist = canciones.filter { urisDeEstaPlaylist.contains(it.uri.toString()) }
            VistaDetalleLista(titulo = playlistSeleccionada!!.nombre, cancionesLista = cancionesDePlaylist, iconoCabecera = Icons.AutoMirrored.Rounded.QueueMusic, musicViewModel = musicViewModel, alRegresar = { playlistSeleccionada = null }, esFavoritos = false, idPlaylistCustom = playlistSeleccionada!!.idPlaylist, colorAcento = colorDinamico, tamañoPortadas = multiplicadorPortada, animaciones = animaciones, portadasCustom = portadasCustom)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tu Biblioteca", color = DarkTextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { mostrarDialogoCrear = true }, modifier = Modifier.size(48.dp).clip(CircleShape).background(DarkSurface)) { Icon(Icons.Rounded.Add, "Crear Playlist", tint = colorDinamico, modifier = Modifier.size(28.dp)) }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().clickable { viendoFavoritos = true }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp * multiplicadorPortada).shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = colorDinamico).background(colorDinamico, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Favorite, null, tint = DarkBackground, modifier = Modifier.size(32.dp * multiplicadorPortada)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { Text("Tus Favoritos", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("${favoritos.size} canciones", color = DarkTextSecondary, fontSize = 14.sp) }
                    }
                }

                items(misPlaylists, key = { it.idPlaylist }) { playlist ->
                    val uris by musicViewModel.obtenerUrisDePlaylist(playlist.idPlaylist).collectAsState(initial = emptyList())
                    val pathCustom = portadasCustom["cover_playlist_${playlist.idPlaylist}"]

                    Row(
                        modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { playlistSeleccionada = playlist }, onLongPress = { menuOpcionesActivo = playlist }) }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(64.dp * multiplicadorPortada).clip(RoundedCornerShape(12.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
                            if (pathCustom != null) { AsyncImage(model = pathCustom, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } else { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = DarkTextSecondary, modifier = Modifier.size(32.dp * multiplicadorPortada)) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) { Text(playlist.nombre, color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${uris.size} canciones", color = DarkTextSecondary, fontSize = 14.sp) }
                        IconButton(onClick = { menuOpcionesActivo = playlist }) { Icon(Icons.Rounded.MoreVert, "Opciones", tint = DarkTextSecondary) }
                    }
                }
            }
        }
    }
}

@Composable
fun VistaDetalleLista(titulo: String, cancionesLista: List<AudioModel>, iconoCabecera: androidx.compose.ui.graphics.vector.ImageVector, musicViewModel: MusicViewModel, alRegresar: () -> Unit, esFavoritos: Boolean, idPlaylistCustom: Int? = null, colorAcento: Color, tamañoPortadas: Float, animaciones: Boolean, portadasCustom: Map<String, String>) {
    val cancionActual by musicViewModel.cancionActual.collectAsState()
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()
    val gatilloDeScroll by musicViewModel.scrollTrigger.collectAsState()
    val nombresCustom by musicViewModel.nombresCustom.collectAsState()

    val headerImageUri = if (idPlaylistCustom != null) portadasCustom["cover_playlist_$idPlaylistCustom"] else null

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alRegresar) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
                if (headerImageUri != null) {
                    AsyncImage(model = headerImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(iconoCabecera, null, tint = colorAcento, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(titulo, color = DarkTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${cancionesLista.size} canciones", color = DarkTextSecondary, fontSize = 14.sp) }
        }

        if (cancionesLista.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) { Text("Aún no hay canciones aquí", color = DarkTextSecondary, fontSize = 16.sp) }
        } else {
            val listState = rememberLazyListState()

            LaunchedEffect(titulo, gatilloDeScroll) {
                val index = cancionesLista.indexOfFirst { it.id == cancionActual?.id }
                if (index != -1) {
                    delay(if (animaciones) 300 else 10)
                    val targetIndex = maxOf(0, index - 3)

                    if (animaciones) {
                        val currentVisible = listState.firstVisibleItemIndex
                        val distancia = abs(currentVisible - targetIndex)

                        if (distancia > 20) {
                            val saltoPrevio = if (currentVisible < targetIndex) maxOf(0, targetIndex - 10) else targetIndex + 10
                            listState.scrollToItem(saltoPrevio)
                        }
                        listState.animateScrollToItem(targetIndex)
                    } else {
                        listState.scrollToItem(targetIndex)
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
                items(cancionesLista, key = { it.id }) { cancion ->
                    val esLaCancionActual = cancion.id == cancionActual?.id

                    val pathCustomSong = portadasCustom["cover_song_${cancion.id}"]
                    val tituloAMostrar = nombresCustom["alias_song_${cancion.id}"] ?: cancion.titulo
                    val imagenAUsar = pathCustomSong ?: cancion.albumArtUri.toString()

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { val tipoCtx = if (esFavoritos) "FAVORITOS" else "PLAYLIST"; musicViewModel.reproducirCancion(cancion, cancionesLista, tipoContexto = tipoCtx, playlistId = idPlaylistCustom, playlistNombre = titulo) }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = imagenAUsar, contentDescription = "Carátula", modifier = Modifier.size(56.dp * tamañoPortadas).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop, error = painterResource(id = R.drawable.vinilo_realista), fallback = painterResource(id = R.drawable.vinilo_realista))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tituloAMostrar, color = if (esLaCancionActual) colorAcento else DarkTextPrimary, fontSize = 16.sp, fontWeight = if (esLaCancionActual) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(cancion.artista, color = DarkTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (esLaCancionActual && estaReproduciendo) { Icon(Icons.Rounded.GraphicEq, null, tint = colorAcento, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)) }
                        IconButton(onClick = { if (esFavoritos) musicViewModel.alternarFavorito(cancion.uri.toString()) else if (idPlaylistCustom != null) musicViewModel.removerDePlaylist(idPlaylistCustom, cancion.uri.toString(), titulo) }) {
                            Icon(imageVector = if (esFavoritos) Icons.Rounded.Favorite else Icons.Rounded.Close, contentDescription = "Quitar", tint = if (esFavoritos) colorAcento else DarkTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
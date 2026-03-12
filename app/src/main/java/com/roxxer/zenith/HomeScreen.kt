package com.roxxer.zenith

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs

data class CoverTarget(val tipo: String, val id: String, val tituloOriginal: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    musicViewModel: MusicViewModel,
    onIrABiblioteca: () -> Unit,
    onIrAPlaylists: () -> Unit,
    onIrAAjustes: () -> Unit
) {
    val canciones by musicViewModel.canciones.collectAsState()
    val agrupacionCarpetas by musicViewModel.cancionesAgrupadasPorCarpeta.collectAsState()
    val misPlaylists by musicViewModel.misPlaylists.collectAsState()
    val favoritos by musicViewModel.listaFavoritos.collectAsState()
    val nombreUsuario by musicViewModel.nombreUsuario.collectAsState()
    val fotoPerfilUri by musicViewModel.fotoPerfilUri.collectAsState()
    val cancionesRecientesLocales by musicViewModel.cancionesRecientes.collectAsState()

    val cancionActual by musicViewModel.cancionActual.collectAsState()
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()
    val contextoTipo by musicViewModel.contextoActualTipo.collectAsState()
    val contextoPlaylistId by musicViewModel.contextoActualPlaylistId.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()
    // --- NUEVO: Inyectamos los Nombres Personalizados ---
    val nombresCustom by musicViewModel.nombresCustom.collectAsState()

    val acentoHex by musicViewModel.colorAcento.collectAsState()

    @Suppress("UseKtx")
    val colorDinamico = remember(acentoHex) { try { Color(android.graphics.Color.parseColor(acentoHex)) } catch (_: Exception) { Color(0xFF1DB954) } }

    val multiplicadorPortada by musicViewModel.tamanoPortadas.collectAsState()
    val animaciones by musicViewModel.animacionesActivas.collectAsState()

    var portadaTarget by remember { mutableStateOf<CoverTarget?>(null) }
    var mostrarRenombrar by remember { mutableStateOf(false) }
    var nuevoNombreTemp by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && portadaTarget != null) {
            musicViewModel.guardarPortadaCustom("cover_${portadaTarget!!.tipo}_${portadaTarget!!.id}", uri)
        }
        portadaTarget = null
    }

    val carpetasPrincipales = remember(agrupacionCarpetas, cancionActual) {
        val todas = agrupacionCarpetas.keys.toList().sorted()
        val carpetaActiva = cancionActual?.carpeta
        val ordenadas = todas.toMutableList()
        if (carpetaActiva != null && ordenadas.contains(carpetaActiva)) { ordenadas.remove(carpetaActiva); ordenadas.add(0, carpetaActiva) }
        ordenadas.take(10)
    }

    val playlistsOrdenadas = remember(misPlaylists, contextoTipo, contextoPlaylistId) {
        val lista = misPlaylists.toMutableList()
        if (contextoTipo == "PLAYLIST" && contextoPlaylistId != null) {
            val indexActiva = lista.indexOfFirst { it.idPlaylist == contextoPlaylistId }
            if (indexActiva > 0) { val playlistActiva = lista.removeAt(indexActiva); lista.add(0, playlistActiva) }
        }
        lista
    }

    val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val saludoAleatorio = remember {
        when (horaActual) {
            in 6..11 -> listOf("Buenos días", "Hola", "Arriba").random()
            in 12..18 -> listOf("Buenas tardes", "Hola", "¿Qué tal?").random()
            else -> listOf("Buenas noches", "Hola", "Descansa").random()
        }
    }

    val recientesListState = rememberLazyListState()
    val playlistsListState = rememberLazyListState()
    val carpetasListState = rememberLazyListState()

    LaunchedEffect(cancionesRecientesLocales.firstOrNull()?.id, cancionActual?.id) {
        if (cancionActual != null && cancionesRecientesLocales.firstOrNull()?.id == cancionActual?.id) {
            delay(50)
            if (animaciones) recientesListState.animateScrollToItem(0) else recientesListState.scrollToItem(0)
        }
    }

    LaunchedEffect(playlistsOrdenadas.firstOrNull()?.idPlaylist, contextoPlaylistId) {
        if (contextoTipo == "PLAYLIST" && contextoPlaylistId != null && playlistsOrdenadas.firstOrNull()?.idPlaylist == contextoPlaylistId) {
            delay(50)
            if (animaciones) playlistsListState.animateScrollToItem(0) else playlistsListState.scrollToItem(0)
        }
    }

    LaunchedEffect(carpetasPrincipales.firstOrNull(), cancionActual?.carpeta) {
        if (cancionActual != null && carpetasPrincipales.firstOrNull() == cancionActual?.carpeta) {
            delay(50)
            if (animaciones) carpetasListState.animateScrollToItem(0) else carpetasListState.scrollToItem(0)
        }
    }

    // --- NUEVO: Cuadro de Diálogo para Renombrar ---
    if (mostrarRenombrar && portadaTarget != null) {
        val llaveAlias = "alias_${portadaTarget!!.tipo}_${portadaTarget!!.id}"
        AlertDialog(
            onDismissRequest = { mostrarRenombrar = false }, containerColor = DarkSurface,
            title = { Text("Renombrar", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nuevoNombreTemp, onValueChange = { nuevoNombreTemp = it },
                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, focusedIndicatorColor = colorDinamico, cursorColor = colorDinamico),
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (portadaTarget!!.tipo == "playlist") {
                        musicViewModel.renombrarPlaylist(portadaTarget!!.id.toInt(), nuevoNombreTemp)
                    } else {
                        musicViewModel.guardarNombreCustom(llaveAlias, nuevoNombreTemp)
                    }
                    mostrarRenombrar = false; portadaTarget = null
                }) { Text("Guardar", color = colorDinamico, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { mostrarRenombrar = false }) { Text("Cancelar", color = DarkTextSecondary) } }
        )
    }

    // --- Menú Inferior Actualizado ---
    if (portadaTarget != null && !mostrarRenombrar) {
        val llaveAlias = "alias_${portadaTarget!!.tipo}_${portadaTarget!!.id}"
        val tituloActual = nombresCustom[llaveAlias] ?: portadaTarget!!.tituloOriginal

        ModalBottomSheet(onDismissRequest = { portadaTarget = null }, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tituloActual, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 24.dp, end = 24.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBackground))

                Row(modifier = Modifier.fillMaxWidth().clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 16.dp, horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = colorDinamico, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Cambiar portada", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth().clickable { nuevoNombreTemp = tituloActual; mostrarRenombrar = true }.padding(vertical = 16.dp, horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Edit, null, tint = DarkTextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Renombrar (Alias)", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                val llaveBusqueda = "cover_${portadaTarget!!.tipo}_${portadaTarget!!.id}"
                if (portadasCustom.containsKey(llaveBusqueda)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { musicViewModel.eliminarPortadaCustom(llaveBusqueda); portadaTarget = null }.padding(vertical = 16.dp, horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Restablecer Portada Original", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding(), contentPadding = PaddingValues(bottom = 140.dp, top = 24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "$saludoAleatorio,", color = colorDinamico, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = nombreUsuario, color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(50.dp).shadow(8.dp, CircleShape, ambientColor = colorDinamico).clip(CircleShape).background(colorDinamico.copy(alpha = 0.2f)).clickable { onIrAAjustes() }, contentAlignment = Alignment.Center) {
                    if (fotoPerfilUri != null) AsyncImage(model = fotoPerfilUri, contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.Person, contentDescription = "Perfil", tint = colorDinamico, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PastillaRapida("Tus Favoritos", Icons.Rounded.Favorite, colorDinamico, Modifier.weight(1f)) { musicViewModel.explorarFavoritos(true); onIrAPlaylists() }
                    PastillaRapida("Toda tu música", Icons.Rounded.LibraryMusic, DarkTextSecondary, Modifier.weight(1f)) { onIrABiblioteca() }
                }
            }
        }

        if (cancionesRecientesLocales.isNotEmpty()) {
            item {
                Text("Vuelve a tu música", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp))
                LazyRow(state = recientesListState, contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(cancionesRecientesLocales, key = { it.id }) { cancion ->
                        val esActiva = cancion.id == cancionActual?.id
                        val pathCustom = portadasCustom["cover_song_${cancion.id}"]
                        val tituloAMostrar = nombresCustom["alias_song_${cancion.id}"] ?: cancion.titulo

                        CardPremiumInteractiva(
                            titulo = tituloAMostrar, subtitulo = cancion.artista, imagenUri = pathCustom ?: cancion.albumArtUri.toString(), colorAcento = colorDinamico, escala = multiplicadorPortada, esActiva = esActiva, estaReproduciendo = estaReproduciendo, animaciones = animaciones, esFormaCircular = false, esPlaylist = false,
                            alHacerClic = { musicViewModel.reproducirCancion(cancion, cancionesRecientesLocales, "CARPETA", cancion.carpeta) },
                            alMantenerPresionado = { portadaTarget = CoverTarget("song", cancion.id.toString(), cancion.titulo) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        if (playlistsOrdenadas.isNotEmpty() || favoritos.isNotEmpty()) {
            item {
                Text("Hecho para ti", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp))
                LazyRow(state = playlistsListState, contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(playlistsOrdenadas, key = { it.idPlaylist }) { playlist ->
                        val esActiva = contextoTipo == "PLAYLIST" && contextoPlaylistId == playlist.idPlaylist
                        val pathCustom = portadasCustom["cover_playlist_${playlist.idPlaylist}"]

                        CardPremiumInteractiva(
                            titulo = playlist.nombre, subtitulo = "Playlist", imagenUri = pathCustom, colorAcento = colorDinamico, escala = multiplicadorPortada, esActiva = esActiva, estaReproduciendo = estaReproduciendo, animaciones = animaciones, esFormaCircular = false, esPlaylist = true,
                            alHacerClic = { musicViewModel.explorarPlaylist(playlist); onIrAPlaylists() },
                            alMantenerPresionado = { portadaTarget = CoverTarget("playlist", playlist.idPlaylist.toString(), playlist.nombre) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        if (carpetasPrincipales.isNotEmpty()) {
            item {
                Text("Explorar tus carpetas", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp))
                LazyRow(state = carpetasListState, contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(carpetasPrincipales, key = { it }) { carpeta ->
                        val esActiva = carpeta == cancionActual?.carpeta
                        val pathCustom = portadasCustom["cover_folder_${carpeta}"]
                        val tituloAMostrar = nombresCustom["alias_folder_${carpeta}"] ?: carpeta

                        CardPremiumInteractiva(
                            titulo = tituloAMostrar, subtitulo = "", imagenUri = pathCustom, colorAcento = colorDinamico, escala = multiplicadorPortada, esActiva = esActiva, estaReproduciendo = estaReproduciendo, animaciones = animaciones, esFormaCircular = true, esPlaylist = false,
                            alHacerClic = { musicViewModel.explorarCarpeta(carpeta); onIrABiblioteca() },
                            alMantenerPresionado = { portadaTarget = CoverTarget("folder", carpeta, carpeta) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardPremiumInteractiva(
    titulo: String, subtitulo: String, imagenUri: String?, colorAcento: Color, escala: Float, esActiva: Boolean, estaReproduciendo: Boolean, animaciones: Boolean, esFormaCircular: Boolean, esPlaylist: Boolean,
    alHacerClic: () -> Unit, alMantenerPresionado: () -> Unit
) {
    var estaPresionado by remember { mutableStateOf(false) }
    val escalaAnimada by animateFloatAsState(targetValue = if (estaPresionado) 0.92f else 1f, animationSpec = if (animaciones) spring() else snap(), label = "EscalaTarjeta")

    val forma = if (esFormaCircular) CircleShape else RoundedCornerShape(12.dp)
    val borde = if (esActiva) Modifier.border(if (esFormaCircular) 3.dp else 2.dp, colorAcento, forma) else Modifier
    val anchoTarjeta = if (esFormaCircular) 110.dp else if (esPlaylist) 150.dp else 140.dp

    Column(
        modifier = Modifier
            .width(anchoTarjeta * escala)
            .graphicsLayer { scaleX = escalaAnimada; scaleY = escalaAnimada }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { estaPresionado = true; tryAwaitRelease(); estaPresionado = false },
                    onTap = { alHacerClic() },
                    onLongPress = { alMantenerPresionado() }
                )
            },
        horizontalAlignment = if (esFormaCircular) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(modifier = Modifier.size(anchoTarjeta * escala).shadow(if (esActiva) 16.dp else 8.dp, forma, ambientColor = if (esActiva) colorAcento else Color.Black).then(borde).clip(forma)) {

            SubcomposeAsyncImage(
                model = imagenUri ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    GeneradorGradiente(
                        texto = titulo,
                        modifier = Modifier.fillMaxSize(),
                        mostrarLetra = esFormaCircular || esPlaylist,
                        fontSize = if (esFormaCircular) 32.sp else 48.sp
                    )
                }
            )

            if (esActiva && estaReproduciendo) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Icon(if (esPlaylist) Icons.AutoMirrored.Rounded.QueueMusic else Icons.Rounded.GraphicEq, null, tint = colorAcento, modifier = Modifier.size(40.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = titulo, color = if (esActiva) colorAcento else DarkTextPrimary, fontSize = if (esFormaCircular) 14.sp else 15.sp, fontWeight = if (esActiva) FontWeight.Bold else FontWeight.SemiBold, maxLines = if (esFormaCircular) 2 else 1, overflow = TextOverflow.Ellipsis, textAlign = if (esFormaCircular) TextAlign.Center else TextAlign.Start)
        if (subtitulo.isNotEmpty()) Text(text = subtitulo, color = DarkTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GeneradorGradiente(texto: String, modifier: Modifier = Modifier, mostrarLetra: Boolean = true, fontSize: TextUnit = 32.sp) {
    val brush = remember(texto) {
        val hash = texto.hashCode()
        val color1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(abs(hash % 360f), 0.8f, 0.7f)))
        val color2 = Color(android.graphics.Color.HSVToColor(floatArrayOf(abs((hash * 31) % 360f), 0.9f, 0.5f)))
        Brush.linearGradient(listOf(color1, color2))
    }
    Box(modifier = modifier.background(brush), contentAlignment = Alignment.Center) {
        if (mostrarLetra && texto.isNotBlank()) Text(texto.take(1).uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PastillaRapida(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector, colorIcono: Color, modifier: Modifier = Modifier, alHacerClic: () -> Unit) {
    Row(modifier = modifier.height(56.dp).clip(RoundedCornerShape(8.dp)).background(DarkSurface).clickable { alHacerClic() }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) { Icon(icono, null, tint = colorIcono, modifier = Modifier.size(28.dp)) }
        Text(titulo, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
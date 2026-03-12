package com.roxxer.zenith

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage
import com.roxxer.zenith.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    cancion: AudioModel, alCerrar: () -> Unit, alIrAlContexto: () -> Unit, musicViewModel: MusicViewModel = viewModel()
) {
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()
    val duracionTotal by musicViewModel.duracionTotal.collectAsState()
    val favoritos by musicViewModel.listaFavoritos.collectAsState()
    val esFavorito = favoritos.contains(cancion.uri.toString())
    val shuffleActivado by musicViewModel.shuffleActivado.collectAsState()
    val modoRepeat by musicViewModel.modoRepeat.collectAsState()
    val tiempoTemporizador by musicViewModel.tiempoRestanteTimer.collectAsState()
    val misPlaylists by musicViewModel.misPlaylists.collectAsState()
    val playlistsDeEstaCancion by musicViewModel.obtenerPlaylistsDeCancion(cancion.uri.toString()).collectAsState(initial = emptyList())
    val estaEnAlgunaPlaylist = playlistsDeEstaCancion.isNotEmpty()

    val gestosActivos by musicViewModel.gestosCaratulaActivos.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()
    val pathCustom = portadasCustom["cover_song_${cancion.id}"]
    val imagenAUsar = pathCustom ?: cancion.albumArtUri.toString()

    val acentoHex by musicViewModel.colorAcento.collectAsState()
    val colorDinamico = remember(acentoHex) { try { Color(android.graphics.Color.parseColor(acentoHex)) } catch (_: Exception) { Color(0xFF1DB954) } }
    val animaciones by musicViewModel.animacionesActivas.collectAsState()

    var mostrarMenuTemporizador by remember { mutableStateOf(false) }
    var mostrarMenuPlaylists by remember { mutableStateOf(false) }

    fun formatTime(ms: Long): String { val totalSeconds = ms / 1000; return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60) }

    // --- MAGIA: Atrapamos el gesto "Atrás" de Android para solo minimizar el reproductor ---
    BackHandler {
        alCerrar()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BotonInteractivo(alHacerClic = { mostrarMenuTemporizador = true }, colorAcento = colorDinamico, animacionesActivas = animaciones) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (tiempoTemporizador != null) colorDinamico.copy(alpha = 0.15f) else Color.Transparent).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (tiempoTemporizador != null) Icons.Rounded.Timer else Icons.Rounded.Bedtime, contentDescription = "Temporizador", tint = if (tiempoTemporizador != null) colorDinamico else DarkTextSecondary, modifier = Modifier.size(28.dp))
                        if (tiempoTemporizador != null && tiempoTemporizador!! > 0L) { Spacer(modifier = Modifier.width(8.dp)); Text(text = formatTime(tiempoTemporizador!!), color = colorDinamico, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                BotonInteractivo(alHacerClic = alCerrar, colorAcento = colorDinamico, animacionesActivas = animaciones) { Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar", tint = DarkTextPrimary, modifier = Modifier.size(44.dp).padding(8.dp)) }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            val coroutineScope = rememberCoroutineScope()
            val offsetX = remember { Animatable(0f) }
            var isPressed by remember { mutableStateOf(false) }
            val animatedScale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, animationSpec = if (animaciones) spring() else snap(), label = "EfectoBoton")

            var anchuraCaratula by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; translationX = offsetX.value }
                    .shadow(30.dp, RoundedCornerShape(24.dp), ambientColor = colorDinamico)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .onSizeChanged { anchuraCaratula = it.width.toFloat() }
                    .pointerInput(gestosActivos) {
                        detectTapGestures(
                            onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                            onTap = { musicViewModel.alternarPlayPause() },
                            onDoubleTap = { offset ->
                                if (gestosActivos) {
                                    val saltoMs = 10000L
                                    val currentPos = musicViewModel.posicionActual.value
                                    if (offset.x < anchuraCaratula / 2) {
                                        val nuevaPos = (currentPos - saltoMs).coerceAtLeast(0L)
                                        musicViewModel.adelantarA(nuevaPos.toFloat()); musicViewModel.mostrarMensaje("⏪ -10s")
                                    } else {
                                        val nuevaPos = (currentPos + saltoMs).coerceAtMost(duracionTotal)
                                        musicViewModel.adelantarA(nuevaPos.toFloat()); musicViewModel.mostrarMensaje("+10s ⏩")
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                isPressed = false
                                coroutineScope.launch {
                                    if (offsetX.value > 100f) musicViewModel.anteriorCancion() else if (offsetX.value < -100f) musicViewModel.siguienteCancion()
                                    if (animaciones) offsetX.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) else offsetX.snapTo(0f)
                                }
                            },
                            onDragCancel = { isPressed = false; coroutineScope.launch { if (animaciones) offsetX.animateTo(0f) else offsetX.snapTo(0f) } },
                            onHorizontalDrag = { change, dragAmount -> change.consume(); isPressed = true; coroutineScope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-180f, 180f)) } }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imagenAUsar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        GeneradorGradiente(
                            texto = cancion.titulo,
                            modifier = Modifier.fillMaxSize(),
                            mostrarLetra = true,
                            fontSize = 120.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = cancion.titulo, color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                Text(text = cancion.artista, color = colorDinamico, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 8.dp).basicMarquee())
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                var isListPressed by remember { mutableStateOf(false) }
                val listScale by animateFloatAsState(targetValue = if (isListPressed) 0.75f else 1f, animationSpec = if (animaciones) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else snap(), label = "escalaLista")
                val listColor by animateColorAsState(targetValue = if (isListPressed) colorDinamico else DarkTextSecondary, animationSpec = if (animaciones) tween(200) else snap(), label = "colorLista")

                Box(
                    modifier = Modifier.size(40.dp).graphicsLayer { scaleX = listScale; scaleY = listScale }.pointerInput(Unit) { detectTapGestures(onPress = { isListPressed = true; tryAwaitRelease(); isListPressed = false }, onTap = { alIrAlContexto() }) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Ir a Lista", tint = listColor, modifier = Modifier.size(28.dp)) }

                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { mostrarMenuPlaylists = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Add, null, tint = if (estaEnAlgunaPlaylist) colorDinamico else DarkTextSecondary, modifier = Modifier.size(28.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { musicViewModel.alternarFavorito(cancion.uri.toString()) }, modifier = Modifier.size(32.dp)) { Icon(imageVector = if (esFavorito) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null, tint = if (esFavorito) colorDinamico else DarkTextSecondary, modifier = Modifier.size(24.dp)) }
            }

            BarraDeProgresoAislada(musicViewModel = musicViewModel, duracionTotal = duracionTotal, colorDinamico = colorDinamico, animaciones = animaciones)

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BotonInteractivo(alHacerClic = { musicViewModel.alternarShuffle() }, colorAcento = colorDinamico, animacionesActivas = animaciones) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuffleActivado) colorDinamico else DarkTextSecondary, modifier = Modifier.size(28.dp)) }
                BotonInteractivo(alHacerClic = { musicViewModel.anteriorCancion() }, colorAcento = colorDinamico, animacionesActivas = animaciones) { Icon(Icons.Rounded.SkipPrevious, null, tint = DarkTextPrimary, modifier = Modifier.size(44.dp)) }
                BotonInteractivo(alHacerClic = { musicViewModel.alternarPlayPause() }, colorAcento = colorDinamico, animacionesActivas = animaciones) {
                    Surface(modifier = Modifier.size(72.dp).shadow(16.dp, CircleShape, ambientColor = colorDinamico), shape = CircleShape, color = colorDinamico) { Box(contentAlignment = Alignment.Center) { Icon(if (estaReproduciendo) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = DarkBackground, modifier = Modifier.size(36.dp)) } }
                }
                BotonInteractivo(alHacerClic = { musicViewModel.siguienteCancion() }, colorAcento = colorDinamico, animacionesActivas = animaciones) { Icon(Icons.Rounded.SkipNext, null, tint = DarkTextPrimary, modifier = Modifier.size(44.dp)) }
                BotonInteractivo(alHacerClic = { musicViewModel.alternarRepeat() }, colorAcento = colorDinamico, animacionesActivas = animaciones) {
                    val iconoRepeat = if (modoRepeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                    Icon(iconoRepeat, null, tint = if (modoRepeat != Player.REPEAT_MODE_OFF) colorDinamico else DarkTextSecondary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }

        if (mostrarMenuPlaylists) {
            ModalBottomSheet(onDismissRequest = { mostrarMenuPlaylists = false }, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Añadir a Playlist", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    if (misPlaylists.isEmpty()) { Text("Aún no tienes playlists.", color = DarkTextSecondary, modifier = Modifier.padding(16.dp)) } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(misPlaylists) { playlist ->
                                val yaEstaEnLista = playlistsDeEstaCancion.contains(playlist.idPlaylist)
                                Row(modifier = Modifier.fillMaxWidth().clickable { if (yaEstaEnLista) musicViewModel.removerDePlaylist(playlist.idPlaylist, cancion.uri.toString(), playlist.nombre) else musicViewModel.agregarAPlaylist(playlist.idPlaylist, cancion.uri.toString(), playlist.nombre); mostrarMenuPlaylists = false }.padding(vertical = 16.dp, horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = if (yaEstaEnLista) colorDinamico else DarkTextSecondary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = playlist.nombre, color = if (yaEstaEnLista) colorDinamico else DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mostrarMenuTemporizador) {
            ModalBottomSheet(onDismissRequest = { mostrarMenuTemporizador = false }, containerColor = DarkSurface, contentColor = DarkTextPrimary) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                        Icon(Icons.Rounded.Bedtime, null, tint = colorDinamico, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Temporizador de Sueño", color = DarkTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Detener la música en:", color = DarkTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))

                    val opciones = listOf(5, 10, 15, 30, 45, 60)
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        items(opciones) { mins ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DarkBackground).clickable { musicViewModel.iniciarTemporizador(mins); mostrarMenuTemporizador = false }.padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(text = "$mins min", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkBackground).clickable { musicViewModel.iniciarTemporizadorFinDeCancion(); mostrarMenuTemporizador = false }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, null, tint = colorDinamico, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Al terminar la canción", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(cancion.titulo, color = DarkTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (tiempoTemporizador != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp)).clickable { musicViewModel.cancelarTemporizador(); mostrarMenuTemporizador = false }.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Desactivar Temporizador", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraDeProgresoAislada(musicViewModel: MusicViewModel, duracionTotal: Long, colorDinamico: Color, animaciones: Boolean) {
    val posicion by musicViewModel.posicionActual.collectAsState()
    var posicionDeslizamiento by remember { mutableStateOf<Float?>(null) }

    val posicionAnimada by animateFloatAsState(targetValue = posicion.toFloat(), animationSpec = if (animaciones) tween(1000, easing = LinearEasing) else snap(), label = "ProgresoSuave")
    val valorSlider = posicionDeslizamiento ?: posicionAnimada

    fun formatTime(ms: Long): String { val totalSeconds = ms / 1000; return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60) }

    val range = 0f..duracionTotal.toFloat().coerceAtLeast(1f)
    val tamanoThumb by animateDpAsState(targetValue = if (posicionDeslizamiento != null) 22.dp else 16.dp, animationSpec = if (animaciones) spring(stiffness = Spring.StiffnessLow) else snap(), label = "TamanoThumb")

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = valorSlider,
            onValueChange = { posicionDeslizamiento = it },
            onValueChangeFinished = { posicionDeslizamiento?.let { musicViewModel.adelantarA(it) }; posicionDeslizamiento = null },
            valueRange = range, modifier = Modifier.fillMaxWidth(),
            thumb = { Box(modifier = Modifier.size(tamanoThumb).shadow(12.dp, CircleShape, ambientColor = colorDinamico, spotColor = colorDinamico).clip(CircleShape).background(colorDinamico)) },
            track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(6.dp).clip(RoundedCornerShape(3.dp)), colors = SliderDefaults.colors(activeTrackColor = colorDinamico, inactiveTrackColor = DarkSurface), drawStopIndicator = null) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(valorSlider.toLong()), color = DarkTextSecondary, fontSize = 13.sp)
            Text(formatTime(duracionTotal), color = DarkTextSecondary, fontSize = 13.sp)
        }
    }
}
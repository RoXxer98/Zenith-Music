package com.roxxer.zenith

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Suppress("SpellCheckingInspection")
fun String.distanciaFuzzy(query: String): Boolean {
    val textoLimpio = this.lowercase()
    val queryLimpia = query.lowercase().trim()
    if (textoLimpio.contains(queryLimpia)) return true

    if (queryLimpia.length <= 3) return false

    val palabras = textoLimpio.split(" ", "-", "_")
    for (palabra in palabras) {
        if (palabra.length >= queryLimpia.length - 1) {
            var costo = IntArray(palabra.length + 1) { it }
            var nuevoCosto = IntArray(palabra.length + 1)
            for (i in 1..queryLimpia.length) {
                nuevoCosto[0] = i
                for (j in 1..palabra.length) {
                    val match = if (palabra[j - 1] == queryLimpia[i - 1]) 0 else 1
                    val costoReemplazo = costo[j - 1] + match
                    val costoInserto = costo[j] + 1
                    val costoBorro = nuevoCosto[j - 1] + 1
                    nuevoCosto[j] = minOf(costoInserto, costoBorro, costoReemplazo)
                }
                val temp = costo
                costo = nuevoCosto
                nuevoCosto = temp
            }
            if (costo[palabra.length] <= 2) return true
        }
    }
    return false
}

@Composable
fun SearchScreen(
    musicViewModel: MusicViewModel,
    onIrABiblioteca: () -> Unit,
    onIrAPlaylists: () -> Unit
) {
    val canciones by musicViewModel.canciones.collectAsState()
    val playlists by musicViewModel.misPlaylists.collectAsState()
    val busquedasRecientes by musicViewModel.busquedasRecientes.collectAsState()
    val sugerenciasRecientes by musicViewModel.cancionesRecientes.collectAsState()

    val cancionSonando by musicViewModel.cancionActual.collectAsState()
    val estaReproduciendo by musicViewModel.estaReproduciendo.collectAsState()
    val favoritos by musicViewModel.listaFavoritos.collectAsState()

    val portadasCustom by musicViewModel.portadasCustom.collectAsState()
    // --- NUEVO: Nombres Customizados ---
    val nombresCustom by musicViewModel.nombresCustom.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }

    val acentoHex by musicViewModel.colorAcento.collectAsState()

    @Suppress("UseKtx")
    val colorDinamico = remember(acentoHex) { try { Color(android.graphics.Color.parseColor(acentoHex)) } catch (_: Exception) { Color(0xFF1DB954) } }
    val multiplicadorPortada by musicViewModel.tamanoPortadas.collectAsState()

    var resultadosCanciones by remember { mutableStateOf<List<AudioModel>>(emptyList()) }
    var resultadosArtistas by remember { mutableStateOf<List<String>>(emptyList()) }
    var resultadosCarpetas by remember { mutableStateOf<List<String>>(emptyList()) }
    var resultadosPlaylists by remember { mutableStateOf<List<PlaylistEntity>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }

    // --- NUEVO: Estado para el menú de Opciones ---
    var menuTarget by remember { mutableStateOf<CoverTarget?>(null) }

    LaunchedEffect(textoBusqueda, canciones) {
        if (textoBusqueda.isBlank()) {
            resultadosCanciones = emptyList(); resultadosArtistas = emptyList(); resultadosCarpetas = emptyList(); resultadosPlaylists = emptyList()
            return@LaunchedEffect
        }

        buscando = true
        delay(300)

        withContext(Dispatchers.Default) {
            val query = textoBusqueda
            val filtradas = canciones.filter { it.titulo.distanciaFuzzy(query) || it.artista.distanciaFuzzy(query) || (nombresCustom["alias_song_${it.id}"]?.distanciaFuzzy(query) == true) }
            resultadosCanciones = filtradas.sortedBy { if (it.titulo.lowercase().startsWith(query.lowercase())) 0 else 1 }.take(15)
            val artistas = canciones.map { it.artista }.distinct()
            resultadosArtistas = artistas.filter { it.distanciaFuzzy(query) || (nombresCustom["alias_artista_$it"]?.distanciaFuzzy(query) == true) }.take(5)
            val carpetas = canciones.map { it.carpeta }.distinct()
            resultadosCarpetas = carpetas.filter { it.distanciaFuzzy(query) || (nombresCustom["alias_folder_$it"]?.distanciaFuzzy(query) == true) }.take(5)
            resultadosPlaylists = playlists.filter { it.nombre.distanciaFuzzy(query) }
        }
        buscando = false
    }

    if (menuTarget != null) {
        val tipo = menuTarget!!.tipo
        val llaveAlias = if (tipo == "playlist") "" else "alias_${tipo}_${menuTarget!!.id}"
        val tituloActual = if (tipo == "playlist") menuTarget!!.tituloOriginal else (nombresCustom[llaveAlias] ?: menuTarget!!.tituloOriginal)

        MenuOpcionesPremium(
            tituloOriginal = menuTarget!!.tituloOriginal, tituloActual = tituloActual, llaveDataStore = llaveAlias, esCarpeta = (tipo == "folder" || tipo == "artista" || tipo == "album"),
            alCerrar = { menuTarget = null }, musicViewModel = musicViewModel,
            onEliminarFisico = if (tipo == "song") { { musicViewModel.borrarCancionesHeredado(listOf(canciones.first { it.id.toString() == menuTarget!!.id }.uri)) } } else null
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text(text = "Buscar música", color = DarkTextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            TextField(
                value = textoBusqueda, onValueChange = { textoBusqueda = it }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Canciones, artistas, álbumes...", color = DarkTextSecondary) },
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, cursorColor = colorDinamico, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = DarkTextSecondary) },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { textoBusqueda = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Borrar", tint = DarkTextPrimary) }
                    }
                }
            )
        }

        if (textoBusqueda.isBlank()) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                if (busquedasRecientes.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Búsquedas recientes", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Borrar", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { musicViewModel.limpiarHistorialBusqueda() }.padding(4.dp))
                        }
                        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                            items(busquedasRecientes, key = { it }) { query ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(DarkSurface).clickable { textoBusqueda = query }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(query, color = DarkTextPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (sugerenciasRecientes.isNotEmpty()) {
                    item {
                        Text("Sugerencias para ti", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                        Column {
                            sugerenciasRecientes.take(5).forEach { cancion ->
                                val esActiva = cancion.id == cancionSonando?.id
                                val esFav = favoritos.contains(cancion.uri.toString())
                                ItemCancionBusqueda(cancion, esActiva, estaReproduciendo, colorDinamico, esFav, portadasCustom, nombresCustom, onPlay = { musicViewModel.reproducirCancion(cancion, sugerenciasRecientes, "CARPETA", cancion.carpeta) }, onFav = { musicViewModel.alternarFavorito(cancion.uri.toString()) }, alMantener = { menuTarget = CoverTarget("song", cancion.id.toString(), cancion.titulo) })
                            }
                        }
                    }
                }
            }
        } else {
            if (resultadosCanciones.isEmpty() && resultadosArtistas.isEmpty() && resultadosCarpetas.isEmpty() && resultadosPlaylists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) {
                    if (buscando) CircularProgressIndicator(color = colorDinamico)
                    else Text("No encontramos '$textoBusqueda'", color = DarkTextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {

                    if (resultadosCanciones.isNotEmpty()) {
                        item { TituloSeccion("Canciones") }
                        items(resultadosCanciones, key = { it.id }) { cancion ->
                            val esActiva = cancion.id == cancionSonando?.id
                            val esFav = favoritos.contains(cancion.uri.toString())
                            ItemCancionBusqueda(
                                cancion = cancion, esActiva = esActiva, estaReproduciendo = estaReproduciendo, colorAcento = colorDinamico, esFav = esFav, portadasCustom = portadasCustom, nombresCustom = nombresCustom,
                                onPlay = {
                                    musicViewModel.guardarBusquedaReciente(textoBusqueda)
                                    musicViewModel.reproducirCancion(cancion, resultadosCanciones, "TODAS")
                                },
                                onFav = { musicViewModel.alternarFavorito(cancion.uri.toString()) },
                                alMantener = { menuTarget = CoverTarget("song", cancion.id.toString(), cancion.titulo) }
                            )
                        }
                    }

                    if (resultadosArtistas.isNotEmpty()) {
                        item {
                            TituloSeccion("Artistas")
                            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(resultadosArtistas, key = { it }) { artista ->
                                    CardArtistaBusqueda(artista, colorDinamico, multiplicadorPortada, portadasCustom, nombresCustom, alMantener = { menuTarget = CoverTarget("artista", artista, artista) }) {
                                        musicViewModel.guardarBusquedaReciente(textoBusqueda)
                                        musicViewModel.explorarTabBiblioteca(2)
                                        musicViewModel.explorarAgrupacion(artista)
                                        onIrABiblioteca()
                                    }
                                }
                            }
                        }
                    }

                    if (resultadosCarpetas.isNotEmpty()) {
                        item {
                            TituloSeccion("Carpetas / Álbumes")
                            LazyRow(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(resultadosCarpetas, key = { it }) { carpeta ->
                                    CardCarpetaBusqueda(carpeta, colorDinamico, multiplicadorPortada, portadasCustom, nombresCustom, alMantener = { menuTarget = CoverTarget("folder", carpeta, carpeta) }) {
                                        musicViewModel.guardarBusquedaReciente(textoBusqueda)
                                        musicViewModel.explorarTabBiblioteca(0)
                                        musicViewModel.explorarCarpeta(carpeta)
                                        onIrABiblioteca()
                                    }
                                }
                            }
                        }
                    }

                    if (resultadosPlaylists.isNotEmpty()) {
                        item {
                            TituloSeccion("Playlists")
                            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(resultadosPlaylists, key = { it.idPlaylist }) { playlist ->
                                    CardPlaylistBusqueda(playlist.nombre, playlist.idPlaylist, colorDinamico, multiplicadorPortada, portadasCustom, alMantener = { menuTarget = CoverTarget("playlist", playlist.idPlaylist.toString(), playlist.nombre) }) {
                                        musicViewModel.guardarBusquedaReciente(textoBusqueda)
                                        musicViewModel.explorarPlaylist(playlist)
                                        onIrAPlaylists()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TituloSeccion(titulo: String) {
    Text(titulo, color = DarkTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
}

@Composable
fun ItemCancionBusqueda(cancion: AudioModel, esActiva: Boolean, estaReproduciendo: Boolean, colorAcento: Color, esFav: Boolean, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, onPlay: () -> Unit, onFav: () -> Unit, alMantener: () -> Unit) {
    val pathCustom = portadasCustom["cover_song_${cancion.id}"]
    val imagenAUsar = pathCustom ?: cancion.albumArtUri.toString()
    val tituloAMostrar = nombresCustom["alias_song_${cancion.id}"] ?: cancion.titulo

    Row(modifier = Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onTap = { onPlay() }, onLongPress = { alMantener() }) }.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {

            SubcomposeAsyncImage(
                model = imagenAUsar,
                contentDescription = "Carátula",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    GeneradorGradiente(texto = cancion.titulo, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 24.sp)
                }
            )

            if (esActiva && estaReproduciendo) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, tint = colorAcento, modifier = Modifier.size(24.dp)) }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tituloAMostrar, color = if (esActiva) colorAcento else DarkTextPrimary, fontSize = 16.sp, fontWeight = if (esActiva) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cancion.artista, color = DarkTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFav) {
            Icon(imageVector = if (esFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = "Fav", tint = if (esFav) colorAcento else DarkTextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun CardArtistaBusqueda(nombre: String, colorAcento: Color, escala: Float, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, alMantener: () -> Unit, onClick: () -> Unit) {
    val pathCustom = portadasCustom["cover_artista_${nombre}"]
    val tituloAMostrar = nombresCustom["alias_artista_${nombre}"] ?: nombre

    Column(modifier = Modifier.width(90.dp * escala).pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { alMantener() }) }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(90.dp * escala).clip(CircleShape).background(DarkSurface), contentAlignment = Alignment.Center) {

            SubcomposeAsyncImage(
                model = pathCustom ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { GeneradorGradiente(texto = nombre, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 32.sp) }
            )

        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(tituloAMostrar, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun CardCarpetaBusqueda(nombre: String, colorAcento: Color, escala: Float, portadasCustom: Map<String, String>, nombresCustom: Map<String, String>, alMantener: () -> Unit, onClick: () -> Unit) {
    val pathCustom = portadasCustom["cover_folder_${nombre}"]
    val tituloAMostrar = nombresCustom["alias_folder_${nombre}"] ?: nombre

    Column(modifier = Modifier.width(120.dp * escala).pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { alMantener() }) }) {
        Box(modifier = Modifier.size(120.dp * escala, 80.dp * escala).clip(RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {

            SubcomposeAsyncImage(
                model = pathCustom ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { GeneradorGradiente(texto = nombre, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 32.sp) }
            )

        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(tituloAMostrar, color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CardPlaylistBusqueda(nombre: String, idPlaylist: Int, colorAcento: Color, escala: Float, portadasCustom: Map<String, String>, alMantener: () -> Unit, onClick: () -> Unit) {
    val pathCustom = portadasCustom["cover_playlist_${idPlaylist}"]

    Column(modifier = Modifier.width(120.dp * escala).pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { alMantener() }) }) {
        Box(modifier = Modifier.size(120.dp * escala, 80.dp * escala).clip(RoundedCornerShape(8.dp)).background(if (pathCustom == null) colorAcento.copy(alpha = 0.15f) else DarkSurface), contentAlignment = Alignment.Center) {

            SubcomposeAsyncImage(
                model = pathCustom ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = { GeneradorGradiente(texto = nombre, modifier = Modifier.fillMaxSize(), mostrarLetra = true, fontSize = 32.sp) }
            )

        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(nombre, color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
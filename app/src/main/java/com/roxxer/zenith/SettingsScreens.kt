package com.roxxer.zenith

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roxxer.zenith.ui.theme.*
import kotlinx.coroutines.launch

fun String.toColor(): Color { return try { Color(android.graphics.Color.parseColor(this)) } catch (_: Exception) { DarkPrimary } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    musicViewModel: MusicViewModel,
    onVolver: () -> Unit,
    onIrAAudio: () -> Unit,
    onIrAInterfaz: () -> Unit,
    onIrAAlmacenamiento: () -> Unit,
    onIrAOtros: () -> Unit,
    onIrAPrivacidad: () -> Unit,
    onIrAAcercaDe: () -> Unit
) {
    val nombreUsuario by musicViewModel.nombreUsuario.collectAsState()
    val fotoPerfilUri by musicViewModel.fotoPerfilUri.collectAsState()

    val acentoActualHex by musicViewModel.colorAcento.collectAsState()
    val colorDinamico = acentoActualHex.toColor()

    var mostrarDialogoNombre by remember { mutableStateOf(false) }
    var nuevoNombreTemp by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { musicViewModel.guardarFotoPerfil(it) } }
    )

    if (mostrarDialogoNombre) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNombre = false }, containerColor = DarkSurface,
            title = { Text("Tu Perfil", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nuevoNombreTemp, onValueChange = { nuevoNombreTemp = it },
                    label = { Text("¿Cómo te llamamos?", color = DarkTextSecondary) }, singleLine = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground, focusedTextColor = DarkTextPrimary, unfocusedTextColor = DarkTextPrimary, focusedIndicatorColor = colorDinamico, cursorColor = colorDinamico),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { musicViewModel.guardarNombreUsuario(nuevoNombreTemp); mostrarDialogoNombre = false }) { Text("Guardar", color = colorDinamico, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { mostrarDialogoNombre = false }) { Text("Cancelar", color = DarkTextSecondary) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajustes", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(DarkSurface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(colorDinamico.copy(alpha = 0.2f)).clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (fotoPerfilUri != null) { AsyncImage(model = fotoPerfilUri, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } else { Icon(Icons.Rounded.Person, null, tint = colorDinamico, modifier = Modifier.size(36.dp)) }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f).clickable { nuevoNombreTemp = nombreUsuario; mostrarDialogoNombre = true }) {
                Text(nombreUsuario, color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Toca para editar perfil", color = DarkTextSecondary, fontSize = 14.sp)
            }
            Icon(Icons.Rounded.Edit, null, tint = DarkTextSecondary, modifier = Modifier.size(20.dp))
        }

        Text("Categorías", color = DarkTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item { ItemCategoriaAjuste(titulo = "Audio y Sonido", descripcion = "Ecualizador, Crossfade, Gapless", icono = Icons.Rounded.GraphicEq, onClick = onIrAAudio) }
            item { ItemCategoriaAjuste(titulo = "Interfaz y Diseño", descripcion = "Temas, Colores, Portadas", icono = Icons.Rounded.Palette, onClick = onIrAInterfaz) }
            item { ItemCategoriaAjuste(titulo = "Almacenamiento", descripcion = "Filtro anti-basura, Re-escaneo", icono = Icons.Rounded.Storage, onClick = onIrAAlmacenamiento) }
            item { ItemCategoriaAjuste(titulo = "Otros útiles", descripcion = "Gestos, Auto, Letras, Widgets", icono = Icons.Rounded.Extension, onClick = onIrAOtros) }
            item { ItemCategoriaAjuste(titulo = "Privacidad / Avanzado", descripcion = "Seguridad, Backups, Datos locales", icono = Icons.Rounded.Security, onClick = onIrAPrivacidad) }
            item { ItemCategoriaAjuste(titulo = "Acerca de / Sobre nosotros", descripcion = "Misión, Donaciones, Créditos", icono = Icons.Rounded.Favorite, onClick = onIrAAcercaDe) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ====================================================================
// 🔥 AUDIO SETTINGS CON BLINDAJE 🔥
// ====================================================================
@Composable
fun AudioSettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit, onIrAEcualizador: () -> Unit) {
    val gaplessActivado by musicViewModel.gaplessActivado.collectAsState()
    val normActivada by musicViewModel.normalizacionActivada.collectAsState()
    val crossfade by musicViewModel.crossfadeSegundos.collectAsState()
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Audio y Sonido", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).clip(RoundedCornerShape(16.dp)).background(colorDinamico).clickable { onIrAEcualizador() }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, null, tint = DarkBackground, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text("Ecualizador Pro", color = DarkBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Control avanzado + Bass Boost", color = DarkBackground.copy(alpha = 0.7f), fontSize = 14.sp) }
            Icon(Icons.Rounded.ChevronRight, null, tint = DarkBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ItemAjusteSwitch(
            titulo = "Reproducción Sin Pausas (Gapless)",
            descripcion = if (crossfade > 0) "Desactivado automáticamente. El DJ Crossfade está activo." else "Ignora silencios y une las canciones sin tiempos de carga.",
            activado = gaplessActivado && crossfade == 0,
            colorAcento = colorDinamico
        ) {
            musicViewModel.alternarGapless(it)
            if (it) musicViewModel.mostrarMensaje("Gapless activado (Crossfade apagado) ✨")
        }

        ItemAjusteSwitch(titulo = "Normalización de Audio", descripcion = "Iguala el volumen de todas las canciones.", activado = normActivada, colorAcento = colorDinamico) { musicViewModel.alternarNormalizacion(it); if (it) musicViewModel.mostrarMensaje("Normalización activada ✨") }

        Spacer(modifier = Modifier.height(24.dp))
        Text("DJ Crossfade Profesional", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Text("Mezcla el final de una canción con el inicio de la siguiente logarítmicamente. Anula el modo Gapless.", color = DarkTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp).padding(vertical = 4.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("0s", color = DarkTextSecondary)
            Slider(
                value = crossfade.toFloat(),
                onValueChange = { musicViewModel.cambiarCrossfade(it.toInt()) },
                onValueChangeFinished = { if (crossfade > 0) musicViewModel.mostrarMensaje("DJ Crossfade Activo (Gapless apagado) 🎛️") },
                valueRange = 0f..12f, steps = 11,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                colors = SliderDefaults.colors(thumbColor = colorDinamico, activeTrackColor = colorDinamico, inactiveTrackColor = DarkSurface)
            )
            Text("${crossfade}s", color = colorDinamico, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EqualizerScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val eqActivado by musicViewModel.eqActivado.collectAsState()
    val bandas by musicViewModel.bandasEcualizador.collectAsState()
    val bassBoostFuerza by musicViewModel.bassBoostFuerza.collectAsState()
    val presetsDisponibles by musicViewModel.presetsDisponibles.collectAsState()
    val presetActual by musicViewModel.presetActual.collectAsState()
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()
    val animaciones by musicViewModel.animacionesActivas.collectAsState()

    val listaPresetsOrdenada = remember(presetsDisponibles, presetActual) { val todos = mutableListOf("Personalizado"); todos.addAll(presetsDisponibles.filter { it != "Personalizado" }); val resultado = mutableListOf<String>(); resultado.add(presetActual); todos.forEach { if (it != presetActual) resultado.add(it) }; resultado }
    val listStatePresets = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ecualizador", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (bandas.isEmpty()) Text("Reproduce una canción para activarlo", color = Color(0xFFFF5252), fontSize = 12.sp)
            }
            Switch(checked = eqActivado, onCheckedChange = { musicViewModel.alternarInterruptorEcualizador(it) }, colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = colorDinamico, uncheckedThumbColor = DarkTextSecondary, uncheckedTrackColor = DarkSurface, uncheckedBorderColor = Color.Transparent))
        }

        if (bandas.isNotEmpty()) {
            if (eqActivado) {
                LazyRow(state = listStatePresets, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listaPresetsOrdenada, key = { it }) { preset -> ChipPreset(nombre = preset, seleccionado = presetActual == preset, colorAcento = colorDinamico) { musicViewModel.aplicarPreset(preset); coroutineScope.launch { if (animaciones) listStatePresets.animateScrollToItem(0) else listStatePresets.scrollToItem(0) } } }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Frecuencias (Hz)", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (eqActivado) { Text("Restablecer", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { musicViewModel.restablecerEcualizador(); coroutineScope.launch { if (animaciones) listStatePresets.animateScrollToItem(0) else listStatePresets.scrollToItem(0) } }.padding(horizontal = 12.dp, vertical = 6.dp)) }
            }

            LazyRow(modifier = Modifier.fillMaxWidth().height(320.dp).padding(top = 24.dp), contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(bandas, key = { it.indice }) { banda ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
                        Text("+15", color = DarkTextSecondary, fontSize = 10.sp)
                        var localNivel by remember { mutableFloatStateOf(banda.nivel.toFloat()) }
                        var isDragging by remember { mutableStateOf(false) }

                        LaunchedEffect(banda.nivel) { if (!isDragging) { localNivel = banda.nivel.toFloat() } }
                        val nivelAnimado by animateFloatAsState(targetValue = localNivel, animationSpec = if (animaciones) tween(500, easing = FastOutSlowInEasing) else snap(), label = "anim_banda_${banda.indice}")

                        Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Slider(value = if (isDragging) localNivel else nivelAnimado, onValueChange = { isDragging = true; localNivel = it; if (eqActivado) musicViewModel.cambiarNivelBanda(banda.indice, it) }, onValueChangeFinished = { isDragging = false }, valueRange = banda.minNivel.toFloat()..banda.maxNivel.toFloat(), colors = SliderDefaults.colors(thumbColor = if (eqActivado) colorDinamico else DarkTextSecondary, activeTrackColor = if (eqActivado) colorDinamico else DarkTextSecondary, inactiveTrackColor = DarkSurface), modifier = Modifier.graphicsLayer { rotationZ = -90f; transformOrigin = TransformOrigin(0.5f, 0.5f) }.requiredWidth(220.dp).requiredHeight(40.dp))
                        }

                        Text("-15", color = DarkTextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${banda.frecuenciaHz}", color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)).background(DarkSurface).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speaker, null, tint = colorDinamico, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bass Boost (Refuerzo de Graves)", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(value = bassBoostFuerza, onValueChange = { if (eqActivado) musicViewModel.cambiarFuerzaBassBoost(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = if (eqActivado) colorDinamico else DarkTextSecondary, activeTrackColor = if (eqActivado) colorDinamico else DarkTextSecondary, inactiveTrackColor = DarkBackground))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Plano", color = DarkTextSecondary, fontSize = 12.sp)
                    Text("Máximo", color = DarkTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ChipPreset(nombre: String, seleccionado: Boolean, colorAcento: Color, onClick: () -> Unit) { Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if (seleccionado) colorAcento else DarkSurface).clickable { onClick() }.padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) { Text(nombre, color = if (seleccionado) DarkBackground else DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
@Composable
fun ItemCategoriaAjuste(titulo: String, descripcion: String, icono: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface), contentAlignment = Alignment.Center) { Icon(icono, null, tint = DarkTextSecondary, modifier = Modifier.size(24.dp)) }; Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(titulo, color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(descripcion, color = DarkTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Icon(Icons.Rounded.ChevronRight, null, tint = DarkTextSecondary) } }
@Composable
fun ItemAjusteSwitch(titulo: String, descripcion: String, activado: Boolean, colorAcento: Color = DarkPrimary, onCheckedChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) { Text(titulo, color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(descripcion, color = DarkTextSecondary, fontSize = 13.sp, lineHeight = 18.sp) }; Switch(checked = activado, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = colorAcento, uncheckedThumbColor = DarkTextSecondary, uncheckedTrackColor = DarkSurface, uncheckedBorderColor = Color.Transparent)) } }

@Composable
fun PrivacySettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(8.dp)); Text("Privacidad", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).clip(RoundedCornerShape(16.dp)).background(DarkSurface).border(1.dp, colorDinamico.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(colorDinamico.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.WifiOff, contentDescription = "Sin Internet", tint = colorDinamico, modifier = Modifier.size(32.dp)) }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recordatorio sin internet", color = DarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Esta app NO tiene permiso de internet.\nNunca se conecta a la red, nunca envía tus datos, nunca descarga nada en segundo plano.\n\nTu música y tu privacidad están 100% seguras y locales. ❤️", color = DarkTextSecondary, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gestión de Datos", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 8.dp))
        ItemCategoriaAjuste(titulo = "Backup local", descripcion = "Guarda tus playlists y ajustes en un archivo (Próximamente 🚧)", icono = Icons.Rounded.Save, onClick = { musicViewModel.mostrarMensaje("Motor de backups próximamente 🚧") })
        ItemCategoriaAjuste(titulo = "Exportar settings", descripcion = "Comparte tu configuración visual (Próximamente 🚧)", icono = Icons.Rounded.IosShare, onClick = { musicViewModel.mostrarMensaje("Exportación de ajustes en construcción 🚧") })
        ItemCategoriaAjuste(titulo = "Estadísticas (Reset)", descripcion = "Borra el historial de música reciente y escuchas (Próximamente 🚧)", icono = Icons.Rounded.DeleteSweep, onClick = { musicViewModel.mostrarMensaje("Menú de reseteo próximamente 🚧") })
    }
}

@Composable
fun ExtraSettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()
    val gestosActivos by musicViewModel.gestosCaratulaActivos.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(8.dp)); Text("Otros útiles", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        Text("Integración y Control", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 8.dp))
        ItemAjusteSwitch(titulo = "Doble toque en carátula", descripcion = "Toca dos veces los lados de la portada para adelantar o atrasar 10 segundos.", activado = gestosActivos, colorAcento = colorDinamico) { musicViewModel.alternarGestosCaratula(it) }
        ItemCategoriaAjuste(titulo = "Widgets de inicio", descripcion = "Controla Zenith desde tu pantalla principal (Próximamente 🚧)", icono = Icons.Rounded.Widgets, onClick = { musicViewModel.mostrarMensaje("Widgets nativos próximamente 🚧") })
        ItemCategoriaAjuste(titulo = "Android Auto & Wear OS", descripcion = "Lleva Zenith a tu coche y reloj inteligente (Próximamente 🚧)", icono = Icons.Rounded.DirectionsCar, onClick = { musicViewModel.mostrarMensaje("Soporte para Auto y Wear próximamente 🚧") })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reproducción Avanzada", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 8.dp))
        ItemCategoriaAjuste(titulo = "Múltiples colas", descripcion = "Gestiona varias listas de reproducción activas (Próximamente 🚧)", icono = Icons.Rounded.QueueMusic, onClick = { musicViewModel.mostrarMensaje("Múltiples colas próximamente 🚧") })
        ItemCategoriaAjuste(titulo = "Letras (Lyrics)", descripcion = "Lectura de letras incrustadas y sincronizadas (Próximamente 🚧)", icono = Icons.Rounded.Article, onClick = { musicViewModel.mostrarMensaje("Motor de letras en construcción 🚧") })
    }
}

@Composable
fun StorageSettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()
    val filtroActivo by musicViewModel.filtroAudiosCortos.collectAsState()
    val escaneoAuto by musicViewModel.escaneoAutomatico.collectAsState()
    val estaEscaneando by musicViewModel.estaEscaneando.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(8.dp)); Text("Almacenamiento", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).clip(RoundedCornerShape(16.dp)).background(colorDinamico).clickable(enabled = !estaEscaneando) { musicViewModel.reescanearBiblioteca() }.padding(20.dp), contentAlignment = Alignment.Center) {
            if (estaEscaneando) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(24.dp), strokeWidth = 3.dp); Spacer(modifier = Modifier.width(16.dp)); Text("Buscando música nueva...", color = DarkBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold) } } else { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Sync, null, tint = DarkBackground, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("Escanear Dispositivo Ahora", color = DarkBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Filtros Inteligentes", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp))
        ItemAjusteSwitch(titulo = "Ignorar audios cortos (< 1 min)", descripcion = "Oculta automáticamente notas de voz de WhatsApp y efectos de sonido de juegos.", activado = filtroActivo, colorAcento = colorDinamico) { musicViewModel.alternarFiltroAudiosCortos(it) }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Comportamiento", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp))
        ItemAjusteSwitch(titulo = "Escaneo Automático", descripcion = "Busca canciones nuevas cada vez que abres la app. Desactívalo si tienes más de 5,000 canciones para abrir la app más rápido.", activado = escaneoAuto, colorAcento = colorDinamico) { musicViewModel.alternarEscaneoAutomatico(it) }
    }
}

@Composable
fun InterfaceSettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val temaActual by musicViewModel.temaGlobal.collectAsState()
    val acentoActualHex by musicViewModel.colorAcento.collectAsState()
    val animaciones by musicViewModel.animacionesActivas.collectAsState()
    val tamanoPortadas by musicViewModel.tamanoPortadas.collectAsState()
    val colorDinamico = acentoActualHex.toColor()
    val coloresDisponibles = listOf(Pair("Verde Neón", "#1DB954"), Pair("Azul Eléctrico", "#2979FF"), Pair("Rojo Fuego", "#FF1744"), Pair("Violeta Profundo", "#D500F9"), Pair("Naranja Coral", "#FF6D00"), Pair("Oro Oscuro", "#FFD700"))
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(8.dp)); Text("Interfaz y Diseño", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        Text("Tema Visual", color = colorDinamico, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Claro", "Oscuro", "Material You").forEach { tema ->
                val seleccionado = temaActual == tema
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (seleccionado) colorDinamico else DarkSurface).clickable { musicViewModel.cambiarTema(tema); if (tema == "Material You") musicViewModel.mostrarMensaje("Material You próximamente 🚧") }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(tema, color = if (seleccionado) DarkBackground else DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Color de Acento", color = colorDinamico, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(coloresDisponibles) { colorItem ->
                val hexCode = colorItem.second
                val isSelected = acentoActualHex == hexCode
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(hexCode.toColor()).clickable { musicViewModel.cambiarAcento(hexCode) }.border(if (isSelected) 3.dp else 0.dp, if (isSelected) Color.White else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) { if (isSelected) Icon(Icons.Rounded.Check, null, tint = DarkBackground) }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Tamaño de Portadas (Global)", color = colorDinamico, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhotoSizeSelectSmall, null, tint = DarkTextSecondary)
            Slider(value = tamanoPortadas, onValueChange = { musicViewModel.cambiarTamanoPortadas(it) }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = colorDinamico, activeTrackColor = colorDinamico, inactiveTrackColor = DarkSurface))
            Icon(Icons.Rounded.PhotoSizeSelectLarge, null, tint = DarkTextSecondary)
        }
        Text(text = "Escala: ${(tamanoPortadas * 100f).toInt()}%", color = DarkTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text("Rendimiento", color = colorDinamico, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp).padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) { Text("Animaciones Fluidas", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text("Desactiva esto para ahorrar batería en viajes largos.", color = DarkTextSecondary, fontSize = 13.sp, lineHeight = 18.sp) }
            Switch(checked = animaciones, onCheckedChange = { musicViewModel.alternarAnimaciones(it) }, colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = colorDinamico, uncheckedThumbColor = DarkTextSecondary, uncheckedTrackColor = DarkSurface, uncheckedBorderColor = Color.Transparent))
        }
    }
}

@Composable
fun AboutSettingsScreen(musicViewModel: MusicViewModel, onVolver: () -> Unit) {
    val colorDinamico = musicViewModel.colorAcento.collectAsState().value.toColor()
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = DarkTextPrimary, modifier = Modifier.size(28.dp)) }; Spacer(modifier = Modifier.width(8.dp)); Text("Acerca de Zenith", color = DarkTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).background(DarkSurface).border(1.dp, colorDinamico.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(24.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Code, null, tint = colorDinamico, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(12.dp)); Text("¡Hola! Soy RoXxer, desde Quito ❤️", color = DarkTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Estoy harto —como tú— de un mundo digital que nos espía, nos bombardea con anuncios tóxicos y vende cada pedacito de nuestra vida al mejor postor.", color = DarkTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Por eso creo apps 100% gratuitas, sin un solo anuncio, sin tracking, sin vender tus datos. Quiero que sientas lo que debería ser normal: experiencias limpias, bonitas y premium... pero realmente tuyas, con privacidad total y respeto absoluto.", color = DarkTextPrimary, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Mi sueño: más herramientas así, hechas con amor —lectores de PDF elegantes, editores de documentos sin trampas, diarios íntimos encriptados y muchas apps mas... todo offline, sin maldad detrás.", color = DarkTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colorDinamico.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Si esta app te devuelve un poco de paz en este caos, y sientes ganas de apoyarme con un café ☕ (o lo que puedas), sería un honor inmenso. No es obligatorio, nunca lo será. Solo una forma de decir: \"Gracias por creer en un internet más humano\".", color = DarkTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Apoya el desarrollo:", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TarjetaDonacion(titulo = "Impulso Zenith", precio = "$0.99", descripcion = "Para que el código nunca deje de fluir.", icono = Icons.Rounded.Bolt, colorAcento = colorDinamico) { musicViewModel.mostrarMensaje("Sistema de donación próximamente 🚧") }
                            TarjetaDonacion(titulo = "☕ Café RoXxer", precio = "$2.99", descripcion = "Una noche de programación por mi cuenta.", icono = Icons.Rounded.LocalCafe, colorAcento = colorDinamico) { musicViewModel.mostrarMensaje("Sistema de donación próximamente 🚧") }
                            TarjetaDonacion(titulo = "🚀 Motor de Innovación", precio = "$4.99", descripcion = "Ayuda directa para nuevas funcionalidades.", icono = Icons.Rounded.RocketLaunch, colorAcento = colorDinamico) { musicViewModel.mostrarMensaje("Sistema de donación próximamente 🚧") }
                            TarjetaDonacion(titulo = "💎 Legado Zenith", precio = "$9.99", descripcion = "Eres el pilar fundamental de esta aplicación.", icono = Icons.Rounded.Diamond, colorAcento = colorDinamico) { musicViewModel.mostrarMensaje("Sistema de donación próximamente 🚧") }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Si quieres donar más, puedes comprar varias veces el mismo ítem.", color = DarkTextSecondary, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(text = "Hecho con ❤️ en Quito, Ecuador.\nTu confianza lo significa todo.", color = DarkTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)); Text("Información de la App", color = colorDinamico, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 8.dp)) }
            item { ItemInfoApp(titulo = "Versión", valor = "0.01 (BETA)", icono = Icons.Rounded.Info) }
            item { ItemInfoApp(titulo = "Changelog", valor = "Primera optimización general", icono = Icons.Rounded.Update) }
            item { ItemInfoApp(titulo = "Créditos", valor = "RoXxer Studio", icono = Icons.Rounded.Star) }
            item { ItemInfoApp(titulo = "Contacto", valor = "roxxer.studio@gmail.com", icono = Icons.Rounded.Email, alHacerClic = { val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:roxxer.studio@gmail.com")); context.startActivity(Intent.createChooser(intent, "Enviar correo a RoXxer Studio")) }) }
        }
    }
}

@Composable
fun TarjetaDonacion(titulo: String, precio: String, descripcion: String, icono: androidx.compose.ui.graphics.vector.ImageVector, colorAcento: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkBackground).border(1.dp, colorAcento.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(colorAcento.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icono, contentDescription = titulo, tint = colorAcento, modifier = Modifier.size(24.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = titulo, color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text(text = precio, color = colorAcento, fontSize = 16.sp, fontWeight = FontWeight.Black) }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = descripcion, color = DarkTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun ItemInfoApp(titulo: String, valor: String, icono: androidx.compose.ui.graphics.vector.ImageVector, alHacerClic: (() -> Unit)? = null) {
    val modifier = if (alHacerClic != null) Modifier.clickable { alHacerClic() } else Modifier
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icono, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(titulo, color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(valor, color = DarkTextSecondary, fontSize = 14.sp) } }
}
@file:Suppress("SpellCheckingInspection")

package com.roxxer.zenith

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private val Context.dataStore by preferencesDataStore(
    name = "zenith_settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "zenith_memoria")) }
)

data class BandaEq(val indice: Short, val frecuenciaHz: Int, var nivel: Short, val minNivel: Short, val maxNivel: Short)

@OptIn(UnstableApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZenithDatabase.getDatabase(application)
    private val favoritosDao = db.favoritosDao()
    private val playlistsDao = db.playlistsDao()

    private val nombreUsuarioKey = stringPreferencesKey("nombre_usuario")
    private val fotoPerfilKey = stringPreferencesKey("foto_perfil")
    private val historialBusquedaKey = stringPreferencesKey("historial_busqueda")
    private val ajusteFiltroCortosKey = booleanPreferencesKey("ajuste_filtro_cortos")
    private val ajusteEscaneoAutoKey = booleanPreferencesKey("ajuste_escaneo_auto")
    private val ajusteTemaKey = stringPreferencesKey("ajuste_tema")
    private val ajusteAcentoKey = stringPreferencesKey("ajuste_acento")
    private val ajusteAnimacionesKey = booleanPreferencesKey("ajuste_animaciones")
    private val ajustePortadasKey = floatPreferencesKey("ajuste_portadas")
    private val ajusteGestosKey = booleanPreferencesKey("ajuste_gestos")
    private val ajusteGaplessKey = booleanPreferencesKey("ajuste_gapless")
    private val ajusteNormalizacionKey = booleanPreferencesKey("ajuste_normalizacion")
    private val ajusteCrossfadeKey = intPreferencesKey("ajuste_crossfade")
    private val eqActivadoKey = booleanPreferencesKey("eq_activado")
    private val bassBoostFuerzaKey = intPreferencesKey("bass_boost_fuerza")
    private val eqPresetActualKey = stringPreferencesKey("eq_preset_actual")
    private val ctxTipoKey = stringPreferencesKey("ctx_tipo")
    private val ctxPidKey = intPreferencesKey("ctx_pid")
    private val ctxValorKey = stringPreferencesKey("ctx_valor")
    private val ctxPnombreKey = stringPreferencesKey("ctx_pnombre")
    private val ctxTabKey = intPreferencesKey("ctx_tab")
    private val historialRecientesKey = stringPreferencesKey("historial_recientes")
    private val ultimaCancionIdKey = stringPreferencesKey("ultima_cancion_id")
    private val ultimaPosicionKey = longPreferencesKey("ultima_posicion")

    val nombreUsuario = getApplication<Application>().dataStore.data.map { it[nombreUsuarioKey] ?: "RoXxer" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "RoXxer")
    val fotoPerfilUri = getApplication<Application>().dataStore.data.map { it[fotoPerfilKey] }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val portadasCustom: StateFlow<Map<String, String>> = getApplication<Application>().dataStore.data
        .map { prefs -> prefs.asMap().filterKeys { it.name.startsWith("cover_") }.mapKeys { it.key.name }.mapValues { it.value as String } }
        .distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // --- NUEVO: Motor de Nombres Personalizados (Alias) ---
    val nombresCustom: StateFlow<Map<String, String>> = getApplication<Application>().dataStore.data
        .map { prefs -> prefs.asMap().filterKeys { it.name.startsWith("alias_") }.mapKeys { it.key.name }.mapValues { it.value as String } }
        .distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val busquedasRecientes = getApplication<Application>().dataStore.data.map { it[historialBusquedaKey]?.split(";;")?.filter { str -> str.isNotBlank() } ?: emptyList() }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val filtroAudiosCortos = getApplication<Application>().dataStore.data.map { it[ajusteFiltroCortosKey] ?: true }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val escaneoAutomatico = getApplication<Application>().dataStore.data.map { it[ajusteEscaneoAutoKey] ?: true }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val temaGlobal = getApplication<Application>().dataStore.data.map { it[ajusteTemaKey] ?: "Oscuro" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "Oscuro")
    val colorAcento = getApplication<Application>().dataStore.data.map { it[ajusteAcentoKey] ?: "#1DB954" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "#1DB954")
    val animacionesActivas = getApplication<Application>().dataStore.data.map { it[ajusteAnimacionesKey] ?: true }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val tamanoPortadas = getApplication<Application>().dataStore.data.map { it[ajustePortadasKey] ?: 1.0f }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val gestosCaratulaActivos = getApplication<Application>().dataStore.data.map { it[ajusteGestosKey] ?: true }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val normalizacionActivada = getApplication<Application>().dataStore.data.map { it[ajusteNormalizacionKey] ?: false }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val crossfadeSegundos = getApplication<Application>().dataStore.data.map { it[ajusteCrossfadeKey] ?: 0 }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val gaplessActivado = getApplication<Application>().dataStore.data.map { it[ajusteGaplessKey] ?: false }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val eqActivado = getApplication<Application>().dataStore.data.map { it[eqActivadoKey] ?: false }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val bassBoostFuerza = getApplication<Application>().dataStore.data.map { (it[bassBoostFuerzaKey] ?: 0).toFloat() / 1000f }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, 0f)
    val presetActual = getApplication<Application>().dataStore.data.map { it[eqPresetActualKey] ?: "Personalizado" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "Personalizado")

    val contextoActualTipo = getApplication<Application>().dataStore.data.map { it[ctxTipoKey] ?: "CARPETA" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "CARPETA")
    val contextoActualPlaylistId = getApplication<Application>().dataStore.data.map { it[ctxPidKey] }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val contextoActualValor = getApplication<Application>().dataStore.data.map { it[ctxValorKey] ?: "" }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val contextoActualPlaylistNombre = getApplication<Application>().dataStore.data.map { it[ctxPnombreKey] }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val tabBibliotecaActiva = getApplication<Application>().dataStore.data.map { it[ctxTabKey] ?: 0 }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun alternarGapless(activo: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { prefs -> prefs[ajusteGaplessKey] = activo; if (activo) prefs[ajusteCrossfadeKey] = 0 } } }
    fun cambiarCrossfade(segundos: Int) { viewModelScope.launch { getApplication<Application>().dataStore.edit { prefs -> prefs[ajusteCrossfadeKey] = segundos; if (segundos > 0) prefs[ajusteGaplessKey] = false } } }

    fun guardarNombreUsuario(nuevoNombre: String) { val nombreLimpio = nuevoNombre.trim().ifBlank { "Usuario" }; viewModelScope.launch { getApplication<Application>().dataStore.edit { it[nombreUsuarioKey] = nombreLimpio } }; mostrarMensaje("Perfil actualizado ✨") }
    fun guardarFotoPerfil(uri: Uri) { val contextoGlobal = getApplication<Application>().applicationContext; viewModelScope.launch(Dispatchers.IO) { try { val inputStream = contextoGlobal.contentResolver.openInputStream(uri); val file = File(contextoGlobal.filesDir, "perfil_zenith.jpg"); val outputStream = file.outputStream(); inputStream?.copyTo(outputStream); inputStream?.close(); outputStream.close(); val localUri = Uri.fromFile(file).toString(); getApplication<Application>().dataStore.edit { it[fotoPerfilKey] = localUri }; mostrarMensaje("Foto de perfil actualizada 📸") } catch (_: Exception) { mostrarMensaje("Error al guardar la foto ❌") } } }

    private fun calcularInSampleSize(options: android.graphics.BitmapFactory.Options): Int { val (height: Int, width: Int) = options.outHeight to options.outWidth; var inSampleSize = 1; if (height > 512 || width > 512) { val halfHeight: Int = height / 2; val halfWidth: Int = width / 2; while (halfHeight / inSampleSize >= 512 && halfWidth / inSampleSize >= 512) { inSampleSize *= 2 } }; return inSampleSize }

    fun guardarPortadaCustom(llave: String, uri: Uri) { val contextoGlobal = getApplication<Application>().applicationContext; viewModelScope.launch(Dispatchers.IO) { try { val dir = File(contextoGlobal.filesDir, "custom_covers"); if (!dir.exists()) dir.mkdir(); val file = File(dir, "${llave}_${System.currentTimeMillis()}.webp"); val options = android.graphics.BitmapFactory.Options(); options.inJustDecodeBounds = true; contextoGlobal.contentResolver.openInputStream(uri)?.use { input -> android.graphics.BitmapFactory.decodeStream(input, null, options) }; options.inSampleSize = calcularInSampleSize(options); options.inJustDecodeBounds = false; val bitmapReducido = contextoGlobal.contentResolver.openInputStream(uri)?.use { input -> android.graphics.BitmapFactory.decodeStream(input, null, options) }; if (bitmapReducido != null) { val outputStream = file.outputStream(); val dimension = minOf(bitmapReducido.width, bitmapReducido.height); val x = (bitmapReducido.width - dimension) / 2; val y = (bitmapReducido.height - dimension) / 2; val cropped = android.graphics.Bitmap.createBitmap(bitmapReducido, x, y, dimension, dimension); val scaled = cropped.scale(512, 512); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { scaled.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 85, outputStream) } else { @Suppress("DEPRECATION") scaled.compress(android.graphics.Bitmap.CompressFormat.WEBP, 85, outputStream) }; outputStream.close(); if (cropped != bitmapReducido) bitmapReducido.recycle(); if (scaled != cropped) cropped.recycle(); val localUri = Uri.fromFile(file).toString(); getApplication<Application>().dataStore.edit { it[stringPreferencesKey(llave)] = localUri }; mostrarMensaje("Portada actualizada ✨") } } catch (_: Exception) { mostrarMensaje("Error al procesar imagen ❌") } } }
    fun eliminarPortadaCustom(llave: String) { viewModelScope.launch(Dispatchers.IO) { val pathActual = portadasCustom.value[llave]; if (pathActual != null) { try { File(pathActual.toUri().path!!).delete() } catch (_: Exception) {} }; getApplication<Application>().dataStore.edit { it.remove(stringPreferencesKey(llave)) }; mostrarMensaje("Portada eliminada 🗑️") } }

    // --- NUEVO: Funciones para Renombrar (Alias) ---
    fun guardarNombreCustom(llave: String, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().dataStore.edit { it[stringPreferencesKey(llave)] = nuevoNombre }
            mostrarMensaje("Renombrado a '$nuevoNombre' ✨")
        }
    }

    // --- NUEVO: Funciones para Eliminar Archivos Físicos ---
    fun obtenerIntentParaEliminar(uris: List<Uri>): android.content.IntentSender? {
        // En Android 10+, necesitamos pedir permiso con un Intent especial para borrar archivos de otros
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, uris).intentSender
        } else null
    }

    fun borrarCancionesHeredado(uris: List<Uri>) {
        // Para Android 9 o inferior (Tienen acceso directo si tienen READ_EXTERNAL_STORAGE)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uris.forEach { uri -> getApplication<Application>().contentResolver.delete(uri, null, null) }
                reescanearBiblioteca()
                mostrarMensaje("Música eliminada del dispositivo 🗑️")
            } catch (_: Exception) {
                mostrarMensaje("Error al eliminar archivo ❌")
            }
        }
    }

    fun guardarBusquedaReciente(query: String) { if (query.isBlank()) return; val listaActual = busquedasRecientes.value.toMutableList(); listaActual.remove(query); listaActual.add(0, query); val nuevaLista = listaActual.take(8); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[historialBusquedaKey] = nuevaLista.joinToString(";;") } } }
    fun limpiarHistorialBusqueda() { viewModelScope.launch { getApplication<Application>().dataStore.edit { it.remove(historialBusquedaKey) } } }

    private val _estaEscaneando = MutableStateFlow(false)
    val estaEscaneando: StateFlow<Boolean> = _estaEscaneando

    fun alternarFiltroAudiosCortos(activo: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteFiltroCortosKey] = activo } }; reescanearBiblioteca() }
    fun alternarEscaneoAutomatico(activo: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteEscaneoAutoKey] = activo } } }
    fun reescanearBiblioteca() { if (_estaEscaneando.value) return; _estaEscaneando.value = true; viewModelScope.launch(Dispatchers.IO) { cargarMusicaLocal(); delay(800); _estaEscaneando.value = false; mostrarMensaje("Biblioteca actualizada 🗂️") } }

    fun cambiarTema(tema: String) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteTemaKey] = tema } } }
    fun cambiarAcento(hexColor: String) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteAcentoKey] = hexColor } } }
    fun alternarAnimaciones(activas: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteAnimacionesKey] = activas } }; mostrarMensaje(if (activas) "Animaciones ON ✨" else "Ahorro de batería activado 🔋") }
    fun cambiarTamanoPortadas(tamano: Float) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajustePortadasKey] = tamano } } }
    fun alternarGestosCaratula(activo: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteGestosKey] = activo } } }
    fun alternarNormalizacion(activo: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ajusteNormalizacionKey] = activo } }; try { normalizadorAudio?.enabled = activo } catch (_: Exception) { } }

    private var ecualizadorAudio: Equalizer? = null
    private var bassBoostAudio: BassBoost? = null
    private var normalizadorAudio: LoudnessEnhancer? = null

    private val _bandasEcualizador = MutableStateFlow<List<BandaEq>>(emptyList())
    val bandasEcualizador: StateFlow<List<BandaEq>> = _bandasEcualizador
    private val _presetsDisponibles = MutableStateFlow<List<String>>(emptyList())
    val presetsDisponibles: StateFlow<List<String>> = _presetsDisponibles

    private fun inicializarEfectosDeSonido(sessionId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val prefs = getApplication<Application>().dataStore.data.first()
                ecualizadorAudio?.release(); bassBoostAudio?.release(); normalizadorAudio?.release()
                val idReal = if (sessionId == android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) 0 else sessionId
                ecualizadorAudio = Equalizer(0, idReal); bassBoostAudio = BassBoost(0, idReal); normalizadorAudio = LoudnessEnhancer(idReal)

                normalizadorAudio?.setTargetGain(800)
                normalizadorAudio?.enabled = prefs[ajusteNormalizacionKey] ?: false
                ecualizadorAudio?.enabled = prefs[eqActivadoKey] ?: false
                bassBoostAudio?.enabled = prefs[eqActivadoKey] ?: false

                val bandas = mutableListOf<BandaEq>()
                val numBandas = ecualizadorAudio?.numberOfBands ?: 0
                val rangoNiveles = ecualizadorAudio?.bandLevelRange ?: shortArrayOf(-1500, 1500)
                for (i in 0 until numBandas) {
                    val indice = i.toShort()
                    val freqCentro = ecualizadorAudio?.getCenterFreq(indice)?.div(1000) ?: 0
                    val nivelGuardado = (prefs[intPreferencesKey("eq_banda_$indice")] ?: 0).toShort()
                    ecualizadorAudio?.setBandLevel(indice, nivelGuardado)
                    bandas.add(BandaEq(indice, freqCentro, nivelGuardado, rangoNiveles[0], rangoNiveles[1]))
                }
                _bandasEcualizador.value = bandas

                val numPresets = ecualizadorAudio?.numberOfPresets ?: 0
                val nombresPresets = mutableListOf<String>()
                for (i in 0 until numPresets) { val nombre = ecualizadorAudio?.getPresetName(i.toShort()) ?: ""; if (nombre.isNotBlank()) nombresPresets.add(nombre) }
                _presetsDisponibles.value = nombresPresets
                if (bassBoostAudio?.strengthSupported == true) { val fuerza = (prefs[bassBoostFuerzaKey] ?: 0).toShort(); bassBoostAudio?.setStrength(fuerza) }
            } catch (_: Exception) { }
        }
    }

    fun alternarInterruptorEcualizador(activar: Boolean) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[eqActivadoKey] = activar } }; if (ecualizadorAudio == null || _bandasEcualizador.value.isEmpty()) { val sessionActual = reproductor?.audioSessionId ?: 0; inicializarEfectosDeSonido(sessionActual) }; try { ecualizadorAudio?.enabled = activar; bassBoostAudio?.enabled = activar; if (activar && _bandasEcualizador.value.isEmpty()) mostrarMensaje("Tu teléfono bloquea el Ecualizador Nativo 🚫") } catch (_: Exception) { mostrarMensaje("Error al activar el chip de sonido 🔌") } }
    fun cambiarNivelBanda(indice: Short, nuevoNivel: Float) { val nivelShort = nuevoNivel.toInt().toShort(); ecualizadorAudio?.setBandLevel(indice, nivelShort); val bandasActualizadas = _bandasEcualizador.value.map { if (it.indice == indice) it.copy(nivel = nivelShort) else it }; _bandasEcualizador.value = bandasActualizadas; viewModelScope.launch { getApplication<Application>().dataStore.edit { it[intPreferencesKey("eq_banda_$indice")] = nivelShort.toInt(); it[eqPresetActualKey] = "Personalizado" } } }
    fun aplicarPreset(nombrePreset: String) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[eqPresetActualKey] = nombrePreset } }; if (nombrePreset == "Personalizado") return; val numPresets = ecualizadorAudio?.numberOfPresets ?: 0; for (i in 0 until numPresets) { if (ecualizadorAudio?.getPresetName(i.toShort()) == nombrePreset) { ecualizadorAudio?.usePreset(i.toShort()); val nuevasBandas = _bandasEcualizador.value.map { banda -> val nivelHardware = ecualizadorAudio?.getBandLevel(banda.indice) ?: 0; viewModelScope.launch { getApplication<Application>().dataStore.edit { it[intPreferencesKey("eq_banda_${banda.indice}")] = nivelHardware.toInt() } }; banda.copy(nivel = nivelHardware) }; _bandasEcualizador.value = nuevasBandas; break } } }
    fun cambiarFuerzaBassBoost(porcentaje: Float) { val fuerzaHardware = (porcentaje * 1000).toInt().toShort(); if (bassBoostAudio?.strengthSupported == true) bassBoostAudio?.setStrength(fuerzaHardware); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[bassBoostFuerzaKey] = fuerzaHardware.toInt() } } }
    fun restablecerEcualizador() { val bandasLimpias = _bandasEcualizador.value.map { banda -> ecualizadorAudio?.setBandLevel(banda.indice, 0); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[intPreferencesKey("eq_banda_${banda.indice}")] = 0 } }; banda.copy(nivel = 0) }; _bandasEcualizador.value = bandasLimpias; if (bassBoostAudio?.strengthSupported == true) bassBoostAudio?.setStrength(0); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[bassBoostFuerzaKey] = 0; it[eqPresetActualKey] = "Personalizado" } }; mostrarMensaje("Ajustes restablecidos a Plano 🎛️") }

    private val _canciones = MutableStateFlow<List<AudioModel>>(emptyList())
    val canciones: StateFlow<List<AudioModel>> = _canciones

    private val _cancionesAgrupadasPorCarpeta = MutableStateFlow<Map<String, List<AudioModel>>>(emptyMap())
    val cancionesAgrupadasPorCarpeta: StateFlow<Map<String, List<AudioModel>>> = _cancionesAgrupadasPorCarpeta
    private val _cancionesAgrupadasPorArtista = MutableStateFlow<Map<String, List<AudioModel>>>(emptyMap())
    val cancionesAgrupadasPorArtista: StateFlow<Map<String, List<AudioModel>>> = _cancionesAgrupadasPorArtista
    private val _cancionesAgrupadasPorAlbum = MutableStateFlow<Map<String, List<AudioModel>>>(emptyMap())
    val cancionesAgrupadasPorAlbum: StateFlow<Map<String, List<AudioModel>>> = _cancionesAgrupadasPorAlbum

    private val _cancionActual = MutableStateFlow<AudioModel?>(null)
    val cancionActual: StateFlow<AudioModel?> = _cancionActual
    private val _estaReproduciendo = MutableStateFlow(false)
    val estaReproduciendo: StateFlow<Boolean> = _estaReproduciendo
    private val _posicionActual = MutableStateFlow(0L)
    val posicionActual: StateFlow<Long> = _posicionActual
    private val _duracionTotal = MutableStateFlow(0L)
    val duracionTotal: StateFlow<Long> = _duracionTotal
    private val _shuffleActivado = MutableStateFlow(false)
    val shuffleActivado: StateFlow<Boolean> = _shuffleActivado
    private val _modoRepeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val modoRepeat: StateFlow<Int> = _modoRepeat
    private val _tiempoRestanteTimer = MutableStateFlow<Long?>(null)
    val tiempoRestanteTimer: StateFlow<Long?> = _tiempoRestanteTimer

    private var jobTemporizador: Job? = null
    private var apagarAlFinalizarCancion = false

    val listaFavoritos: StateFlow<List<String>> = favoritosDao.obtenerTodosLosFavoritos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val misPlaylists: StateFlow<List<PlaylistEntity>> = playlistsDao.obtenerTodasLasPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _mensajeNotificacion = MutableStateFlow<String?>(null)
    val mensajeNotificacion: StateFlow<String?> = _mensajeNotificacion
    private var jobNotificacion: Job? = null
    fun mostrarMensaje(mensaje: String) { jobNotificacion?.cancel(); jobNotificacion = viewModelScope.launch { _mensajeNotificacion.value = null; delay(100); _mensajeNotificacion.value = mensaje; delay(3500); _mensajeNotificacion.value = null } }

    private val _cancionesRecientes = MutableStateFlow<List<AudioModel>>(emptyList())
    val cancionesRecientes: StateFlow<List<AudioModel>> = _cancionesRecientes
    private val _carpetaAExplorar = MutableStateFlow<String?>(null)
    val carpetaAExplorar: StateFlow<String?> = _carpetaAExplorar
    private val _playlistAExplorar = MutableStateFlow<PlaylistEntity?>(null)
    val playlistAExplorar: StateFlow<PlaylistEntity?> = _playlistAExplorar
    private val _explorarFavoritos = MutableStateFlow(false)
    val explorarFavoritos: StateFlow<Boolean> = _explorarFavoritos
    private val _scrollTrigger = MutableStateFlow(0L)
    val scrollTrigger: StateFlow<Long> = _scrollTrigger
    private val _agrupacionAExplorar = MutableStateFlow<String?>(null)
    val agrupacionAExplorar: StateFlow<String?> = _agrupacionAExplorar

    fun explorarCarpeta(nombre: String?) { _carpetaAExplorar.value = nombre }
    fun explorarPlaylist(playlist: PlaylistEntity?) { _playlistAExplorar.value = playlist }
    fun explorarFavoritos(abrir: Boolean) { _explorarFavoritos.value = abrir }
    fun explorarTabBiblioteca(index: Int) { viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ctxTabKey] = index } } }
    fun explorarAgrupacion(nombre: String?) { _agrupacionAExplorar.value = nombre }

    fun prepararNavegacionAContexto(): String {
        val cancion = _cancionActual.value ?: return "biblioteca"
        if (contextoActualTipo.value == "FAVORITOS" && !listaFavoritos.value.contains(cancion.uri.toString())) { aplicarParacaidasDeSeguridad(cancion) }
        _scrollTrigger.value = System.currentTimeMillis()

        return when (contextoActualTipo.value) {
            "CARPETA" -> { val carpetaDestino =
                contextoActualValor.value.ifBlank { cancion.carpeta }; explorarTabBiblioteca(0); explorarCarpeta(carpetaDestino); "biblioteca" }
            "CANCIONES" -> { explorarTabBiblioteca(1); "biblioteca" }
            "ARTISTA" -> { explorarTabBiblioteca(2); explorarAgrupacion(contextoActualValor.value); "biblioteca" }
            "ALBUM" -> { explorarTabBiblioteca(3); explorarAgrupacion(contextoActualValor.value); "biblioteca" }
            "FAVORITOS" -> { explorarFavoritos(true); "playlists" }
            "PLAYLIST" -> { if (contextoActualPlaylistId.value != null && contextoActualPlaylistNombre.value != null) { explorarPlaylist(PlaylistEntity(contextoActualPlaylistId.value!!, contextoActualPlaylistNombre.value!!)); "playlists" } else { explorarTabBiblioteca(0); explorarCarpeta(cancion.carpeta); "biblioteca" } }
            "TODAS" -> { explorarTabBiblioteca(1); "biblioteca" }
            else -> { explorarTabBiblioteca(0); explorarCarpeta(cancion.carpeta); "biblioteca" }
        }
    }

    private fun aplicarParacaidasDeSeguridad(cancion: AudioModel) { explorarTabBiblioteca(0); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ctxTipoKey] = "CARPETA"; it[ctxValorKey] = cancion.carpeta } } }

    private fun registrarEnHistorial(idCancion: String) {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dataStore.data.first()
            val historialStr = prefs[historialRecientesKey] ?: ""
            val historial = historialStr.split(",").filter { it.isNotBlank() }.toMutableList()
            historial.remove(idCancion); historial.add(0, idCancion)
            if (historial.size > 15) historial.removeAt(historial.lastIndex)
            getApplication<Application>().dataStore.edit { it[historialRecientesKey] = historial.joinToString(",") }
            actualizarListaRecientes(historial)
        }
    }

    private fun actualizarListaRecientes(historialIds: List<String>) {
        val todas = _canciones.value
        val listaHistorial = historialIds.mapNotNull { id -> todas.find { it.id.toString() == id } }
        if (listaHistorial.isEmpty()) _cancionesRecientes.value = todas.reversed().take(15) else _cancionesRecientes.value = listaHistorial
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var reproductor: Player? = null

    private var ghostPlayer: ExoPlayer? = null
    private var isMixing = false
    private var isAutoSkippingForCrossfade = false
    private var isGhostPrepared = false

    private val faderHandler = Handler(Looper.getMainLooper())
    private val motorFaderRunnable = object : Runnable {
        override fun run() {
            try {
                reproductor?.let { player ->
                    if (player.isPlaying || ghostPlayer?.isPlaying == true) {
                        val pos = player.currentPosition
                        val dur = player.duration
                        val cfMs = crossfadeSegundos.value * 1000L

                        if (player.isPlaying) {
                            _posicionActual.value = pos
                        }

                        if (cfMs > 0L && dur > 0L && !gaplessActivado.value && player.isPlaying) {
                            val tiempoRestante = dur - pos

                            if (tiempoRestante in (cfMs + 1)..(cfMs + 5000) && !isGhostPrepared && player.hasNextMediaItem()) {
                                isGhostPrepared = true
                                val currentItem = player.currentMediaItem
                                if (currentItem != null) {
                                    ghostPlayer?.setMediaItem(currentItem)
                                    ghostPlayer?.prepare()
                                    ghostPlayer?.seekTo(dur - cfMs)
                                    ghostPlayer?.volume = 0f
                                    ghostPlayer?.pause()
                                }
                            }

                            if (tiempoRestante in 1..cfMs && !isMixing && player.hasNextMediaItem()) {
                                isMixing = true

                                if (!isGhostPrepared) {
                                    val currentItem = player.currentMediaItem
                                    if (currentItem != null) {
                                        ghostPlayer?.setMediaItem(currentItem)
                                        ghostPlayer?.prepare()
                                        ghostPlayer?.seekTo(pos)
                                    }
                                } else {
                                    ghostPlayer?.seekTo(pos)
                                }

                                ghostPlayer?.volume = player.volume
                                ghostPlayer?.play()

                                player.volume = 0f

                                isAutoSkippingForCrossfade = true
                                faderHandler.postDelayed({
                                    if (isMixing) {
                                        player.seekToNext()
                                        isAutoSkippingForCrossfade = false
                                        isGhostPrepared = false
                                    }
                                }, 150)
                            }
                        }

                        if (ghostPlayer?.isPlaying == true) {
                            val ghostPos = ghostPlayer!!.currentPosition
                            val ghostDur = ghostPlayer!!.duration
                            val ghostRest = ghostDur - ghostPos

                            if (cfMs > 0 && ghostRest > 0) {
                                val prop = (ghostRest.toFloat() / cfMs.toFloat()).coerceIn(0f, 1f)
                                ghostPlayer?.volume = (prop * prop * prop).coerceAtLeast(0f)
                            } else {
                                ghostPlayer?.stop()
                                ghostPlayer?.clearMediaItems()
                            }
                        }

                        if (isMixing && player.isPlaying) {
                            if (isAutoSkippingForCrossfade) {
                                player.volume = 0f
                            } else if (cfMs > 0 && pos in 0..cfMs) {
                                val prop = (pos.toFloat() / cfMs.toFloat()).coerceIn(0f, 1f)
                                player.volume = kotlin.math.sqrt(prop.toDouble()).toFloat()
                            } else {
                                player.volume = 1f
                                isMixing = false
                            }
                        }

                        if (!isMixing && player.volume < 1f && cfMs == 0L) {
                            player.volume = 1f
                        }
                    }
                }
            } catch (_: Exception) { }

            if (_estaReproduciendo.value || ghostPlayer?.isPlaying == true) {
                faderHandler.postDelayed(this, 16L)
            }
        }
    }

    init {
        cargarMusicaLocal()
        ghostPlayer = ExoPlayer.Builder(application).build()
        conectarAlServicio()
    }

    fun crearPlaylist(nombre: String) { viewModelScope.launch(Dispatchers.IO) { playlistsDao.crearPlaylist(PlaylistEntity(nombre = nombre)); mostrarMensaje("Playlist '$nombre' creada") } }
    fun agregarAPlaylist(idPlaylist: Int, uriString: String, nombrePlaylist: String) { viewModelScope.launch(Dispatchers.IO) { playlistsDao.agregarCancionAPlaylist(PlaylistCancionCrossRef(idPlaylist, uriString)); mostrarMensaje("Agregada a $nombrePlaylist") } }
    fun removerDePlaylist(idPlaylist: Int, uriString: String, nombrePlaylist: String) { viewModelScope.launch(Dispatchers.IO) { playlistsDao.eliminarCancionDePlaylist(idPlaylist, uriString); mostrarMensaje("Eliminada de $nombrePlaylist"); if (contextoActualTipo.value == "PLAYLIST" && contextoActualPlaylistId.value == idPlaylist && _cancionActual.value?.uri?.toString() == uriString) { _cancionActual.value?.let { aplicarParacaidasDeSeguridad(it) } } } }
    fun eliminarPlaylist(idPlaylist: Int) { viewModelScope.launch(Dispatchers.IO) { playlistsDao.vaciarPlaylist(idPlaylist); playlistsDao.eliminarPlaylist(idPlaylist); mostrarMensaje("Playlist eliminada"); if (contextoActualTipo.value == "PLAYLIST" && contextoActualPlaylistId.value == idPlaylist) { _cancionActual.value?.let { aplicarParacaidasDeSeguridad(it) } } } }
    fun obtenerUrisDePlaylist(idPlaylist: Int) = playlistsDao.obtenerCancionesDePlaylist(idPlaylist)
    fun obtenerPlaylistsDeCancion(uriString: String) = playlistsDao.obtenerPlaylistsDeCancion(uriString)

    // --- NUEVO: Renombrar Playlist ---
    fun renombrarPlaylist(idPlaylist: Int, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            playlistsDao.actualizarNombrePlaylist(idPlaylist, nuevoNombre)
            mostrarMensaje("Playlist renombrada ✨")
        }
    }

    fun reproducirCancion(cancion: AudioModel, listaDeContexto: List<AudioModel>? = null, tipoContexto: String = "CARPETA", valorContexto: String = "", playlistId: Int? = null, playlistNombre: String? = null) {
        viewModelScope.launch { getApplication<Application>().dataStore.edit { prefs -> prefs[ctxTipoKey] = tipoContexto; prefs[ctxValorKey] = if (tipoContexto == "CARPETA" && valorContexto.isEmpty()) cancion.carpeta else valorContexto; if (playlistId != null) prefs[ctxPidKey] = playlistId else prefs.remove(ctxPidKey); if (playlistNombre != null) prefs[ctxPnombreKey] = playlistNombre else prefs.remove(ctxPnombreKey) } }
        val listaAEjecutar = listaDeContexto ?: _canciones.value.filter { it.carpeta == cancion.carpeta }
        val indiceSeleccionado = listaAEjecutar.indexOf(cancion)
        reproductor?.let { player ->
            if (indiceSeleccionado != -1) {
                isMixing = false
                isGhostPrepared = false
                ghostPlayer?.stop()

                val mediaItemsParaAndroid = listaAEjecutar.map { audio -> val metadatos = MediaMetadata.Builder().setTitle(audio.titulo).setArtist(audio.artista).setArtworkUri(audio.albumArtUri).build(); MediaItem.Builder().setMediaId(audio.id.toString()).setUri(audio.uri).setMediaMetadata(metadatos).build() }
                player.setMediaItems(mediaItemsParaAndroid, indiceSeleccionado, 0L); player.prepare(); player.play()
            }
        }
    }

    fun iniciarTemporizador(minutos: Int) { cancelarTemporizador(); apagarAlFinalizarCancion = false; jobTemporizador = viewModelScope.launch(Dispatchers.Main) { var tiempoRestante = minutos * 60 * 1000L; while (tiempoRestante > 0) { _tiempoRestanteTimer.value = tiempoRestante; delay(1000); tiempoRestante -= 1000 }; reproductor?.pause(); cancelarTemporizador() } }
    fun iniciarTemporizadorFinDeCancion() { cancelarTemporizador(); apagarAlFinalizarCancion = true; _tiempoRestanteTimer.value = -1L }
    fun cancelarTemporizador() { jobTemporizador?.cancel(); jobTemporizador = null; apagarAlFinalizarCancion = false; _tiempoRestanteTimer.value = null }

    fun alternarFavorito(uriString: String) { viewModelScope.launch(Dispatchers.IO) { val esFavorito = listaFavoritos.value.contains(uriString); if (esFavorito) { favoritosDao.quitarFavorito(FavoritoEntity(uriString)); mostrarMensaje("Eliminada de Favoritos"); if (contextoActualTipo.value == "FAVORITOS" && _cancionActual.value?.uri?.toString() == uriString) { _cancionActual.value?.let { aplicarParacaidasDeSeguridad(it) } } } else { favoritosDao.agregarFavorito(FavoritoEntity(uriString)); mostrarMensaje("Agregada a Favoritos") } } }

    private fun conectarAlServicio() {
        val context = getApplication<Application>().applicationContext
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            reproductor = controllerFuture?.get()

            if (reproductor?.mediaItemCount == 0) restaurarUltimaCancion() else sincronizarConServicioActivo()
            reproductor?.audioSessionId?.let { sessionActual -> if (sessionActual != 0) inicializarEfectosDeSonido(sessionActual) }

            reproductor?.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) { super.onAudioSessionIdChanged(audioSessionId); inicializarEfectosDeSonido(audioSessionId) }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _estaReproduciendo.value = isPlaying
                    if (isPlaying) {
                        faderHandler.removeCallbacks(motorFaderRunnable)
                        faderHandler.post(motorFaderRunnable)
                    } else {
                        if (ghostPlayer?.isPlaying == false) faderHandler.removeCallbacks(motorFaderRunnable)
                        reproductor?.let { player -> viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ultimaPosicionKey] = player.currentPosition } } }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_READY) _duracionTotal.value = reproductor?.duration ?: 0L }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    mediaItem?.mediaId?.let { idString ->
                        _cancionActual.value = _canciones.value.find { it.id.toString() == idString }
                        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ultimaCancionIdKey] = idString } }
                        registrarEnHistorial(idString)
                    }

                    val isAuto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || isAutoSkippingForCrossfade

                    if (!isAuto) {
                        isMixing = false
                        isGhostPrepared = false
                        ghostPlayer?.stop()
                        ghostPlayer?.clearMediaItems()
                        reproductor?.volume = 1f
                    } else {
                        val cfMs = crossfadeSegundos.value * 1000
                        if (cfMs == 0 || gaplessActivado.value) {
                            reproductor?.volume = 1f
                        }
                    }

                    if (apagarAlFinalizarCancion && isAuto && !isAutoSkippingForCrossfade) { reproductor?.pause(); cancelarTemporizador() }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { _shuffleActivado.value = shuffleModeEnabled }
                override fun onRepeatModeChanged(repeatMode: Int) { _modoRepeat.value = repeatMode }
            })
        }, ContextCompat.getMainExecutor(context))
    }

    fun alternarShuffle() { reproductor?.let { val nuevoEstado = !it.shuffleModeEnabled; it.shuffleModeEnabled = nuevoEstado; _shuffleActivado.value = nuevoEstado } }
    fun alternarRepeat() { reproductor?.let { val nuevoModo = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; it.repeatMode = nuevoModo; _modoRepeat.value = nuevoModo } }

    private fun restaurarUltimaCancion() { viewModelScope.launch { val prefs = getApplication<Application>().dataStore.data.first(); val ultimoId = prefs[ultimaCancionIdKey]; val ultimaPosicion = prefs[ultimaPosicionKey] ?: 0L; if (ultimoId != null) { val cancion = _canciones.value.find { it.id.toString() == ultimoId }; cancion?.let { ultimaCancion -> val listaDeLaCarpeta = _canciones.value.filter { audio -> audio.carpeta == ultimaCancion.carpeta }; val indice = listaDeLaCarpeta.indexOf(ultimaCancion); if (indice != -1) { val mediaItems = listaDeLaCarpeta.map { audio -> MediaItem.Builder().setMediaId(audio.id.toString()).setUri(audio.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(audio.titulo).setArtist(audio.artista).setArtworkUri(audio.albumArtUri).build()).build() }; reproductor?.setMediaItems(mediaItems, indice, ultimaPosicion); reproductor?.prepare(); _cancionActual.value = ultimaCancion; _posicionActual.value = ultimaPosicion } } } } }

    private fun sincronizarConServicioActivo() { val currentId = reproductor?.currentMediaItem?.mediaId; if (currentId != null) { _cancionActual.value = _canciones.value.find { it.id.toString() == currentId }; _posicionActual.value = reproductor?.currentPosition ?: 0L; _duracionTotal.value = reproductor?.duration ?: 0L; _shuffleActivado.value = reproductor?.shuffleModeEnabled ?: false; _modoRepeat.value = reproductor?.repeatMode ?: Player.REPEAT_MODE_OFF; val sonando = reproductor?.isPlaying == true; _estaReproduciendo.value = sonando; if (sonando) { faderHandler.removeCallbacks(motorFaderRunnable); faderHandler.post(motorFaderRunnable) } } }

    fun adelantarA(posicion: Float) { reproductor?.seekTo(posicion.toLong()); viewModelScope.launch { getApplication<Application>().dataStore.edit { it[ultimaPosicionKey] = posicion.toLong() } } }

    fun alternarPlayPause() {
        reproductor?.let {
            if (it.isPlaying) {
                it.pause()
                ghostPlayer?.pause()
            } else {
                it.play()
                if (isMixing) ghostPlayer?.play()
            }
        }
    }

    fun siguienteCancion() { isMixing = false; isGhostPrepared = false; ghostPlayer?.stop(); reproductor?.volume = 1f; reproductor?.seekToNext() }
    fun anteriorCancion() { isMixing = false; isGhostPrepared = false; ghostPlayer?.stop(); reproductor?.volume = 1f; reproductor?.seekToPrevious() }

    override fun onCleared() { super.onCleared(); ecualizadorAudio?.release(); bassBoostAudio?.release(); normalizadorAudio?.release(); faderHandler.removeCallbacksAndMessages(null); ghostPlayer?.release(); controllerFuture?.let { MediaController.releaseFuture(it) } }

    private fun cargarMusicaLocal() {
        val listaTemporal = mutableListOf<AudioModel>()
        val context = getApplication<Application>().applicationContext
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        viewModelScope.launch(Dispatchers.Default) {
            val prefs = getApplication<Application>().dataStore.data.first()
            val filtroActivo = prefs[ajusteFiltroCortosKey] ?: true
            var seleccion = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            if (filtroActivo) seleccion += " AND ${MediaStore.Audio.Media.DURATION} >= 60000"
            context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA), seleccion, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
                val idColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tituloColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistaColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val duracionColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumna = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val duracionMs = cursor.getLong(duracionColumna)
                    val audioId = cursor.getLong(idColumna)
                    val albumId = cursor.getLong(albumIdColumna)

                    val uriCancion = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId)

                    val uriPortada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        uriCancion
                    } else {
                        "content://media/external/audio/albumart/$albumId".toUri()
                    }

                    listaTemporal.add(
                        AudioModel(
                            id = audioId,
                            titulo = cursor.getString(tituloColumna) ?: "Desconocido",
                            artista = cursor.getString(artistaColumna) ?: "Artista Desconocido",
                            duracionStr = String.format(Locale.US, "%d:%02d", (duracionMs / 1000) / 60, (duracionMs / 1000) % 60),
                            uri = uriCancion,
                            albumArtUri = uriPortada,
                            carpeta = File(cursor.getString(dataColumna)).parentFile?.name ?: "Desconocida"
                        )
                    )
                }
            }
            _canciones.value = listaTemporal
            _cancionesAgrupadasPorCarpeta.value = listaTemporal.groupBy { it.carpeta }
            _cancionesAgrupadasPorArtista.value = listaTemporal.groupBy { it.artista }.filterKeys { it != "<unknown>" }
            _cancionesAgrupadasPorAlbum.value = listaTemporal.groupBy { it.carpeta }.filterKeys { it != "<unknown>" }
            val historialStr = prefs[historialRecientesKey] ?: ""
            val historial = historialStr.split(",").filter { it.isNotBlank() }
            actualizarListaRecientes(historial)
        }
    }
}
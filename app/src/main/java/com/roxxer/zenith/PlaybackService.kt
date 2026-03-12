package com.roxxer.zenith

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Perfil de Audio (Música)
        val atributosDeAudio = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // --- MAGIA ANTI-LAG: Buffer Expandido ---
        val escudoAntiLag = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000,  // Mínimo buffer: 50 s
                120_000, // Máximo buffer: 120 s
                1_500,   // Inicio rápido: 1.5 s
                3_000    // Reanudar tras interrupción: 3 s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 2. Motor con WakeLock + escudo inyectado
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(atributosDeAudio, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(escudoAntiLag)
            // ★ FIX PRINCIPAL: WakeLock parcial sobre la CPU
            // Impide que Android suspenda el hilo de audio cuando
            // el lector de huellas genera un spike de actividad.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setUsePlatformDiagnostics(false)
            .build()

        // ★ AUDIO OFFLOAD: delega el audio al DSP de hardware (API correcta para Media3)
        // Libera la CPU principal → el unlock de huella ya no afecta la reproducción
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            player!!.trackSelectionParameters = player!!.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(
                            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                        )
                        .setIsGaplessSupportRequired(true)
                        .build()
                )
                .build()
        }

        // 3. Sesión con el Extractor de Carátulas
        mediaSession = MediaSession.Builder(this, player!!)
            .setBitmapLoader(ExtraccionCaratulaNativa(this))
            .build()

        // 4. Notificación Premium
        val proveedorNotificacion = DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(R.drawable.ic_launcher_foreground)
        }
        setMediaNotificationProvider(proveedorNotificacion)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

// --- EL EXTRACTOR DE CARÁTULAS PARA MEDIA3 Y LA PANTALLA DE BLOQUEO ---
@OptIn(UnstableApi::class)
class ExtraccionCaratulaNativa(private val context: Context) : BitmapLoader {
    // Pool de 2 hilos: uno activo, uno en standby
    private val hiloExtraccion = Executors.newFixedThreadPool(2)

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val futuro = SettableFuture.create<Bitmap>()
        hiloExtraccion.submit {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) futuro.set(bitmap)
                else futuro.setException(IllegalArgumentException("No se pudo decodificar la imagen"))
            } catch (e: Exception) {
                futuro.setException(e)
            }
        }
        return futuro
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val futuro = SettableFuture.create<Bitmap>()
        hiloExtraccion.submit {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    uri.toString().contains("audio/media")
                ) {
                    val bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                    futuro.set(bitmap)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                        if (bitmap != null) futuro.set(bitmap)
                        else futuro.setException(IllegalArgumentException("Imagen vacía"))
                    } ?: futuro.setException(IllegalArgumentException("No se pudo leer el archivo"))
                }
            } catch (e: Exception) {
                futuro.setException(e)
            }
        }
        return futuro
    }
}
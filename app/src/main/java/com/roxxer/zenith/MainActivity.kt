package com.roxxer.zenith

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.roxxer.zenith.ui.theme.ZenithTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- MAGIA PREMIUM: INMERSIÓN TOTAL ---
        enableEdgeToEdge()

        // --- MAGIA: Configuramos Coil para que extraiga carátulas nativas en Android 10+ ---
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(AudioCoverFetcher.Factory(this@MainActivity))
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            ZenithTheme {
                EnrutadorPrincipal()
            }
        }
    }
}

// --- EL EXTRACTOR DE CARÁTULAS PARA ANDROID MODERNO (Scoped Storage) ---
class AudioCoverFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Usamos el motor nativo de Android para extraer la miniatura incrustada en el MP3
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                val drawable = BitmapDrawable(context.resources, bitmap)
                DrawableResult(drawable, isSampled = true, dataSource = DataSource.DISK)
            } else null
        } catch (e: Exception) {
            // Si la canción no tiene portada, devolvemos null y la app mostrará tu generador de gradientes
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Solo interceptamos si la URI pertenece a un archivo de audio local
            if (data.scheme == "content" && data.toString().contains("audio/media")) {
                return AudioCoverFetcher(data, options, context)
            }
            return null
        }
    }
}

@Composable
fun EnrutadorPrincipal() {
    val contexto = LocalContext.current

    val permisosNecesarios = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    var permisosConcedidos by remember {
        mutableStateOf(
            permisosNecesarios.all { permiso ->
                ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    if (permisosConcedidos) {
        MainScreen()
    } else {
        OnboardingScreen(
            onPermissionGranted = {
                permisosConcedidos = true
            }
        )
    }
}
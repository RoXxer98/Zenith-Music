package com.roxxer.zenith

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.roxxer.zenith.ui.theme.*

@Composable
fun OnboardingScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val permisosRequeridos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var denegadoPermanentemente by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todosConcedidos = resultados.values.all { concedido -> concedido == true }

        if (todosConcedidos) {
            onPermissionGranted()
        } else {
            if (activity != null) {
                val algunBloqueado = permisosRequeridos.any { permiso ->
                    val concedido = resultados[permiso] ?: false
                    !concedido && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permiso)
                }

                if (algunBloqueado) {
                    denegadoPermanentemente = true
                    Toast.makeText(context, "Permisos bloqueados. Actívalos en Ajustes.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Zenith necesita ambos permisos para brindarte la experiencia completa.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(DarkBackground, Color(0xFF070A0D))
    )

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // --- NUEVO MENSAJE DE BIENVENIDA ---
            Text(
                text = "Hola, gracias por probar Zenith 🎧",
                color = DarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Esta app fue hecha con mucho cariño para quienes aman la música y también su privacidad.\n\nAquí no encontrarás anuncios ni seguimiento.",
                color = DarkTextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Solo tu música, en paz.",
                color = DarkTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Para empezar, Zenith necesita acceso a tu biblioteca de audio y a las notificaciones.",
                color = DarkTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (denegadoPermanentemente) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        permissionLauncher.launch(permisosRequeridos)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = DarkBackground),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = if (denegadoPermanentemente) "Abrir Ajustes del Sistema" else "Permitir acceso",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (denegadoPermanentemente) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Android ocultó la ventana de permisos. Toca el botón para ir a Ajustes > Permisos y permitir el acceso.",
                    color = DarkTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
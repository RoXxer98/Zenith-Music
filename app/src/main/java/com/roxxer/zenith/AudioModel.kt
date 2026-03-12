package com.roxxer.zenith

import android.net.Uri

data class AudioModel(
    val id: Long,
    val titulo: String,
    val artista: String,
    val duracionStr: String,
    val uri: Uri,
    val albumArtUri: Uri,
    val carpeta: String // ¡NUEVO! Aquí guardaremos si está en "Download", "WhatsApp Audio", etc.
)
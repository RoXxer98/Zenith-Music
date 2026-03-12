plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Activamos la herramienta KSP
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.roxxer.zenith"
    compileSdk = 36 // Ajustado para ser más estable con Compose

    defaultConfig {
        applicationId = "com.roxxer.zenith"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // El motor de audio premium de Google (ExoPlayer/Media3)
    implementation("androidx.media3:media3-exoplayer:1.9.2")
    // Herramienta profesional para cargar imágenes (Coil)
    implementation("io.coil-kt:coil-compose:2.7.0")
    // El comunicador entre la app y las notificaciones de Android
    implementation("androidx.media3:media3-session:1.9.2")
    // Caja de herramientas de Iconos Premium de Google
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // El motor de navegación profesional entre pantallas
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // --- MOTOR DE BÓVEDA (ROOM PRO) ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences) //DataStore
    implementation(libs.billing.ktx)    //PlayStore
}
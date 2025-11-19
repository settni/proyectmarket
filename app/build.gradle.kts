import org.gradle.api.JavaVersion
plugins {
    // Aplica el plugin de Android (usa la versión 8.1.3 definida arriba)
    id("com.android.application")

    // Aplica el plugin de Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.diegodev.marketplace"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.diegodev.marketplace"
        minSdk = 26
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
        // Configuración para usar Java 11
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Dependencias de Android y UI
    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // -- CONFIGURACIÓN DE FIREBASE (USANDO BOM) --
    // Importa la BOM (Bill of Materials) - Fija las versiones de Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))

    // Dependencias específicas (sin versión, usando KTX)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // -- OTRAS DEPENDENCIAS --
    // Librería Glide para imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
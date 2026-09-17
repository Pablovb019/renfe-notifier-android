plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Plugin de Google Services (Firebase). Solo se aplica si existe google-services.json,
// que se inyecta en CI de release (secret ANDROID_GOOGLE_SERVICES_JSON_B64) y nunca
// se commitea. Sin el archivo, el build debug/CI sigue funcionando sin Firebase.
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.pablovb019.renfenotifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pablovb019.renfenotifier"
        // Android 6.0+. El dispositivo objetivo (realme GT Neo 2) ejecuta Android 13.
        minSdk = 26
        // Android 13 y superiores; nunca un targetSdk antiguo para eludir restricciones.
        targetSdk = 34
        versionCode = 4
        versionName = "0.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Backend por defecto (emulador -> host local). La URL se configura en
        // pantalla en un paso posterior; el cliente solo acepta https salvo loopback.
        buildConfigField("String", "BACKEND_URL", "\"http://34.73.192.35:8000/\"")
    }

    signingConfigs {
        create("release") {
            val storeFile = System.getenv("KEYSTORE_PATH")?.let { File(it) }
            val storePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            if (storeFile != null && storeFile.exists() && storePassword != null && keyAlias != null && keyPassword != null) {
                this.storeFile = storeFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Solo aplicar signingConfig si las variables de entorno están definidas
            val hasSigning = System.getenv("KEYSTORE_PATH") != null &&
                             System.getenv("KEYSTORE_PASSWORD") != null &&
                             System.getenv("KEY_ALIAS") != null &&
                             System.getenv("KEY_PASSWORD") != null
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.google.play.services.base)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
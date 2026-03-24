import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val keystoreProperties = Properties().also { props ->
    val file = rootProject.file("buildsystem/keystore.properties")
    if (file.exists()) props.load(file.inputStream())
}

android {
    namespace = "com.julian.dixmille"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.julian.dixmille"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = (keystoreProperties["STORE_FILE"] as String?)?.let { file(it) }
            storePassword = keystoreProperties["STORE_PASSWORD"] as String?
            keyAlias = keystoreProperties["KEY_ALIAS"] as String?
            keyPassword = keystoreProperties["KEY_PASSWORD"] as String?
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    debugImplementation(libs.compose.uiTooling)
}

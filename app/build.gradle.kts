plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.givebackmyoneplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.givebackmyoneplus"
        minSdk = 29
        targetSdk = 35
        // Номер сборки GitHub Actions — чтобы каждая новая сборка была «новее» предыдущей.
        versionCode = 20
        versionName = "1.0 (build 20)"
    }

    signingConfigs {
        // Постоянный ключ в репозитории: обновления ставятся поверх без удаления.
        create("release") {
            storeFile = file("release.jks")
            storePassword = "android1"
            keyAlias = "app"
            keyPassword = "android1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}

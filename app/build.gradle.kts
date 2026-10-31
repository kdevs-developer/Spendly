import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    android.set(false)
    ignoreFailures.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

android {
    namespace = "com.kdevs.spendly"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.kdevs.spendly"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    // Without it Android's normal debug key is used.
    val keystoreProps =
        Properties().apply {
            val f = rootProject.file("keystore.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            getByName("debug") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        // Optional: add releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword to
        // keystore.properties and the release build is signed with them.
        if (keystoreProps.containsKey("releaseStoreFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("releaseStoreFile"))
                storePassword = keystoreProps.getProperty("releaseStorePassword")
                keyAlias = keystoreProps.getProperty("releaseKeyAlias")
                keyPassword = keystoreProps.getProperty("releaseKeyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.room:room-common:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Biometric lock (FragmentActivity is what the system prompt attaches to)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    // Continue with Google
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // Friend QR: draw our code, scan theirs with Google's scanner (no camera permission needed)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Coroutines (android adds Dispatchers.Main)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Dependency injection (Hilt)
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

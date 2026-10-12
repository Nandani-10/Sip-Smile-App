plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sipsmile.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sipsmile.app"
        minSdk = 26
        targetSdk = 35
        // CI passes the release tag and run number so every release has a higher version
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 3
        versionName = (project.findProperty("appVersion") as String?) ?: "1.2.0"
    }

    // One permanent signing key (given to CI as a secret) so new APKs can update old installs.
    val keystorePath = System.getenv("KEYSTORE_FILE")
    val hasKey = keystorePath != null && File(keystorePath).exists()
    if (hasKey) {
        signingConfigs {
            create("sipsmile") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "sipsmile"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (hasKey) signingConfig = signingConfigs.getByName("sipsmile")
        }
        release {
            isMinifyEnabled = false
            if (hasKey) signingConfig = signingConfigs.getByName("sipsmile")
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
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}

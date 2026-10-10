plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.appsventory.digiapps.scour"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.appsventory.digiapps.scour"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "2.0.0"

    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Native video player (Media3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
}

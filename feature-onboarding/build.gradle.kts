plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.signkorean.feature.onboarding"
    compileSdk = 37

    defaultConfig {
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":domain"))
    implementation(project(":designsystem"))

    testImplementation(libs.junit4)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
